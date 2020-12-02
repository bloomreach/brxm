/*
 *  Copyright 2008-2020 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.addon.frontend.gallerypicker;

import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.hippoecm.frontend.model.JcrHelper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.easymock.PowerMock;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;
import static org.powermock.api.easymock.PowerMock.replayAll;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"org.apache.logging.log4j.*", "javax.management.*", "com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*", "org.w3c.dom.*", "com.sun.org.apache.xalan.*", "javax.activation.*", "javax.net.ssl.*"})
@PrepareForTest({JcrHelper.class})
public class ImageItemTest {
    private static final String EMPTY_UUID = "";
    private static final String NULL_UUID = null;
    private static final String EXISTING_UUID = "existinguuid";
    private static final String EXISTING_NODE_NAME = "/existing node name";
    private static final String PATH_TO_IMAGE = "/path/to/image/in/binaries";

    private Session mockSession;
    private MockSessionProvider mockSessionProvider;

    @Before
    public void setUp() {
        mockSession = createMock("session", Session.class);
        mockSessionProvider = new MockSessionProvider(mockSession);
    }

    @Test
    public void getPrimaryUrlWithoutUuid() throws NoSuchMethodException {
        ImageItem imageItem = new ImageItem();
        assertEquals("", imageItem.getPrimaryUrl(mockSessionProvider));
    }

    @Test
    public void getPrimaryUrlWithEmptyUuid() throws NoSuchMethodException {
        ImageItem imageItem = new ImageItem(EMPTY_UUID);
        assertEquals("", imageItem.getPrimaryUrl());
    }

    @Test
    public void getPrimaryUrlWithNullUuid() throws NoSuchMethodException {
        ImageItem imageItem = new ImageItem(NULL_UUID);
        assertEquals("", imageItem.getPrimaryUrl());
    }

    /**
     * This is the happy path test where everything should be fine and results in a String containing
     * the primary url of an image node.
     */
    @Test
    public void getPrimaryUrlWithExistingUuid() throws RepositoryException {
        PowerMock.mockStatic(JcrHelper.class);

        ImageItem imageItem = new ImageItem(EXISTING_UUID);
        Node mockHandleForUUID = createMock(Node.class);
        Node mockActualNode = createMock(Node.class);
        Item mockPrimaryItem = createMock(Item.class);

        expect(mockSession.getNodeByIdentifier(EXISTING_UUID)).andReturn(mockHandleForUUID);
        expect(mockHandleForUUID.getName()).andReturn(EXISTING_NODE_NAME);
        expect(mockHandleForUUID.getNode(EXISTING_NODE_NAME)).andReturn(mockActualNode);
        expect(JcrHelper.getPrimaryItem(mockActualNode)).andReturn(mockPrimaryItem);
        expect(mockPrimaryItem.getPath()).andReturn(PATH_TO_IMAGE);

        Object[] mocks = {mockSession, mockHandleForUUID, mockActualNode, mockPrimaryItem};
        replayAll(mocks);

        assertEquals(ImageItem.BASE_PATH_BINARIES + PATH_TO_IMAGE, imageItem.getPrimaryUrl(mockSessionProvider));

        verify(mocks);
    }

    @Test
    public void getPrimaryUrlWithExistingUuidAndpathToRoot() throws RepositoryException {
        ImageItem imageItem = new ImageItem(EXISTING_UUID);
        Node mockHandleForUUID = createMock(Node.class);

        expect(mockSession.getNodeByIdentifier(EXISTING_UUID)).andReturn(mockHandleForUUID);
        expect(mockHandleForUUID.getName()).andReturn("/");

        Object[] mocks = {mockSession, mockHandleForUUID};
        replay(mocks);

        assertEquals("", imageItem.getPrimaryUrl(mockSessionProvider));

        verify(mocks);
    }
}