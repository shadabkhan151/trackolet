/*
 * Copyright 2015 - 2022 Anton Tananaev (anton@traccar.org)
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.trackolet.trackolet.BaseDataHandler;
import com.trackolet.trackolet.model.Position;
import com.trackolet.trackolet.storage.Storage;
import com.trackolet.trackolet.storage.query.Columns;
import com.trackolet.trackolet.storage.query.Request;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Singleton
@ChannelHandler.Sharable
public class DefaultDataHandler extends BaseDataHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultDataHandler.class);

    private final Storage storage;

    @Inject
    public DefaultDataHandler(Storage storage) {
        this.storage = storage;
    }

    @Override
    protected Position handlePosition(Position position) {

        try {
            position.setId(storage.addObject(position, new Request(new Columns.Exclude("id"))));
        } catch (Exception error) {
            LOGGER.warn("Failed to store position", error);
        }

        return position;
    }

}
