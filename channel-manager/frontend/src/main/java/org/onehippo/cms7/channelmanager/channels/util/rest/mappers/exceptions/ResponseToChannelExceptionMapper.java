/*
 *  Copyright 2012-2014 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms7.channelmanager.channels.util.rest.mappers.exceptions;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;

import org.apache.commons.lang.StringUtils;
import org.apache.cxf.jaxrs.client.ResponseExceptionMapper;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.hippoecm.hst.configuration.channel.ChannelException;

public class ResponseToChannelExceptionMapper implements ResponseExceptionMapper<ChannelException>, Serializable {

    /**
     * Converts a JAX-RS {@link javax.ws.rs.core.Response} to {@link ChannelException}
     *
     * @param response JAX-RS Response
     * @return Mapped exception instance, can be null
     */
    @Override
    public ChannelException fromResponse(final Response response) {
        List<Object> values = response.getMetadata().get(HttpHeaders.CONTENT_TYPE);

        if (values != null && !values.isEmpty()) {
            final String typeName = parseExceptionInfo((String) values.get(0)).get("type");
            final Object entity = response.getEntity();
            String message = "";
            ChannelException.Type channelExceptionType;
            String[] parameters = new String[0];

            if (StringUtils.isNotBlank(typeName)) {
                channelExceptionType = ChannelException.Type.valueOf(typeName);
            } else {
                channelExceptionType = ChannelException.Type.UNKNOWN;
            }

            if (entity instanceof InputStream) {
                final InputStream entityInputStream = (InputStream) entity;
                final ObjectMapper jsonMapper = new ObjectMapper();

                try {
                    // final JsonParser jsonParser = jsonMapper.readTree(entityInputStream);
                    final JsonNode jsonNode = jsonMapper.readTree(entityInputStream);
                    Object[] parsedInfo = parseChannelExceptionInfo(jsonNode);
                    message = (String) parsedInfo[0];
                    parameters = (String[]) parsedInfo[1];
                } catch (IOException ioe) {
                    message = "";
                    parameters = new String[0];
                }
            }

            return new ChannelException(message, channelExceptionType, parameters);
        }

        return new ChannelException("A channel operation related error happened for unknown reason. Please check with you administrator",
                ChannelException.Type.UNKNOWN);

    }

    private Map<String, String> parseExceptionInfo(final String exceptionInfo) {
        Map<String, String> parsedInfo = new HashMap<String, String>(1);

        for (String infoElement : exceptionInfo.split(";")) {
            final String[] elementParts = infoElement.trim().split("=");

            if (elementParts.length == 2) {
                parsedInfo.put(elementParts[0].trim(), elementParts[1].trim());
            }
        }

        return parsedInfo;
    }

    private Object[] parseChannelExceptionInfo(final JsonNode jsonNode) {
        Object[] parsedInfo = new Object[2];
        final JsonNode error = jsonNode.get("error");

        if (error != null && error.isObject()) {
            final JsonNode message = error.get("message");
            parsedInfo[0] = (message != null) ? message.getTextValue() : "";
            final JsonNode parameters = error.get("parameters");
            parsedInfo[1] = parseParameters(parameters);
        } else {
            parsedInfo[0] = "";
            parsedInfo[1] = new String[0];
        }
        return parsedInfo;
    }

    private String[] parseParameters(final JsonNode parameters) {
        if (parameters == null || !parameters.isArray()) {
            return new String[0];
        }

        final Iterator<JsonNode> paramsNodes = parameters.getElements();
        List<String> parsedParams = new ArrayList<String>(parameters.size());

        while(paramsNodes.hasNext()) {
            parsedParams.add(paramsNodes.next().getTextValue());
        }

        return parsedParams.toArray(new String[0]);
    }

}
