/*
 * Copyright 2019-2021 Hippo B.V. (http://www.onehippo.com)
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
 *
 */

package org.onehippo.repository.jaxrs.api;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class JsonResourceServiceModuleTest {

    private ObjectMapper objectMapper;

    @Before
    public void setUp() {
        objectMapper = new JsonResourceServiceModule() {
            @Override
            protected Object getRestResource(final SessionRequestContextProvider sessionRequestContextProvider) {
                return null;
            }
        }.createObjectMapper();
    }

    @Test
    public void test_Iso8601_calendar() throws IOException {

        final Calendar timestamp = new GregorianCalendar();
        final String jsonStringRepresentation = objectMapper.writeValueAsString(timestamp);
        final Calendar converted = objectMapper.readValue(jsonStringRepresentation, Calendar.class);
        assertThat(converted.getTimeInMillis(), is(timestamp.getTimeInMillis()));

        // object mapper produces quoted strings in UTC timezone
        final SimpleDateFormat df = new SimpleDateFormat("\"yyyy-MM-dd'T'HH:mm:ss.SSSZ\"");
        df.setTimeZone(TimeZone.getTimeZone("UTC"));
        assertThat(jsonStringRepresentation, is(df.format(Date.from(timestamp.toInstant()))));
    }

    @Test
    public void test_Iso8601_date_keys_in_map() throws IOException {

        final Date timestamp = Date.from(Instant.ofEpochMilli(0));
        final Map<Date, Long> map = new HashMap<>();
        map.put(timestamp, 0L);

        final String jsonStringRepresentation = objectMapper.writeValueAsString(map);
        final Map converted = objectMapper.readValue(jsonStringRepresentation, Map.class);
        final String timestampString = "\"" + converted.keySet().stream().findFirst().orElse(null) + "\"";
        assertThat(timestampString, is(objectMapper.writeValueAsString(timestamp)));
        assertThat(timestampString, is("\"1970-01-01T00:00:00.000+0000\""));

    }

}