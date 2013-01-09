/*
 *  Copyright 2012-2013 Hippo B.V. (http://www.onehippo.com)
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

import java.io.IOException;
import java.io.StringWriter;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerator;
import org.hippoecm.hst.configuration.channel.ChannelException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An {@link ExceptionMapper} which maps a {@link ChannelException} to a proper HTTP error
 */
@Provider
public class ChannelExceptionMapper implements ExceptionMapper<ChannelException> {

    private static final Logger log = LoggerFactory.getLogger(ChannelExceptionMapper.class.getName());

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
                .entity(buildJsonString(exception))
                .build();
    }

    protected String buildJsonString(ChannelException exception) {
        try {
            StringWriter jsonObjectWriter = new StringWriter();
            JsonGenerator jsonGenerator = new JsonFactory().createJsonGenerator(jsonObjectWriter);

            jsonGenerator.writeStartObject();
            jsonGenerator.writeObjectFieldStart("error");
            jsonGenerator.writeStringField("message", exception.getMessage());
            jsonGenerator.writeArrayFieldStart("parameters");
            String[] parameters = exception.getParameters();
            if (parameters != null) {
                for (String parameter : parameters) {
                    jsonGenerator.writeString(parameter);
                }
            }

            jsonGenerator.writeEndArray();
            jsonGenerator.writeEndObject();
            jsonGenerator.writeEndObject();
            jsonGenerator.flush();
            return jsonObjectWriter.toString();
        } catch (IOException ioe) {
            log.debug("Could not generate JSON object", ioe);
            return "";
        }
    }

}
