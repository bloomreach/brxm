/*
 * Copyright 2015-2016 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.hippoecm.hst.restapi;

import java.util.UUID;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;

public abstract class AbstractResource {

    private ResourceContextFactory resourceContextFactory;

    public abstract Logger getLogger();

    public void setResourceContextFactory(final ResourceContextFactory resourceContextFactory) {
        this.resourceContextFactory = resourceContextFactory;
    }

    public ResourceContextFactory getResourceContextFactory() {
        return resourceContextFactory;
    }

    public static final class Error {
        public final int status;
        public final String description;
        Error(final int status, final String description) {
            this.status = status;
            this.description = description;
        }
    }

    public void logException(final String message, final Exception e) {
        if (getLogger().isDebugEnabled()) {
            getLogger().info(message, e);
        } else {
            getLogger().info(message + ": '{}'", e.toString());
        }
    }

    public UUID parseUUID(final String uuid) throws IllegalArgumentException {
        try {
            return UUID.fromString(uuid);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("The string '" + uuid + "' is not a valid UUID");
        }
    }

    public boolean isNodePartOfApiContent(final ResourceContext context, final Node node) throws RepositoryException {
        // context.getRequestContext().getResolvedMount().getMount().getContentPath();
        if (node.getPath().startsWith(context.getRequestContext().getResolvedMount().getMount().getContentPath() + "/")) {
            return true;
        }
        return false;
    }

    public Response buildErrorResponse(final int status, final Exception exception) {
        return buildErrorResponse(status, exception.toString());
    }

    public Response buildErrorResponse(final int status, final String description) {
        return Response.status(status).entity(new Error(status, description)).build();
    }

}