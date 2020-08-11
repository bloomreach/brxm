/*
 *  Copyright 2020 Hippo B.V. (http://www.onehippo.com)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.hippoecm.hst.pagecomposer.jaxrs.services;

import javax.ws.rs.core.Response;

import org.hippoecm.hst.pagecomposer.jaxrs.model.ContainerRepresentation;

/**
 * TODO ideally we could move annotations from the implementation classes to the interface methods, for example
 * <pre>
 *     @POST
 *     @Path("/{itemUUID}")
 *     @Consumes(MediaType.APPLICATION_JSON)
 *     @Produces(MediaType.APPLICATION_JSON)
 *     Response createContainerItem
 * </pre>
 * However this does not seem to work, although cxf claims to support it
 * https://cxf.apache.org/docs/jax-rs-basics.html#JAX-RSBasics-Annotationinheritance
 */
public interface ContainerComponentResourceInterface {

    Response createContainerItem(String itemUUID, long versionStamp);

    Response createContainerItemAndAddBefore(String itemUUID, String siblingItemUUID, long versionStamp);

    Response updateContainer(ContainerRepresentation container);

    Response deleteContainerItem(String itemUUID, long versionStamp);
}
