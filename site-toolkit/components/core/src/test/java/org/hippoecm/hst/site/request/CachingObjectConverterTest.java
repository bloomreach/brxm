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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class CachingObjectConverterTest {

    private ObjectConverter objectConverter;
    private Node node;
    private Session session;

    @Before
    public void setUp() throws Exception {
        objectConverter = createMock(ObjectConverter.class);
        node = createMock(Node.class);
        session = createMock(Session.class);

        expect(session.getUserID()).andReturn("admin").anyTimes();
        expect(node.getSession()).andReturn(session).anyTimes();
    }

    @Test
    public void testThatNullIsReturnedTwiceWhenAskingForANullObject() throws Exception {
        expect(node.getPath()).andReturn("/null").anyTimes();
        expect(objectConverter.getObject(node)).andReturn(null).once();

        CachingObjectConverter cachingObjectConverter = createCachingObjectConverter();

        Object obj1 = cachingObjectConverter.getObject(node);
        Object obj2 = cachingObjectConverter.getObject(node);

        assertNull(obj1);
        assertNull(obj2);
    }

    private CachingObjectConverter createCachingObjectConverter() {
        replay(session, node, objectConverter);

        return new CachingObjectConverter(objectConverter);
    }

    @Test
    public void testThatCachingObjectConverterOnlyDelegatesToDelegateeOncePerNode() throws Exception {
        expect(node.getPath()).andReturn("/content/documents").anyTimes();
        HippoFolder folderBean = new HippoFolder();
        expect(objectConverter.getObject(node)).andReturn(folderBean).once();

        CachingObjectConverter cachingObjectConverter = createCachingObjectConverter();

        Object obj1 = cachingObjectConverter.getObject(node);
        Object obj2 = cachingObjectConverter.getObject(node);

        assertEquals(folderBean, obj1);
        assertEquals(folderBean, obj2);
    }
}
