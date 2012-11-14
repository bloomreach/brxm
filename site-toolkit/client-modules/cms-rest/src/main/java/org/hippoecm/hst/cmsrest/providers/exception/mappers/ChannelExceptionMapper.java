/*
 *  Copyright 2012 Hippo.
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
package org.hippoecm.hst.cmsrest.providers.exception.mappers;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.hippoecm.hst.configuration.channel.ChannelException;

/**
 * An {@link ExceptionMapper} which maps a {@link ChannelException} to a proper HTTP error
 */
@Provider
public class ChannelExceptionMapper implements ExceptionMapper<ChannelException> {

    private static final String APPLICATION_EXCEPTION_MEDIA_TYPE = "application/x-exception";
    private static final String EXCEPTION_CLASS_MEDIA_TYPE_PARAMETER = "class=" + ChannelException.class.getName();
    private static final String EXCEPTION_TYPE_MEDIA_TYPE_PARAMETER = "type=";

    /**
     * Map a {@link ChannelException} to a proper JAX-RS {@link Response}
     *
     * @param exception The exception to map to a response
     * @return A response mapped from the supplied exception
     */
    @Override
    public Response toResponse(final ChannelException exception) {
        return Response.status(Response.Status.BAD_REQUEST).type(APPLICATION_EXCEPTION_MEDIA_TYPE + "; "
                + EXCEPTION_CLASS_MEDIA_TYPE_PARAMETER + "; " + EXCEPTION_TYPE_MEDIA_TYPE_PARAMETER + exception.getType().name())
                .build();
    }

}
