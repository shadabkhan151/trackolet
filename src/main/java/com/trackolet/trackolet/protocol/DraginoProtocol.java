/*
 * Copyright 2023 Anton Tananaev (anton@traccar.org)
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

import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import jakarta.inject.Inject;
import com.trackolet.trackolet.BaseProtocol;
import com.trackolet.trackolet.PipelineBuilder;
import com.trackolet.trackolet.TrackerServer;
import com.trackolet.trackolet.config.Config;

public class DraginoProtocol extends BaseProtocol {

    @Inject
    public DraginoProtocol(Config config) {
        addServer(new TrackerServer(config, getName(), false) {
            @Override
            protected void addProtocolHandlers(PipelineBuilder pipeline, Config config) {
                pipeline.addLast(new HttpResponseEncoder());
                pipeline.addLast(new HttpRequestDecoder());
                pipeline.addLast(new HttpObjectAggregator(65535));
                pipeline.addLast(new DraginoProtocolDecoder(DraginoProtocol.this));
            }
        });
    }

}
