/*
 * Copyright 2012 - 2022 Anton Tananaev (anton@traccar.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.trackolet.trackolet;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.socket.DatagramChannel;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.timeout.IdleStateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.trackolet.trackolet.config.Config;
import com.trackolet.trackolet.config.Keys;
import com.trackolet.trackolet.database.StatisticsManager;
import com.trackolet.trackolet.handler.AcknowledgementHandler;
import com.trackolet.trackolet.helper.DateUtil;
import com.trackolet.trackolet.helper.NetworkUtil;
import com.trackolet.trackolet.helper.model.PositionUtil;
import com.trackolet.trackolet.model.Device;
import com.trackolet.trackolet.model.Position;
import com.trackolet.trackolet.session.ConnectionManager;
import com.trackolet.trackolet.session.cache.CacheManager;
import com.trackolet.trackolet.storage.Storage;
import com.trackolet.trackolet.storage.StorageException;
import com.trackolet.trackolet.storage.query.Columns;
import com.trackolet.trackolet.storage.query.Condition;
import com.trackolet.trackolet.storage.query.Request;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

@Singleton
@ChannelHandler.Sharable
public class MainEventHandler extends ChannelInboundHandlerAdapter {

    private static final Logger LOGGER = LoggerFactory.getLogger(MainEventHandler.class);

    private final Set<String> connectionlessProtocols = new HashSet<>();
    private final Set<String> logAttributes = new LinkedHashSet<>();

    private final CacheManager cacheManager;
    private final Storage storage;
    private final ConnectionManager connectionManager;
    private final StatisticsManager statisticsManager;

    @Inject
    public MainEventHandler(
            Config config, CacheManager cacheManager, Storage storage, ConnectionManager connectionManager,
            StatisticsManager statisticsManager) {
        this.cacheManager = cacheManager;
        this.storage = storage;
        this.connectionManager = connectionManager;
        this.statisticsManager = statisticsManager;
        String connectionlessProtocolList = config.getString(Keys.STATUS_IGNORE_OFFLINE);
        if (connectionlessProtocolList != null) {
            connectionlessProtocols.addAll(Arrays.asList(connectionlessProtocolList.split("[, ]")));
        }
        logAttributes.addAll(Arrays.asList(config.getString(Keys.LOGGER_ATTRIBUTES).split("[, ]")));
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (msg instanceof Position) {

            Position position = (Position) msg;
            Device device = cacheManager.getObject(Device.class, position.getDeviceId());

            try {
                if (PositionUtil.isLatest(cacheManager, position)) {
                    Device updatedDevice = new Device();
                    updatedDevice.setId(position.getDeviceId());
                    updatedDevice.setPositionId(position.getId());
                    storage.updateObject(updatedDevice, new Request(
                            new Columns.Include("positionId"),
                            new Condition.Equals("id", updatedDevice.getId())));

                    cacheManager.updatePosition(position);
                    connectionManager.updatePosition(true, position);
                }
            } catch (StorageException error) {
                LOGGER.warn("Failed to update device", error);
            }

            StringBuilder builder = new StringBuilder();
            builder.append("[").append(NetworkUtil.session(ctx.channel())).append("] ");
            builder.append("id: ").append(device.getUniqueId());
            for (String attribute : logAttributes) {
                switch (attribute) {
                    case "time":
                        builder.append(", time: ").append(DateUtil.formatDate(position.getFixTime(), false));
                        break;
                    case "position":
                        builder.append(", lat: ").append(String.format("%.5f", position.getLatitude()));
                        builder.append(", lon: ").append(String.format("%.5f", position.getLongitude()));
                        break;
                    case "speed":
                        if (position.getSpeed() > 0) {
                            builder.append(", speed: ").append(String.format("%.1f", position.getSpeed()));
                        }
                        break;
                    case "course":
                        builder.append(", course: ").append(String.format("%.1f", position.getCourse()));
                        break;
                    case "accuracy":
                        if (position.getAccuracy() > 0) {
                            builder.append(", accuracy: ").append(String.format("%.1f", position.getAccuracy()));
                        }
                        break;
                    case "outdated":
                        if (position.getOutdated()) {
                            builder.append(", outdated");
                        }
                        break;
                    case "invalid":
                        if (!position.getValid()) {
                            builder.append(", invalid");
                        }
                        break;
                    default:
                        Object value = position.getAttributes().get(attribute);
                        if (value != null) {
                            builder.append(", ").append(attribute).append(": ").append(value);
                        }
                        break;
                }
            }
            LOGGER.info(builder.toString());

            statisticsManager.registerMessageStored(position.getDeviceId(), position.getProtocol());

            ctx.writeAndFlush(new AcknowledgementHandler.EventHandled(position));
        }
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        if (!(ctx.channel() instanceof DatagramChannel)) {
            LOGGER.info("[{}] connected", NetworkUtil.session(ctx.channel()));
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        LOGGER.info("[{}] disconnected", NetworkUtil.session(ctx.channel()));
        closeChannel(ctx.channel());

        boolean supportsOffline = BasePipelineFactory.getHandler(ctx.pipeline(), HttpRequestDecoder.class) == null
                && !connectionlessProtocols.contains(ctx.pipeline().get(BaseProtocolDecoder.class).getProtocolName());
        connectionManager.deviceDisconnected(ctx.channel(), supportsOffline);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        while (cause.getCause() != null && cause.getCause() != cause) {
            cause = cause.getCause();
        }
        LOGGER.info("[{}] error", NetworkUtil.session(ctx.channel()), cause);
        closeChannel(ctx.channel());
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {
        if (evt instanceof IdleStateEvent) {
            LOGGER.info("[{}] timed out", NetworkUtil.session(ctx.channel()));
            closeChannel(ctx.channel());
        }
    }

    private void closeChannel(Channel channel) {
        if (!(channel instanceof DatagramChannel)) {
            channel.close();
        }
    }

}
