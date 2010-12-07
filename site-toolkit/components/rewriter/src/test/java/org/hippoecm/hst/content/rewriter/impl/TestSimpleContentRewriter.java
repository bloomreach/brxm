/*
 *  Copyright 2008 Hippo.
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
package org.hippoecm.hst.content.rewriter.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import javax.jcr.Node;

import org.easymock.EasyMock;
import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.content.rewriter.ContentRewriter;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.junit.Before;
import org.junit.Test;

public class TestSimpleContentRewriter {

    private static final String EMPTY_BODY_HTML = 
        "<html>\n" + 
        "<head>\n" + 
        "<title>Hello</title>\n" + 
        "</head>\n" + 
        "<body></body>\n" + 
        "</html>";
    
    private static final String NULL_BODY_HTML = 
        "<html>\n" + 
        "<head>\n" + 
        "<title>Hello</title>\n" + 
        "</head>\n" + 
        "<body/>\n" + 
        "</html>";
    
    private static final String CONTENT_ONLY_HTML = 
        "<div>\n" + 
        "<h1>Hello, World!</h1>\n" + 
        "<p>Test</p>\n" + 
        "</div>";
    
    private Node node;
    private HstRequestContext requestContext;
    private Mount mount;
    
    @Before
    public void setUp() {
        node = EasyMock.createNiceMock(Node.class);
        requestContext = EasyMock.createNiceMock(HstRequestContext.class);
        mount = EasyMock.createNiceMock(Mount.class);
        
        EasyMock.replay(node);
        EasyMock.replay(requestContext);
        EasyMock.replay(mount);
    }
    
    @Test
    public void testEmptyBodyHtml() {
        ContentRewriter<String> rewriter = new SimpleContentRewriter();
        String html = rewriter.rewrite(EMPTY_BODY_HTML, node, requestContext, mount);
        assertEquals("", html);
    }
    
    @Test
    public void testNullBodyHtml() {
        ContentRewriter<String> rewriter = new SimpleContentRewriter();
        String html = rewriter.rewrite(NULL_BODY_HTML, node, requestContext, mount);
        assertNull(html);
    }
    
    @Test
    public void testContentOnlyHtml() {
        ContentRewriter<String> rewriter = new SimpleContentRewriter();
        String html = rewriter.rewrite(CONTENT_ONLY_HTML, node, requestContext, mount);
        assertEquals(CONTENT_ONLY_HTML, html);
    }
}
