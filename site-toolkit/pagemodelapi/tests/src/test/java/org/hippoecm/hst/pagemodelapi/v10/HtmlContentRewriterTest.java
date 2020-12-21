/*
 *  Copyright 2020 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.pagemodelapi.v10;

import javax.jcr.Node;

import org.easymock.EasyMock;
import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.configuration.sitemap.HstSiteMapItem;
import org.hippoecm.hst.content.rewriter.ContentRewriter;
import org.hippoecm.hst.content.rewriter.HtmlCleanerFactoryBean;
import org.hippoecm.hst.content.rewriter.impl.SimpleContentRewriter;
import org.hippoecm.hst.core.linking.HstLink;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.core.request.ResolvedMount;
import org.hippoecm.hst.core.request.ResolvedSiteMapItem;
import org.hippoecm.hst.mock.core.linking.MockHstLink;
import org.hippoecm.hst.pagemodelapi.v10.content.rewriter.HtmlContentRewriter;
import org.htmlcleaner.HtmlCleaner;
import org.junit.Before;
import org.junit.Test;

import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class HtmlContentRewriterTest {

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
    
    private static final String CONTENT_WITH_LINKS = 
        "<div>\n" + 
        "<h1>Hello, World!</h1>\n" + 
        "<p>Test</p>\n" + 
        "<a href=\"/foo/bar\">Foo1</a>\n" +
        "<a href=\"/foo/bar?a=b\">Foo2</a>\n" +
        "<a href=\"http://www.onehippo.org/external/foo/bar?a=b\">Foo2</a>\n" +
        "</div>";

    private static final String CONTENT_WITH_NON_INTERNAL_IMAGES = 
            "<div>\n" + 
            "<h1>Hello, World!</h1>\n" + 
            "<p>Test</p>\n" + 
            "<img src=\"http://upload.wikimedia.org/wikipedia/commons/3/31/Red-dot-5px.png\" alt=\"Red dot\"/>\n" +
            "<img src=\"data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAUAAAAFCAYAAACNbyblAAAAHElEQVQI12P4//8/w38GIAXDIBKE0DHxgljNBAAO9TXL0Y4OHwAAAABJRU5ErkJggg==\" alt=\"Red dot\">\n" +
            "</div>";


    private static final String CONTENT_WITH_EXTERNAL_PROTOCOLS =
            "<html>\n" +
            "<head>\n" +
            "<title>Hello</title>\n" +
            "</head>\n" +
            "<body>" +
            "<a href=\"sip:mail@provider.com\">sip-protocol</a>" +
            "</body>\n" +
            "</html>";

    private static final String CONTENT_WITH_ANCHOR_LINK =
            "<html>\n" +
                    "<head>\n" +
                    "<title>Hello</title>\n" +
                    "</head>\n" +
                    "<body>" +
                    "<a href=\"home#foo\">anchor</a>" +
                    "</body>\n" +
                    "</html>";

    private Node node;
    private HstRequestContext requestContext;
    private Mount mount;
    private Mount parentMount;
    private ContentRewriter<String> rewriter;
    
    @Before
    public void setUp() throws Exception {
        node = createNiceMock(Node.class);
        requestContext = createNiceMock(HstRequestContext.class);
        mount = createNiceMock(Mount.class);
        parentMount = createNiceMock(Mount.class);
        expect(mount.getParent()).andStubReturn(parentMount);
        ResolvedMount resolvedMount = createNiceMock(ResolvedMount.class);
        expect(resolvedMount.getMount()).andStubReturn(mount);

        HstSiteMapItem siteMapItem =  createNiceMock(HstSiteMapItem.class);
        expect(siteMapItem.getApplicationId()).andStubReturn("spa1");
        ResolvedSiteMapItem resolvedSiteMapItem =  createNiceMock(ResolvedSiteMapItem.class);
        expect(resolvedSiteMapItem.getHstSiteMapItem()).andStubReturn(siteMapItem);

        expect(requestContext.getResolvedSiteMapItem()).andStubReturn(resolvedSiteMapItem);
        expect(requestContext.getResolvedMount()).andStubReturn(resolvedMount);
        replay(node, requestContext, mount, resolvedMount, siteMapItem, resolvedSiteMapItem);

        rewriter = new HtmlContentRewriter(new ExtHtmlCleanerFactoryBean().createInstance()) {
            @Override
            protected HstLink getDocumentLink(final String path, final Node hippoHtmlNode, final HstRequestContext requestContext, final Mount targetMount) {
                return new MockHstLink() {
                    @Override
                    public String getPath() {
                        if (path.contains("#")) {
                            throw new IllegalArgumentException("Expected path to not have # in it for HstLink");
                        }
                        return path;
                    }

                    @Override
                    public Mount getMount() {
                        return parentMount;
                    }

                    @Override
                    public HstSiteMapItem getHstSiteMapItem() {
                        return siteMapItem;
                    }

                    // The PMA won't use toUrlForm since we do not return 'mount path' or 'context path' in the URLs
                    // but just the path, see the assertion below
                    @Override
                    public String toUrlForm(final HstRequestContext requestContext, final boolean fullyQualified) {
                        return "/site" + "/" + path;
                    }
                };
            }
        };
    }

    class ExtHtmlCleanerFactoryBean extends HtmlCleanerFactoryBean {
        @Override
        protected HtmlCleaner createInstance() throws Exception {
            return super.createInstance();
        }
    }

    @Test
    public void testEmptyBodyHtml() {
        String html = rewriter.rewrite(EMPTY_BODY_HTML, node, requestContext, mount);
        assertEquals("\n\n", html);
    }
    
    @Test
    public void testNullBodyHtml() {
        String html = rewriter.rewrite(NULL_BODY_HTML, node, requestContext, mount);
        assertEquals(html, "\n\n");
    }
    
    @Test
    public void testContentOnlyHtml() {
        String html = rewriter.rewrite(CONTENT_ONLY_HTML, node, requestContext, mount);
        assertEquals(CONTENT_ONLY_HTML, html);
    }
    
    @Test
    public void testContentWithLinks() throws Exception {

        String html = rewriter.rewrite(CONTENT_WITH_LINKS, node, requestContext, mount);
        assertTrue(html.contains("/foo/bar?a=b"));
        assertTrue(html.contains("http://www.onehippo.org/external/foo/bar?a=b"));
    }

    @Test
    public void testContentWithNonInternalImages() {
        ContentRewriter<String> rewriter = new SimpleContentRewriter();
        String html = rewriter.rewrite(CONTENT_WITH_NON_INTERNAL_IMAGES, node, requestContext, mount);
        assertEquals(CONTENT_WITH_NON_INTERNAL_IMAGES, html);
    }

    @Test
    public void testContentWithExternalProtocols() {
        ContentRewriter<String> rewriter = new SimpleContentRewriter();
        String html = rewriter.rewrite(CONTENT_WITH_EXTERNAL_PROTOCOLS, node, requestContext, mount);
        assertTrue(html.contains("href=\"sip:mail@provider.com\""));
    }

    @Test
    public void testContentWithAnchorLinks() throws Exception {

        String html = rewriter.rewrite(CONTENT_WITH_ANCHOR_LINK, node, requestContext, mount);
        // The PMA won't use toUrlForm since we do not return 'mount path' or 'context path' in the URLs
        // but just the path
        assertTrue(html.contains("<a href=\"/home#foo\" data-type=\"internal\">anchor</a>"));
    }

}
