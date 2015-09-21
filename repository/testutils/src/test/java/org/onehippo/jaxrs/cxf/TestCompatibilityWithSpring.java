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

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.hamcrest.core.IsEqual.equalTo;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:/org/onehippo/jaxrs/cxf/spring.xml"})
public class TestCompatibilityWithSpring extends CXFTest {

    @Autowired
    HelloWorldEndpoint helloWorldEndpoint;

    @Before
    public void setup() {
        setup(helloWorldEndpoint);
    }

    @Test
    public void callingHelloWorldInjectedThroughSpringMustSucceed() {
        when()
                .get("/helloworld")
        .then()
                .statusCode(200)
                .body(equalTo("Hello world"));
    }
}
