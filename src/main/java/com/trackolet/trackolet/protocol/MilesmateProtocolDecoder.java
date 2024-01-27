/*
 * Copyright 2018 Anton Tananaev (anton@traccar.org)
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

import io.netty.channel.Channel;
import com.trackolet.trackolet.BaseProtocolDecoder;
import com.trackolet.trackolet.session.DeviceSession;
import com.trackolet.trackolet.NetworkMessage;
import com.trackolet.trackolet.Protocol;
import com.trackolet.trackolet.helper.DateBuilder;
import com.trackolet.trackolet.helper.Parser;
import com.trackolet.trackolet.helper.PatternBuilder;
import com.trackolet.trackolet.helper.UnitsConverter;
import com.trackolet.trackolet.model.Position;

import java.net.SocketAddress;
import java.util.regex.Pattern;

public class MilesmateProtocolDecoder extends BaseProtocolDecoder {

    public MilesmateProtocolDecoder(Protocol protocol) {
        super(protocol);
    }

    private static final Pattern PATTERN = new PatternBuilder()
            .text("ApiString={")
            .number("A:(d+),")                   // imei
            .number("B:(d+.d+),")                // battery
            .number("C:(d+.d+),")                // adc
            .number("D:(dd)(dd)(dd),")           // time (hhmmss)
            .number("E:(dd)(dd.d+)([NS]),")      // latitude
            .number("F:(ddd)(dd.d+)([EW]),")     // longitude
            .number("G:(d+.d+),")                // speed
            .number("H:(dd)(dd)(dd),")           // date (ddmmyy)
            .expression("I:[GL],")               // location source
            .number("J:(d{8}),")                 // flags
            .number("K:(d{7})")                  // flags
            .expression("([AV]),")               // validity
            .number("L:d{4},")                   // pin
            .number("M:(d+.d+)")                 // course
            .text("}")
            .compile();

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        Parser parser = new Parser(PATTERN, (String) msg);
        if (!parser.matches()) {
            return null;
        }

        if (channel != null) {
            channel.writeAndFlush(new NetworkMessage("+##Received OK\n", remoteAddress));
        }

        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, parser.next());
        if (deviceSession == null) {
            return null;
        }

        Position position = new Position(getProtocolName());
        position.setDeviceId(deviceSession.getDeviceId());

        position.set(Position.KEY_BATTERY, parser.nextDouble());
        position.set(Position.PREFIX_ADC + 1, parser.nextDouble());

        DateBuilder dateBuilder = new DateBuilder()
                .setTime(parser.nextInt(), parser.nextInt(), parser.nextInt());

        position.setLatitude(parser.nextCoordinate());
        position.setLongitude(parser.nextCoordinate());
        position.setSpeed(UnitsConverter.knotsFromKph(parser.nextDouble()));

        dateBuilder.setDateReverse(parser.nextInt(), parser.nextInt(), parser.nextInt());
        position.setTime(dateBuilder.getDate());

        String flags = parser.next();
        position.set(Position.KEY_IGNITION, flags.charAt(0) == '1');
        position.set(Position.KEY_ALARM, flags.charAt(1) == '1' ? Position.ALARM_SOS : null);
        position.set(Position.KEY_CHARGE, flags.charAt(5) == '1');
        position.set(Position.KEY_ALARM, flags.charAt(7) == '1' ? Position.ALARM_OVERSPEED : null);

        flags = parser.next();
        position.set(Position.KEY_BLOCKED, flags.charAt(0) == '1');
        position.set(Position.KEY_ALARM, flags.charAt(1) == '1' ? Position.ALARM_TOW : null);

        position.setValid(parser.next().equals("A"));

        position.setCourse(parser.nextDouble());

        return position;
    }

}
