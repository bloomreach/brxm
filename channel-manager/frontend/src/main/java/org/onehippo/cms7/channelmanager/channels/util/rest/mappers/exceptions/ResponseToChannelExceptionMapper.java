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
package org.onehippo.cms7.channelmanager.channels.util.rest.mappers.exceptions;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;

import org.apache.commons.lang.StringUtils;
import org.apache.cxf.jaxrs.client.ResponseExceptionMapper;
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

            if (StringUtils.isNotBlank(typeName)) {
                return new ChannelException("", ChannelException.Type.valueOf(typeName));
            }
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

}
