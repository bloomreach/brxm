/*
 * Copyright 2018-2020 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms.channelmanager.content.document.util;

import java.util.Optional;

import javax.jcr.Node;
import javax.jcr.Session;


import org.hippoecm.repository.util.DocumentUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.onehippo.cms.channelmanager.content.error.ErrorInfo;
import org.onehippo.cms.channelmanager.content.error.NotFoundException;
import org.powermock.api.easymock.PowerMock;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static junit.framework.TestCase.fail;
import static org.easymock.EasyMock.expect;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.powermock.api.easymock.PowerMock.createMock;
import static org.powermock.api.easymock.PowerMock.replayAll;
import static org.powermock.api.easymock.PowerMock.verifyAll;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"org.apache.logging.log4j.*", "javax.management.*", "com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*", "org.w3c.dom.*", "com.sun.org.apache.xalan.*", "javax.activation.*", "javax.net.ssl.*"})
@PrepareForTest({
        DocumentUtils.class,
})

public class DocumentHandleUtilsTest {

    private Session session;

    @Before
    public void setup() {
        session = createMock(Session.class);
        PowerMock.mockStatic(DocumentUtils.class);
    }

    @Test
    public void testValidHandleExists() throws Exception {
        final String uuid = "uuid";
        final Node node = createMock(Node.class);
        final String notDeletedNodeType = "notHippo:deleted";

        expect(DocumentUtils.getHandle(uuid, session)).andReturn(Optional.of(node));
        expect(DocumentUtils.getVariantNodeType(node)).andReturn(Optional.of(notDeletedNodeType));
        replayAll();

        assertThat(DocumentHandleUtils.getHandle(uuid, session), is(node));

        verifyAll();
    }

    @Test
    public void testInvalidHandleExists() throws Exception {
        final String uuid = "uuid";
        final Node node = createMock(Node.class);
        final String deletedNodeType = "hippo:deleted";

        expect(DocumentUtils.getHandle(uuid, session)).andReturn(Optional.of(node));
        expect(DocumentUtils.getVariantNodeType(node)).andReturn(Optional.of(deletedNodeType));
        replayAll();

        try {
            DocumentHandleUtils.getHandle(uuid, session);
            fail("No Exception");
        } catch (NotFoundException e) {
            assertThat(((ErrorInfo) e.getPayload()).getReason(), is(ErrorInfo.Reason.DOES_NOT_EXIST));
        }

        verifyAll();
    }

    @Test
    public void testValidHandleDoesNotExist() throws Exception {
        final String uuid = "uuid";

        expect(DocumentUtils.getHandle(uuid, session)).andReturn(Optional.empty());
        replayAll();

        try {
            DocumentHandleUtils.getHandle(uuid, session);
            fail("No Exception");
        } catch (NotFoundException e) {
            assertThat(((ErrorInfo) e.getPayload()).getReason(), is(ErrorInfo.Reason.DOES_NOT_EXIST));
        }

        verifyAll();
    }
}
