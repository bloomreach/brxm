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
package org.onehippo.jaxrs.cxf.hst;

import org.hippoecm.hst.core.request.HstRequestContext;
import org.junit.Before;
import org.junit.Test;
import org.onehippo.jaxrs.cxf.CXFTest;

import static com.jayway.restassured.RestAssured.when;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.hamcrest.core.IsEqual.equalTo;

public class TestHstCXFTestFixtureHelper extends CXFTest {

    @Before
    public void setup() {
        HstRequestContext context = createMock(HstRequestContext.class);
        expect(context.getAttribute(anyObject())).andReturn("Hello from context").anyTimes();
        replay(context);

        HstCXFTestFixtureHelper helper = new HstCXFTestFixtureHelper(context);
        Config config = createDefaultConfig()
                .addServerClass(HelloFromContextEndpoint.class)
                .addServerSingleton(helper);
        setup(config);
    }

    @Test
    public void callingHelloWorldMustSucceed() {
        when().
                get("/hellofromcontext").
        then().
                statusCode(200).
                body(equalTo("Hello from context"));
    }
}
