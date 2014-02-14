/*
 * Copyright 2014 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.cms7.essentials.rest.model;

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;
import org.junit.Test;
import org.onehippo.cms7.essentials.dashboard.rest.KeyValueRestful;
import org.onehippo.cms7.essentials.dashboard.rest.PostPayloadRestful;
import org.onehippo.cms7.essentials.dashboard.rest.RestfulList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertEquals;

/**
 * @version "$Id$"
 */
public class RestListTest {

    private static Logger log = LoggerFactory.getLogger(RestListTest.class);

    @Test
    public void testList() throws Exception {

        final ObjectMapper mapper = new ObjectMapper();

        final RestfulList<KeyValueRestful> keyValue = new RestfulList<>();
        keyValue.add(new KeyValueRestful("test", "test"));
        keyValue.add(new KeyValueRestful("test1", "test1"));
        String result = mapper.writeValueAsString(keyValue);
        log.info("{}", result);
        @SuppressWarnings("unchecked")
        final RestfulList<KeyValueRestful> myList = mapper.readValue(result, new TypeReference<RestfulList<KeyValueRestful>>() {
        });
        assertEquals(2, myList.getItems().size());
        //mix of implementations:
        final RestList<KeyValueRestful> listKeyValue = new RestList<>();
        listKeyValue.add(new KeyValueRestful("test", "test"));
        listKeyValue.add(new KeyValueRestful("test2", "test2"));
        result = mapper.writeValueAsString(keyValue);
        log.info("{}", result);
        final RestfulList<KeyValueRestful> List = mapper.readValue(result, new TypeReference<RestfulList<KeyValueRestful>>() {
        });
        assertEquals(2, List.getItems().size());
        // payload
        RestfulList<PostPayloadRestful> payloadList = new RestfulList<>();
        final PostPayloadRestful resource = new PostPayloadRestful();
        payloadList.add(resource);
        resource.add("test", "test");
        resource.add("test1", "test1");
        result = mapper.writeValueAsString(payloadList);
        log.info("result {}", result);
        payloadList = mapper.readValue(result, new TypeReference<RestfulList<PostPayloadRestful>>() {
        });

        assertEquals(1, payloadList.getItems().size());
        PostPayloadRestful postPayloadRestful = payloadList.getItems().get(0);
        assertEquals(2, postPayloadRestful.getValues().size());
        result = "{\"items\":[{\"values\":{\"path\":\"/hippo:namespaces/hippostd/html/editor:templates/_default_/root\",\"property\":\"Xinha.config.toolbar\"}}]}";
        payloadList = mapper.readValue(result, new TypeReference<RestfulList<PostPayloadRestful>>() {
        });
        assertEquals(1, payloadList.getItems().size());
        postPayloadRestful = payloadList.getItems().get(0);
        assertEquals(2, postPayloadRestful.getValues().size());
        // test payload:
        postPayloadRestful = new PostPayloadRestful();
        postPayloadRestful.add("testKey", "testValue");
        result = mapper.writeValueAsString(postPayloadRestful);
        log.info("postPayloadRestful {}", result);
    }

}
