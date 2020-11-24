/*
 *  Copyright 2015-2020 Hippo B.V. (http://www.onehippo.com)
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
import org.powermock.api.easymock.PowerMock;
import org.powermock.api.support.membermodification.MemberMatcher;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertEquals;
import static org.powermock.api.easymock.PowerMock.mockStaticPartial;
import static org.powermock.api.easymock.PowerMock.replayAll;
import static org.powermock.api.easymock.PowerMock.verifyAll;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"org.apache.logging.log4j.*", "javax.management.*", "javax.net.ssl.*"})
@PrepareForTest(ClassWithStaticMembers.class)
public class TestCompatibilityWithPowerMock extends CXFTest {

    @Before
    public void setup() {
        setup(HelloWorldEndpoint.class);
    }

    @Test
    public void callingHelloWorldInCombinationWithPowerMockMustSucceed() {
        when()
                .get("/helloworld")
        .then()
                .statusCode(200)
                .body(equalTo("Hello world"));
    }

    @Test
    public void unmockedCallMustReturnOriginal() {
        assertEquals("Unmocked call must return original", "original", ClassWithStaticMembers.func());
    }

    @Test
    public void mockedCallMustReturnMockedAnswer() throws Exception {
        mockStaticPartial(ClassWithStaticMembers.class, "privateImplementation");
        PowerMock.expectPrivate(
                ClassWithStaticMembers.class,
                MemberMatcher.method(ClassWithStaticMembers.class, "privateImplementation")
        ).andReturn("mocked");

        replayAll();

        assertEquals("Mocked should return mocked", "mocked", ClassWithStaticMembers.func());

        verifyAll();
    }
}
