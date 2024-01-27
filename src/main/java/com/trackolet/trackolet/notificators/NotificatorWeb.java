/*
 * Copyright 2018 - 2023 Anton Tananaev (anton@traccar.org)
 * Copyright 2018 Andrey Kunitsyn (andrey@traccar.org)
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
package com.trackolet.trackolet.notificators;

import com.trackolet.trackolet.model.Event;
import com.trackolet.trackolet.model.Notification;
import com.trackolet.trackolet.model.Position;
import com.trackolet.trackolet.model.User;
import com.trackolet.trackolet.notification.NotificationFormatter;
import com.trackolet.trackolet.session.ConnectionManager;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Singleton
public final class NotificatorWeb implements Notificator {

    private final ConnectionManager connectionManager;
    private final NotificationFormatter notificationFormatter;

    @Inject
    public NotificatorWeb(ConnectionManager connectionManager, NotificationFormatter notificationFormatter) {
        this.connectionManager = connectionManager;
        this.notificationFormatter = notificationFormatter;
    }

    @Override
    public void send(Notification notification, User user, Event event, Position position) {

        Event copy = new Event();
        copy.setId(event.getId());
        copy.setDeviceId(event.getDeviceId());
        copy.setType(event.getType());
        copy.setEventTime(event.getEventTime());
        copy.setPositionId(event.getPositionId());
        copy.setGeofenceId(event.getGeofenceId());
        copy.setMaintenanceId(event.getMaintenanceId());
        copy.getAttributes().putAll(event.getAttributes());

        var message = notificationFormatter.formatMessage(notification, user, event, position, "short");
        copy.set("message", message.getBody());

        connectionManager.updateEvent(true, user.getId(), copy);
    }

}
