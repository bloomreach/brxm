/*
 * Copyright 2017 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.cms7.essentials;

import java.io.IOException;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.cxf.jaxrs.ext.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public final class WebUtils {
    private static final Logger log = LoggerFactory.getLogger(WebUtils.class);
    public static final ObjectMapper JSON = new ObjectMapper();
    public static final byte[] EMPTY_BYTES = new byte[0];

    static {
        JSON.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    private WebUtils() {
    }


    public static <T> String toJson(final T object) {
        if (object == null) {
            return "";
        }

        try {
            return JSON.writeValueAsString(object);
        } catch (IOException e) {
            log.error("JSON error", e);
        }
        return "";
    }

    public static <T> byte[] toBytesJson(final T object) {
        if (object == null) {
            return EMPTY_BYTES;
        }

        try {
            return JSON.writeValueAsBytes(object);
        } catch (Exception e) {
            log.error("JSON error", e);
        }
        return EMPTY_BYTES;
    }


    @Nullable
    public static <T> T fromJson(final byte[] value, Class<T> clazz) {

        try {
            return JSON.readValue(value, clazz);
        } catch (Exception e) {
            log.error("JSON error", e);
        }
        return null;
    }

    @Nullable
    public static <T> T fromJson(final String value, Class<T> clazz) {

        try {
            return JSON.readValue(value, clazz);
        } catch (Exception e) {
            log.error("JSON error (see message below){}", value, e);
        }
        return null;
    }

}
