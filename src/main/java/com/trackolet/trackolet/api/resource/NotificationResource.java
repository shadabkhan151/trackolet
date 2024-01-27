/*
 * Copyright 2016 - 2023 Anton Tananaev (anton@traccar.org)
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
package com.trackolet.trackolet.api.resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.trackolet.trackolet.api.ExtendedObjectResource;
import com.trackolet.trackolet.model.Event;
import com.trackolet.trackolet.model.Notification;
import com.trackolet.trackolet.model.Typed;
import com.trackolet.trackolet.model.User;
import com.trackolet.trackolet.notification.MessageException;
import com.trackolet.trackolet.notification.NotificatorManager;
import com.trackolet.trackolet.storage.StorageException;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

@Path("notifications")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class NotificationResource extends ExtendedObjectResource<Notification> {

    private static final Logger LOGGER = LoggerFactory.getLogger(NotificationResource.class);

    @Inject
    private NotificatorManager notificatorManager;

    public NotificationResource() {
        super(Notification.class);
    }

    @GET
    @Path("types")
    public Collection<Typed> get() {
        List<Typed> types = new LinkedList<>();
        Field[] fields = Event.class.getDeclaredFields();
        for (Field field : fields) {
            if (Modifier.isStatic(field.getModifiers()) && field.getName().startsWith("TYPE_")) {
                try {
                    types.add(new Typed(field.get(null).toString()));
                } catch (IllegalArgumentException | IllegalAccessException error) {
                    LOGGER.warn("Get event types error", error);
                }
            }
        }
        return types;
    }

    @GET
    @Path("notificators")
    public Collection<Typed> getNotificators() {
        return notificatorManager.getAllNotificatorTypes();
    }

    @POST
    @Path("test")
    public Response testMessage() throws MessageException, StorageException {
        User user = permissionsService.getUser(getUserId());
        for (Typed method : notificatorManager.getAllNotificatorTypes()) {
            notificatorManager.getNotificator(method.getType()).send(null, user, new Event("test", 0), null);
        }
        return Response.noContent().build();
    }

    @POST
    @Path("test/{notificator}")
    public Response testMessage(@PathParam("notificator") String notificator)
            throws MessageException, StorageException {
        User user = permissionsService.getUser(getUserId());
        notificatorManager.getNotificator(notificator).send(null, user, new Event("test", 0), null);
        return Response.noContent().build();
    }

}
