/*
 *  Copyright 2010-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.servlet.util;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Calendar;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeType;

import org.hippoecm.hst.servlet.utils.ResourceUtils;
import org.junit.Test;

public class ResourceUtilsTest {

    @Test
    public void testGetLastModifiedDate() throws RepositoryException {
        Property p = createMock(Property.class);
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(1234L);
        expect(p.getDate()).andReturn(c);

        Node n = createMock(Node.class);
        expect(n.getProperty(ResourceUtils.DEFAULT_BINARY_LAST_MODIFIED_PROP_NAME)).andReturn(p);

        replay(n, p);

        assertEquals(c.getTimeInMillis(), ResourceUtils.getLastModifiedDate(n,
                ResourceUtils.DEFAULT_BINARY_LAST_MODIFIED_PROP_NAME));
    }

    @Test
    public void testNoLastModifiedDate() throws RepositoryException {
        Node n = createMock(Node.class);
        expect(n.getProperty(ResourceUtils.DEFAULT_BINARY_LAST_MODIFIED_PROP_NAME)).andThrow(
                new PathNotFoundException("No such property (INTENTIONAL TEST ERROR"));

        replay(n);

        assertEquals(-1L, ResourceUtils.getLastModifiedDate(n, ResourceUtils.DEFAULT_BINARY_LAST_MODIFIED_PROP_NAME));
    }

    @Test
    public void testGetDataLength() throws RepositoryException {
        Property p = createMock(Property.class);
        expect(p.getLength()).andReturn(1234L);

        Node n = createMock(Node.class);
        expect(n.getProperty(ResourceUtils.DEFAULT_BINARY_DATA_PROP_NAME)).andReturn(p);

        replay(n, p);

        assertEquals(1234L, ResourceUtils.getDataLength(n, ResourceUtils.DEFAULT_BINARY_DATA_PROP_NAME));
    }

    @Test
    public void testGetDataLengthNoDataProperty() throws RepositoryException {
        Node n = createMock(Node.class);
        expect(n.getProperty(ResourceUtils.DEFAULT_BINARY_DATA_PROP_NAME)).andThrow(
                new PathNotFoundException("No such property"));

        replay(n);

        assertEquals(-1L, ResourceUtils.getDataLength(n, ResourceUtils.DEFAULT_BINARY_DATA_PROP_NAME));
    }

    @Test
    public void testHasDataProperty() throws RepositoryException {
        Node n = createMock(Node.class);
        expect(n.hasProperty(ResourceUtils.DEFAULT_BINARY_DATA_PROP_NAME)).andReturn(true);

        replay(n);

        assertTrue(ResourceUtils.hasBinaryProperty(n, ResourceUtils.DEFAULT_BINARY_DATA_PROP_NAME));
    }

    @Test
    public void testHasNoDataProperty() throws RepositoryException {
        Node n = createMock(Node.class);
        expect(n.hasProperty(ResourceUtils.DEFAULT_BINARY_DATA_PROP_NAME)).andReturn(false);
        expect(n.getPath()).andReturn("/binaries/test.pdf");

        replay(n);

        assertFalse(ResourceUtils.hasBinaryProperty(n, ResourceUtils.DEFAULT_BINARY_DATA_PROP_NAME));
    }

    @Test
    public void testHasMimeTypeProperty() throws RepositoryException {
        Node n = createMock(Node.class);
        expect(n.hasProperty(ResourceUtils.DEFAULT_BINARY_MIME_TYPE_PROP_NAME)).andReturn(true);

        replay(n);

        assertTrue(ResourceUtils.hasBinaryProperty(n, ResourceUtils.DEFAULT_BINARY_MIME_TYPE_PROP_NAME));
    }

    @Test
    public void testHasNoMimeTypeProperty() throws RepositoryException {
        Node n = createMock(Node.class);
        expect(n.hasProperty(ResourceUtils.DEFAULT_BINARY_MIME_TYPE_PROP_NAME)).andReturn(false);
        expect(n.getPath()).andReturn("/binaries/test.pdf");
        replay(n);

        assertFalse(ResourceUtils.hasBinaryProperty(n, ResourceUtils.DEFAULT_BINARY_MIME_TYPE_PROP_NAME));
    }

    @Test
    public void testHasValidType() throws RepositoryException {
        Node n = createMock(Node.class);
        expect(n.isNodeType(ResourceUtils.DEFAULT_BINARY_RESOURCE_NODE_TYPE)).andReturn(true);
        replay(n);

        assertTrue(ResourceUtils.hasValideType(n, ResourceUtils.DEFAULT_BINARY_RESOURCE_NODE_TYPE));
    }

    @Test
    public void testHasInValidType() throws RepositoryException {
        NodeType nt = createMock(NodeType.class);
        expect(nt.getName()).andReturn("my:type");
        Node n = createMock(Node.class);
        expect(n.isNodeType(ResourceUtils.DEFAULT_BINARY_RESOURCE_NODE_TYPE)).andReturn(false);
        expect(n.getPrimaryNodeType()).andReturn(nt);
        replay(nt, n);

        assertFalse(ResourceUtils.hasValideType(n, ResourceUtils.DEFAULT_BINARY_RESOURCE_NODE_TYPE));
    }

    @Test
    public void testGetFileName() throws RepositoryException {
        String[] fileNameProps = new String[] { "name1", "name2", "name3" };
        Property p = createMock(Property.class);

        expect(p.getString()).andReturn("test.pdf");

        Node n = createMock(Node.class);
        expect(n.hasProperty("name1")).andReturn(false);
        expect(n.hasProperty("name2")).andReturn(true);
        expect(n.getProperty("name2")).andReturn(p);

        replay(n, p);

        assertEquals("test.pdf", ResourceUtils.getFileName(n, fileNameProps));
    }

    @Test
    public void testNoFileName() throws RepositoryException {
        String[] fileNameProps = new String[] { "name1", "name2", "name3" };

        Node n = createMock(Node.class);
        expect(n.hasProperty("name1")).andReturn(false);
        expect(n.hasProperty("name2")).andReturn(false);
        expect(n.hasProperty("name3")).andReturn(false);

        replay(n);

        assertEquals(null, ResourceUtils.getFileName(n, fileNameProps));
    }

    @Test
    public void testResoucePathIsValid() {
        assertTrue(ResourceUtils.isValidResourcePath("/my/path/file.pdf"));
        assertFalse(ResourceUtils.isValidResourcePath("a/relative/path"));
        assertFalse(ResourceUtils.isValidResourcePath(null));
    }

}
