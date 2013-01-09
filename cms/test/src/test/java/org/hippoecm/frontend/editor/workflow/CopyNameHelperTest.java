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
package org.hippoecm.frontend.editor.workflow;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.hippoecm.repository.api.StringCodecFactory.UriEncoding;
import org.junit.Test;

public class CopyNameHelperTest {
    
    @Test
    public void testFirst() throws RepositoryException {
        Node folder = createMock(Node.class);
        expect(folder.hasNode("bla-kopietje")).andReturn(false);
        replay(folder);

        CopyNameHelper helper = new CopyNameHelper(new UriEncoding(), "kopietje");
        assertEquals("bla (kopietje)", helper.getCopyName("bla", folder));
        verify();
    }

    @Test
    public void testSecond() throws RepositoryException {
        Node folder = createMock(Node.class);
        expect(folder.hasNode("bla-kopietje")).andReturn(true);
        expect(folder.hasNode("bla-kopietje-2")).andReturn(false);
        replay(folder);

        CopyNameHelper helper = new CopyNameHelper(new UriEncoding(), "kopietje");
        String newName = helper.getCopyName("bla", folder);
        verify();
        assertEquals("bla (kopietje 2)", newName); 
    }

    @Test
    public void testThird() throws RepositoryException {
        Node folder = createMock(Node.class);
        expect(folder.hasNode("bla-kopietje")).andReturn(true);
        expect(folder.hasNode("bla-kopietje-2")).andReturn(true);
        expect(folder.hasNode("bla-kopietje-3")).andReturn(false);
        replay(folder);

        CopyNameHelper helper = new CopyNameHelper(new UriEncoding(), "kopietje");
        String newName = helper.getCopyName("bla", folder);
        verify();
        assertEquals("bla (kopietje 3)", newName); 
    }
}
