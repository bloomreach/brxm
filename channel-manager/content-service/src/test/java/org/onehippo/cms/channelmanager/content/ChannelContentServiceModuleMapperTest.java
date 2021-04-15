/*
 * Copyright 2021 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms.channelmanager.content;

import java.text.SimpleDateFormat;
import java.time.temporal.ChronoUnit;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import org.junit.Before;
import org.junit.Test;
import org.onehippo.cms.channelmanager.content.document.model.Version;
import org.onehippo.repository.campaign.Campaign;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class ChannelContentServiceModuleMapperTest {

    private ObjectMapper objectMapper;

    @Before
    public void setUp() {
        objectMapper = new ChannelContentServiceModule().createObjectMapper();
    }


    @Test
    public void test_object_mapper_dates_campaign_and_label() throws JsonProcessingException {

        final Calendar timestamp = new GregorianCalendar();
        final String jsonStringRepresentation = objectMapper.writeValueAsString(timestamp);

        // the converted time should be without milliseconds
        final Calendar converted = objectMapper.readValue(jsonStringRepresentation, Calendar.class);
        assertThat(converted.toInstant().truncatedTo(ChronoUnit.SECONDS), is(timestamp.toInstant().truncatedTo(ChronoUnit.SECONDS)));

        // object mapper produces quoted strings in UTC timezone
        final SimpleDateFormat df = new SimpleDateFormat("\"yyyy-MM-dd'T'HH:mm:ss'Z'\"");
        df.setTimeZone(TimeZone.getTimeZone("UTC"));
        assertThat(jsonStringRepresentation, is(df.format(Date.from(timestamp.toInstant()))));

        Version version = new Version(timestamp, "admin", "uuid-123", true, "foo", "My Label",
                new Campaign("uuid-123", timestamp, timestamp));

        String serializedVersion = objectMapper.writeValueAsString(version);

        assertTrue(serializedVersion.contains("campaign"));
        assertTrue(serializedVersion.contains("label"));

        Version deserVersion = objectMapper.readValue(serializedVersion, Version.class);

        // assert the date format in deser is correct AND assert that the 'campaign' does not have a 'uuid' field
        // because of CampaignSerializationMixin

        assertThat(deserVersion.getTimestamp().toInstant().truncatedTo(ChronoUnit.SECONDS), is(timestamp.toInstant().truncatedTo(ChronoUnit.SECONDS)));
        assertThat(deserVersion.getCampaign().getFrom().toInstant().truncatedTo(ChronoUnit.SECONDS), is(timestamp.toInstant().truncatedTo(ChronoUnit.SECONDS)));
        assertThat(deserVersion.getCampaign().getTo().toInstant().truncatedTo(ChronoUnit.SECONDS), is(timestamp.toInstant().truncatedTo(ChronoUnit.SECONDS)));

        assertEquals(deserVersion.getLabel(), "My Label");
        assertNull("'uuid' of the campaign object should not be in the serialized version, see CampaignSerializationMixin",
                deserVersion.getCampaign().getUuid());

        // null label and null campaign should not be present in the serialization string
        version = new Version(timestamp, "admin", "uuid-123", true, "foo", null, null);
        serializedVersion = objectMapper.writeValueAsString(version);

        assertFalse(serializedVersion.contains("campaign"));
        assertFalse(serializedVersion.contains("label"));
    }
}
