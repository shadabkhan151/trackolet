/*
 * Copyright 2017 - 2023 Anton Tananaev (anton@traccar.org)
 * Copyright 2017 - 2018 Andrey Kunitsyn (andrey@traccar.org)
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

import com.trackolet.trackolet.database.StatisticsManager;
import com.trackolet.trackolet.model.Event;
import com.trackolet.trackolet.model.Notification;
import com.trackolet.trackolet.model.Position;
import com.trackolet.trackolet.model.User;
import com.trackolet.trackolet.notification.MessageException;
import com.trackolet.trackolet.notification.NotificationFormatter;
import com.trackolet.trackolet.sms.SmsManager;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Singleton
public class NotificatorSms implements Notificator {

    private final SmsManager smsManager;
    private final NotificationFormatter notificationFormatter;
    private final StatisticsManager statisticsManager;

    @Inject
    public NotificatorSms(
            SmsManager smsManager, NotificationFormatter notificationFormatter, StatisticsManager statisticsManager) {
        this.smsManager = smsManager;
        this.notificationFormatter = notificationFormatter;
        this.statisticsManager = statisticsManager;
    }

    @Override
    public void send(Notification notification, User user, Event event, Position position) throws MessageException {
        if (user.getPhone() != null) {
            var shortMessage = notificationFormatter.formatMessage(notification, user, event, position, "short");
            statisticsManager.registerSms();
            smsManager.sendMessage(user.getPhone(), shortMessage.getBody(), false);
        }
    }

}
