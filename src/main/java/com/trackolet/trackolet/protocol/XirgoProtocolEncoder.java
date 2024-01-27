/*
 * Copyright 2017 - 2019 Anton Tananaev (anton@traccar.org)
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

import com.trackolet.trackolet.StringProtocolEncoder;
import com.trackolet.trackolet.model.Command;
import com.trackolet.trackolet.Protocol;

public class XirgoProtocolEncoder extends StringProtocolEncoder {

    public XirgoProtocolEncoder(Protocol protocol) {
        super(protocol);
    }

    @Override
    protected Object encodeCommand(Command command) {

        switch (command.getType()) {
            case Command.TYPE_OUTPUT_CONTROL:
                return String.format("+XT:7005,%d,1", command.getInteger(Command.KEY_DATA) + 1);
            default:
                return null;
        }
    }

}
