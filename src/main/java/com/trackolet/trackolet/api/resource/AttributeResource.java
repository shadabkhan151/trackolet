/*
 * Copyright 2017 - 2022 Anton Tananaev (anton@traccar.org)
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
package com.trackolet.trackolet.api.resource;

import com.trackolet.trackolet.model.Attribute;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import com.trackolet.trackolet.api.ExtendedObjectResource;
import com.trackolet.trackolet.handler.ComputedAttributesHandler;
import com.trackolet.trackolet.model.Device;
import com.trackolet.trackolet.model.Position;
import com.trackolet.trackolet.storage.StorageException;
import com.trackolet.trackolet.storage.query.Columns;
import com.trackolet.trackolet.storage.query.Condition;
import com.trackolet.trackolet.storage.query.Request;

@Path("attributes/computed")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AttributeResource extends ExtendedObjectResource<Attribute> {

    @Inject
    private ComputedAttributesHandler computedAttributesHandler;

    public AttributeResource() {
        super(Attribute.class);
    }

    @POST
    @Path("test")
    public Response test(@QueryParam("deviceId") long deviceId, Attribute entity) throws StorageException {
        permissionsService.checkAdmin(getUserId());
        permissionsService.checkPermission(Device.class, getUserId(), deviceId);

        Position position = storage.getObject(Position.class, new Request(
                new Columns.All(),
                new Condition.LatestPositions(deviceId)));

        Object result = computedAttributesHandler.computeAttribute(entity, position);
        if (result != null) {
            switch (entity.getType()) {
                case "number":
                    Number numberValue = (Number) result;
                    return Response.ok(numberValue).build();
                case "boolean":
                    Boolean booleanValue = (Boolean) result;
                    return Response.ok(booleanValue).build();
                default:
                    return Response.ok(result.toString()).build();
            }
        } else {
            return Response.noContent().build();
        }
    }

    @POST
    public Response add(Attribute entity) throws Exception {
        permissionsService.checkAdmin(getUserId());
        return super.add(entity);
    }

    @Path("{id}")
    @PUT
    public Response update(Attribute entity) throws Exception {
        permissionsService.checkAdmin(getUserId());
        return super.update(entity);
    }

    @Path("{id}")
    @DELETE
    public Response remove(@PathParam("id") long id) throws Exception {
        permissionsService.checkAdmin(getUserId());
        return super.remove(id);
    }

}
