/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;

import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.easymock.classextension.ConstructorArgs;
import org.junit.Test;

import static org.easymock.EasyMock.expect;
import static org.easymock.classextension.EasyMock.createMock;
import static org.easymock.classextension.EasyMock.replay;
import static org.easymock.classextension.EasyMock.verify;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Jettro Coenradie
 * @author Jeroen Reijn
 */
public class ImageItemTest {
    private static final String EMPTY_UUID = "";
    private static final String NULL_UUID = null;
    private static final String EXISTING_UUID = "existinguuid";
    private static final String EXISTING_NODE_NAME = "/existing node name";
    private static final String PATH_TO_IMAGE = "/path/to/image/in/binaries";


    @Test
    public void getPrimaryUrlWithoutUuid() throws NoSuchMethodException {
        Constructor defaultConstructor = ImageItem.class.getDeclaredConstructor();
        defaultConstructor.setAccessible(true);
        ImageItem imageItem = createMock("ImageItem", ImageItem.class,
                new ConstructorArgs(defaultConstructor)
                , ImageItem.class.getDeclaredMethod("obtainJcrSession"));

        Object[] mocks = new Object[]{imageItem};
        replay(mocks);

        assertEquals("", imageItem.getPrimaryUrl());

        verify(mocks);
    }

    @Test
    public void getStartsWithConfiguration() throws NoSuchMethodException {
        List<String> startsWith = new ArrayList<String>();//Arrays.asList(config.getStringArray("sections"));

        String[] array = new String[]{"/content/gallery", "/content/videos"};


        startsWith.add("/content/gallery/");
        startsWith.add("");
        startsWith.add(null);
        startsWith.add("/content/videos/");

        assertTrue(arrayContainsStartWith(array, "/content/videos/bla"));
        assertTrue(arrayContainsStartWith(array, "/content/gallery/bla"));
        assertFalse(arrayContainsStartWith(array, "/content/assets/bla"));
    }

    public boolean arrayContainsStartWith(List<String> list, String path) {
        for (int i = 0; i < list.size(); i++) {
            if (path.startsWith(list.get(i)))
                return (i >= 0);
        }
        return false;
    }

    public boolean arrayContainsStartWith(String[] array, String path) {
        for (int i = 0; i < array.length; i++) {
            if (path.startsWith(array[i]))
                return (i >= 0);
        }
        return false;
    }


    @Test
    public void getPrimaryUrlWithEmptyUuid() throws NoSuchMethodException {
        ImageItem imageItem = createPartialMockImageItem(EMPTY_UUID);

        Object[] mocks = new Object[]{imageItem};
        replay(mocks);

        assertEquals("", imageItem.getPrimaryUrl());

        verify(mocks);
    }

    @Test
    public void getPrimaryUrlWithNullUuid() throws NoSuchMethodException {
        ImageItem imageItem = createPartialMockImageItem(NULL_UUID);

        Object[] mocks = new Object[]{imageItem};
        replay(mocks);

        assertEquals("", imageItem.getPrimaryUrl());

        verify(mocks);
    }

    /**
     * This is the happy path test where everything should be fine and result in a String containing
     * the primary url of an image node.
     *
     * @throws NoSuchMethodException should not be thrown
     * @throws RepositoryException   should not be thrown
     */
    // @Test
    public void getPrimaryUrlWithExistingUuid() throws NoSuchMethodException, RepositoryException {
        ImageItem imageItem = createPartialMockImageItem(EXISTING_UUID);
        Session mockSession = createMock("session", Session.class);
        Node mockHandleForUUID = createMock(Node.class);
        Node mockActualNode = createMock(Node.class);
        Item mockPrimaryItem = createMock(Item.class);


        expect(imageItem.obtainJcrSession()).andReturn(mockSession);
        expect(mockSession.getNodeByIdentifier(EXISTING_UUID)).andReturn(mockHandleForUUID);
        expect(mockHandleForUUID.getName()).andReturn(EXISTING_NODE_NAME);
        expect(mockHandleForUUID.getNode(EXISTING_NODE_NAME)).andReturn(mockActualNode);
        expect(mockActualNode.getPrimaryItem()).andReturn(mockPrimaryItem);
        expect(mockPrimaryItem.getPath()).andReturn(PATH_TO_IMAGE);

        Object[] mocks = new Object[]{imageItem, mockSession, mockHandleForUUID, mockActualNode, mockPrimaryItem};
        replay(mocks);

        assertEquals(ImageItem.BASE_PATH_BINARIES + PATH_TO_IMAGE, imageItem.getPrimaryUrl());

        verify(mocks);

    }

    @Test
    public void getPrimaryUrlWithExistingUuidAndpathToRoot() throws NoSuchMethodException, RepositoryException {
        ImageItem imageItem = createPartialMockImageItem(EXISTING_UUID);
        Session mockSession = createMock("session", Session.class);
        Node mockHandleForUUID = createMock(Node.class);

        expect(imageItem.obtainJcrSession()).andReturn(mockSession);
        expect(mockSession.getNodeByIdentifier(EXISTING_UUID)).andReturn(mockHandleForUUID);
        expect(mockHandleForUUID.getName()).andReturn("/");

        Object[] mocks = new Object[]{imageItem, mockSession, mockHandleForUUID};
        replay(mocks);

        assertEquals("", imageItem.getPrimaryUrl());

        verify(mocks);

    }

    private ImageItem createPartialMockImageItem(String constructorArg) throws NoSuchMethodException {
        Constructor constructor = ImageItem.class.getDeclaredConstructor(String.class);
        constructor.setAccessible(true);

        return createMock("ImageItem", ImageItem.class,
                new ConstructorArgs(constructor, constructorArg)
                , ImageItem.class.getDeclaredMethod("obtainJcrSession"));
    }


}