/*
 * Copyright 2015 - 2019 Anton Tananaev (anton@traccar.org)
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
package com.trackolet.trackolet.protocol;

import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import com.trackolet.trackolet.BaseProtocol;
import com.trackolet.trackolet.PipelineBuilder;
import com.trackolet.trackolet.TrackerServer;
import com.trackolet.trackolet.config.Config;
import com.trackolet.trackolet.model.Command;

import jakarta.inject.Inject;

public class CityeasyProtocol extends BaseProtocol {

    @Inject
    public CityeasyProtocol(Config config) {
        setSupportedDataCommands(
                Command.TYPE_POSITION_SINGLE,
                Command.TYPE_POSITION_PERIODIC,
                Command.TYPE_POSITION_STOP,
                Command.TYPE_SET_TIMEZONE);
        addServer(new TrackerServer(config, getName(), false) {
            @Override
            protected void addProtocolHandlers(PipelineBuilder pipeline, Config config) {
                pipeline.addLast(new LengthFieldBasedFrameDecoder(1024, 2, 2, -4, 0));
                pipeline.addLast(new CityeasyProtocolEncoder(CityeasyProtocol.this));
                pipeline.addLast(new CityeasyProtocolDecoder(CityeasyProtocol.this));
            }
        });
    }

}
