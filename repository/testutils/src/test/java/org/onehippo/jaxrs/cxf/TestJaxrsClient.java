/*
 *  Copyright 2015 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.jaxrs.cxf;

import java.time.LocalDateTime;

import javax.ws.rs.core.Response;

import org.junit.Before;
import org.junit.Test;
import org.onehippo.jaxrs.cxf.HelloObjectEndpoint.StructuredMessage;

import static com.jayway.restassured.RestAssured.when;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertEquals;

public class TestJaxrsClient extends CXFTest {

    @Before
    public void setup() {
        setup(HelloObjectEndpoint.class);
    }

    @Test
    public void callingUsingRestAssuredMustSucceed() {
        when().
                get("/helloobject").
        then().
                statusCode(200).
                body("message", equalTo("Hello object"),
                     "timestamp.year", equalTo(LocalDateTime.now().getYear()));
    }

    @Test
    public void callingUsingJaxrsClientMustSucceed() {
        final Response response = createJaxrsClient("/helloobject").get();

        assertEquals(200, response.getStatus());

        StructuredMessage message = response.readEntity(StructuredMessage.class);

        assertEquals("Hello object", message.getMessage());
        assertEquals(LocalDateTime.now().getYear(), message.getTimestamp().getYear());
    }
}
