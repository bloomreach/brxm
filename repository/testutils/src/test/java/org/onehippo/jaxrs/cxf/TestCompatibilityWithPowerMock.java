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

import javax.ws.rs.core.Response;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.junit.Assert.assertEquals;
import static org.powermock.api.mockito.PowerMockito.doReturn;
import static org.powermock.api.mockito.PowerMockito.spy;

@RunWith(PowerMockRunner.class)
@PrepareForTest(TestCompatibilityWithPowerMock.ToBeMocked.class)
public class TestCompatibilityWithPowerMock extends CXFTest {

    public class ToBeMocked {
        private String privateImplementation() {
            return "original";
        }
        public String func() {
            return privateImplementation();
        }
    }

    @Before
    public void setup() {
        setup(HelloWorldEndpoint.class);
    }

    @Test
    public void testHelloWorld() {
        Response r = createClient("/helloworld").get();
        assertEquals(Response.Status.OK.getStatusCode(), r.getStatus());
        assertEquals("Hello world", r.readEntity(String.class));
    }

    @Test
    public void testPowerMock() throws Exception {
        ToBeMocked myMock = spy(new ToBeMocked());
        assertEquals("original", myMock.func());
        doReturn("mocked").when(myMock, "privateImplementation");
        assertEquals("mocked", myMock.func());
    }
}
