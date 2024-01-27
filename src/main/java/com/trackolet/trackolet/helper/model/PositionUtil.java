/*
 * Copyright 2022 Anton Tananaev (anton@traccar.org)
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
package com.trackolet.trackolet.helper.model;

import com.trackolet.trackolet.model.BaseModel;
import com.trackolet.trackolet.model.Device;
import com.trackolet.trackolet.model.Position;
import com.trackolet.trackolet.model.User;
import com.trackolet.trackolet.session.cache.CacheManager;
import com.trackolet.trackolet.storage.Storage;
import com.trackolet.trackolet.storage.StorageException;
import com.trackolet.trackolet.storage.query.Columns;
import com.trackolet.trackolet.storage.query.Condition;
import com.trackolet.trackolet.storage.query.Order;
import com.trackolet.trackolet.storage.query.Request;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

public final class PositionUtil {

    private PositionUtil() {
    }

    public static boolean isLatest(CacheManager cacheManager, Position position) {
        Position lastPosition = cacheManager.getPosition(position.getDeviceId());
        return lastPosition == null || position.getFixTime().compareTo(lastPosition.getFixTime()) >= 0;
    }

    public static double calculateDistance(Position first, Position last, boolean useOdometer) {
        double distance;
        double firstOdometer = first.getDouble(Position.KEY_ODOMETER);
        double lastOdometer = last.getDouble(Position.KEY_ODOMETER);

        if (useOdometer && firstOdometer != 0.0 && lastOdometer != 0.0) {
            distance = lastOdometer - firstOdometer;
        } else {
            distance = last.getDouble(Position.KEY_TOTAL_DISTANCE) - first.getDouble(Position.KEY_TOTAL_DISTANCE);
        }
        return distance;
    }

    public static List<Position> getPositions(
            Storage storage, long deviceId, Date from, Date to) throws StorageException {
        return storage.getObjects(Position.class, new Request(
                new Columns.All(),
                new Condition.And(
                        new Condition.Equals("deviceId", deviceId),
                        new Condition.Between("fixTime", "from", from, "to", to)),
                new Order("fixTime")));
    }

    public static List<Position> getLatestPositions(Storage storage, long userId) throws StorageException {
        var devices = storage.getObjects(Device.class, new Request(
                new Columns.Include("id"),
                new Condition.Permission(User.class, userId, Device.class)));
        var deviceIds = devices.stream().map(BaseModel::getId).collect(Collectors.toUnmodifiableSet());

        var positions = storage.getObjects(Position.class, new Request(
                new Columns.All(), new Condition.LatestPositions()));
        return positions.stream()
                .filter(position -> deviceIds.contains(position.getDeviceId()))
                .collect(Collectors.toList());
    }

}
