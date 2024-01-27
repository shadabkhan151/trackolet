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

import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import com.trackolet.trackolet.BaseProtocol;
import com.trackolet.trackolet.CharacterDelimiterFrameDecoder;
import com.trackolet.trackolet.PipelineBuilder;
import com.trackolet.trackolet.TrackerServer;
import com.trackolet.trackolet.config.Config;
import com.trackolet.trackolet.model.Command;

import jakarta.inject.Inject;

public class XirgoProtocol extends BaseProtocol {

    @Inject
    public XirgoProtocol(Config config) {
        setSupportedDataCommands(
                Command.TYPE_OUTPUT_CONTROL);
        addServer(new TrackerServer(config, getName(), false) {
            @Override
            protected void addProtocolHandlers(PipelineBuilder pipeline, Config config) {
                pipeline.addLast(new CharacterDelimiterFrameDecoder(1024, "##"));
                pipeline.addLast(new StringEncoder());
                pipeline.addLast(new StringDecoder());
                pipeline.addLast(new XirgoProtocolEncoder(XirgoProtocol.this));
                pipeline.addLast(new XirgoProtocolDecoder(XirgoProtocol.this));
            }
        });
        addServer(new TrackerServer(config, getName(), true) {
            @Override
            protected void addProtocolHandlers(PipelineBuilder pipeline, Config config) {
                pipeline.addLast(new StringEncoder());
                pipeline.addLast(new StringDecoder());
                pipeline.addLast(new XirgoProtocolEncoder(XirgoProtocol.this));
                pipeline.addLast(new XirgoProtocolDecoder(XirgoProtocol.this));
            }
        });
    }

}
