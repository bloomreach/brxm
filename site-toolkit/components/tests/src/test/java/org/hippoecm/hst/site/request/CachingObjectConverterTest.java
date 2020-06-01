/*
 * Copyright 2013-2016 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.site.request;

import javax.jcr.Node;
import javax.jcr.Session;

import org.hippoecm.hst.content.beans.manager.ObjectConverter;
import org.hippoecm.hst.content.beans.standard.HippoFolder;
import org.junit.Before;
import org.junit.Test;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.reset;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

public class CachingObjectConverterTest {

    private static final int NODE_SIZE = 10;

    private ObjectConverter objectConverter;
    private Node [] nodes;
    private Session session;

    @Before
    public void setUp() throws Exception {
        objectConverter = createMock(ObjectConverter.class);
        nodes = new Node[NODE_SIZE];

        session = createMock(Session.class);
        expect(session.getUserID()).andReturn("admin").anyTimes();

        for (int i = 0; i < NODE_SIZE; i++) {
            nodes[i] = createMock(Node.class);
            expect(nodes[i].getSession()).andReturn(session).anyTimes();
        }
    }

    @Test
    public void testThatNullIsReturnedTwiceWhenAskingForANullObject() throws Exception {
        Node node = nodes[0];
        expect(node.getPath()).andReturn("/null").anyTimes();
        expect(objectConverter.getObject(node)).andReturn(null).once();

        CachingObjectConverter cachingObjectConverter = createCachingObjectConverter();

        Object obj1 = cachingObjectConverter.getObject(node);
        Object obj2 = cachingObjectConverter.getObject(node);

        assertNull(obj1);
        assertNull(obj2);
    }

    @Test
    public void testThatCachingObjectConverterOnlyDelegatesToDelegateeOncePerNode() throws Exception {
        Node node = nodes[0];
        expect(node.getPath()).andReturn("/content/documents").anyTimes();
        HippoFolder folderBean = new HippoFolder();
        expect(objectConverter.getObject(node)).andReturn(folderBean).once();

        CachingObjectConverter cachingObjectConverter = createCachingObjectConverter();

        Object obj1 = cachingObjectConverter.getObject(node);
        Object obj2 = cachingObjectConverter.getObject(node);

        assertEquals(folderBean, obj1);
        assertEquals(folderBean, obj2);
    }

    @Test
    public void testCacheSize_withDefaults() throws Exception {
        HippoFolder [] folderBeans = new HippoFolder[NODE_SIZE];

        for (int i = 0; i < NODE_SIZE; i++) {
            Node node = nodes[i];
            expect(node.getPath()).andReturn("/content/documents/folder-" + i).anyTimes();
        }

        for (int i = 0; i < NODE_SIZE; i++) {
            folderBeans[i] = new HippoFolder();
            expect(objectConverter.getObject(nodes[i])).andReturn(folderBeans[i]).anyTimes();
        }

        CachingObjectConverter cachingObjectConverter = createCachingObjectConverter();

        Object [] objects = new Object[NODE_SIZE];

        for (int i = 0; i < NODE_SIZE; i++) {
            objects[i] = cachingObjectConverter.getObject(nodes[i]);
            assertEquals(folderBeans[i], objects[i]);
        }

        // Even if it's reset to return new HippoFolder objects from here,
        // because cachingObjectConverter is supposed to have cached all for the nodes,
        // it should return the same object as above in the end.

        reset(objectConverter);

        for (int i = 0; i < NODE_SIZE; i++) {
            folderBeans[i] = new HippoFolder();
            expect(objectConverter.getObject(nodes[i])).andReturn(folderBeans[i]).anyTimes();
        }

        replay(objectConverter);

        for (int i = 0; i < NODE_SIZE; i++) {
            Object obj = cachingObjectConverter.getObject(nodes[i]);
            assertSame(objects[i], obj);
        }
    }

    @Test
    public void testCacheSize_withCacheSizeHalfOfNodeSize() throws Exception {
        HippoFolder [] folderBeans = new HippoFolder[NODE_SIZE];

        for (int i = 0; i < NODE_SIZE; i++) {
            Node node = nodes[i];
            expect(node.getPath()).andReturn("/content/documents/folder-" + i).anyTimes();
        }

        for (int i = 0; i < NODE_SIZE; i++) {
            folderBeans[i] = new HippoFolder();
            expect(objectConverter.getObject(nodes[i])).andReturn(folderBeans[i]).anyTimes();
        }

        final int cacheSize = NODE_SIZE / 2;
        CachingObjectConverter cachingObjectConverter = createCachingObjectConverter(cacheSize);

        Object [] objects = new Object[NODE_SIZE];

        for (int i = 0; i < NODE_SIZE; i++) {
            objects[i] = cachingObjectConverter.getObject(nodes[i]);
            assertEquals(folderBeans[i], objects[i]);
        }

        // Now when it's reset to return new HippoFolder objects from here,
        // because cachingObjectConverter is supposed to have cached half of the node size only,
        // it should return the same objects for the last half, but different objects for the rest.

        reset(objectConverter);

        for (int i = 0; i < NODE_SIZE; i++) {
            folderBeans[i] = new HippoFolder();
            expect(objectConverter.getObject(nodes[i])).andReturn(folderBeans[i]).anyTimes();
        }

        replay(objectConverter);

        for (int i = NODE_SIZE - cacheSize; i < NODE_SIZE; i++) {
            Object obj = cachingObjectConverter.getObject(nodes[i]);
            assertSame(objects[i], obj);
        }

        for (int i = 0; i < NODE_SIZE - cacheSize; i++) {
            Object obj = cachingObjectConverter.getObject(nodes[i]);
            assertNotSame(objects[i], obj);
        }
    }

    private CachingObjectConverter createCachingObjectConverter() {
        return createCachingObjectConverter(-1);
    }

    private CachingObjectConverter createCachingObjectConverter(final int maxCacheSize) {
        replay(session, objectConverter);
        replay((Object []) nodes);

        if (maxCacheSize >= 0) {
            return new CachingObjectConverter(objectConverter, maxCacheSize);
        } else {
            return new CachingObjectConverter(objectConverter);
        }
    }

}
