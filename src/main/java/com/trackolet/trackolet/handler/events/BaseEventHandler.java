/*
 * Copyright 2016 - 2022 Anton Tananaev (anton@traccar.org)
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
package com.trackolet.trackolet.handler.events;

import java.util.Map;

import com.trackolet.trackolet.BaseDataHandler;
import com.trackolet.trackolet.database.NotificationManager;
import com.trackolet.trackolet.model.Event;
import com.trackolet.trackolet.model.Position;

import jakarta.inject.Inject;

public abstract class BaseEventHandler extends BaseDataHandler {

    private NotificationManager notificationManager;

    @Inject
    public void setNotificationManager(NotificationManager notificationManager) {
        this.notificationManager = notificationManager;
    }

    @Override
    protected Position handlePosition(Position position) {
        Map<Event, Position> events = analyzePosition(position);
        if (events != null && !events.isEmpty()) {
            notificationManager.updateEvents(events);
        }
        return position;
    }

    protected abstract Map<Event, Position> analyzePosition(Position position);

}