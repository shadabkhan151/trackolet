/*
 * Copyright 2016 - 2022 Anton Tananaev (anton@traccar.org)
 * Copyright 2016 - 2017 Andrey Kunitsyn (andrey@traccar.org)
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
package com.trackolet.trackolet.handler;

import io.netty.channel.ChannelHandler;
import com.trackolet.trackolet.BaseDataHandler;
import com.trackolet.trackolet.config.Config;
import com.trackolet.trackolet.config.Keys;
import com.trackolet.trackolet.helper.model.AttributeUtil;
import com.trackolet.trackolet.model.Position;
import com.trackolet.trackolet.session.cache.CacheManager;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Singleton
@ChannelHandler.Sharable
public class CopyAttributesHandler extends BaseDataHandler {

    private final boolean enabled;
    private final CacheManager cacheManager;

    @Inject
    public CopyAttributesHandler(Config config, CacheManager cacheManager) {
        enabled = config.getBoolean(Keys.PROCESSING_COPY_ATTRIBUTES_ENABLE);
        this.cacheManager = cacheManager;
    }

    @Override
    protected Position handlePosition(Position position) {
        if (enabled) {
            String attributesString = AttributeUtil.lookup(
                    cacheManager, Keys.PROCESSING_COPY_ATTRIBUTES, position.getDeviceId());
            Position last = cacheManager.getPosition(position.getDeviceId());
            if (last != null && attributesString != null) {
                for (String attribute : attributesString.split("[ ,]")) {
                    if (last.hasAttribute(attribute) && !position.hasAttribute(attribute)) {
                        position.getAttributes().put(attribute, last.getAttributes().get(attribute));
                    }
                }
            }
        }
        return position;
    }

}
