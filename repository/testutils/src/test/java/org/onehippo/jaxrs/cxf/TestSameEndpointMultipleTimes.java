/*
 *  Copyright 2015-2017 Hippo B.V. (http://www.onehippo.com)
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

import org.apache.cxf.jaxrs.impl.WebApplicationExceptionMapper;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;
import org.junit.Before;
import org.junit.Test;
import org.onehippo.testutils.log4j.Log4jInterceptor;

import static org.hamcrest.core.IsEqual.equalTo;

public class TestSameEndpointMultipleTimes extends CXFTest {

    @Before
    public void setup() {
        setup(HelloWorldEndpoint.class);
    }

    @Test
    public void callingHelloWorldMustSucceed() {
        when()
                .get("/helloworld")
        .then()
                .statusCode(200)
                .body(equalTo("Hello world"));
    }

    @Test
    public void callingHelloWorldAgainMustSucceed() {
        when()
                .get("/helloworld")
        .then()
                .statusCode(200)
                .body(equalTo("Hello world"));
    }

    @Test
    public void makingIllegalCallMustBeSuppressible() {
        // Suppress warning for calling non-existing verb
        try (Log4jInterceptor ignored = Log4jInterceptor.onWarn()
                .deny(JAXRSUtils.class, WebApplicationExceptionMapper.class)
                .build()) {
            when()
                    .delete("/helloworld")
            .then()
                    .statusCode(405);
        }
    }
}
