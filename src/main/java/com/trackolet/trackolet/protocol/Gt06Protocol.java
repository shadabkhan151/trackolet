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

import com.trackolet.trackolet.BaseProtocol;
import com.trackolet.trackolet.PipelineBuilder;
import com.trackolet.trackolet.TrackerServer;
import com.trackolet.trackolet.config.Config;
import com.trackolet.trackolet.model.Command;

import jakarta.inject.Inject;

public class Gt06Protocol extends BaseProtocol {

    @Inject
    public Gt06Protocol(Config config) {
        setSupportedDataCommands(
                Command.TYPE_ENGINE_STOP,
                Command.TYPE_ENGINE_RESUME,
                Command.TYPE_CUSTOM);
        addServer(new TrackerServer(config, getName(), false) {
            @Override
            protected void addProtocolHandlers(PipelineBuilder pipeline, Config config) {
                pipeline.addLast(new Gt06FrameDecoder());
                pipeline.addLast(new Gt06ProtocolEncoder(Gt06Protocol.this));
                pipeline.addLast(new Gt06ProtocolDecoder(Gt06Protocol.this));
            }
        });
    }

}
