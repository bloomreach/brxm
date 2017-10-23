/*
 * Copyright 2017 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.tag;

import java.io.IOException;
import java.io.Writer;

import javax.jcr.RepositoryException;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;

import org.hippoecm.hst.container.ModifiableRequestContextProvider;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.mock.core.request.MockHstRequestContext;
import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.HippoNodeType;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onehippo.repository.mock.MockNode;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockJspWriter;
import org.springframework.mock.web.MockPageContext;
import org.springframework.mock.web.MockServletContext;

import static javax.servlet.jsp.tagext.Tag.EVAL_BODY_INCLUDE;
import static javax.servlet.jsp.tagext.Tag.EVAL_PAGE;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;

public class HstManageContentTagTest {

    private MockHstRequestContext hstRequestContext;
    private MockPageContext pageContext;
    private MockHttpServletResponse response;
    private HstManageContentTag tag;

    @Before
    public void setUp() {
        hstRequestContext = new MockHstRequestContext();
        hstRequestContext.setCmsRequest(true);
        ModifiableRequestContextProvider.set(hstRequestContext);

        MockServletContext servletContext = new MockServletContext();
        MockHttpServletRequest request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        pageContext = new MockPageContext(servletContext, request, response);

        tag = new HstManageContentTag();
        tag.setPageContext(pageContext);
    }

    @After
    public void tearDown() {
        ModifiableRequestContextProvider.clear();
    }

    @Test
    public void startTagDoesNothing() throws Exception {
        assertEquals(EVAL_BODY_INCLUDE, tag.doStartTag());
        assertEquals("", response.getContentAsString());
    }

    @Test
    public void noHstRequestOutputsNothing() throws Exception {
        ModifiableRequestContextProvider.set(null);
        assertEquals(EVAL_PAGE, tag.doEndTag());
        assertEquals("", response.getContentAsString());
    }

    @Test
    public void noCmsRequestOutputsNothing() throws Exception {
        hstRequestContext.setCmsRequest(false);
        assertEquals(EVAL_PAGE, tag.doEndTag());
        assertEquals("", response.getContentAsString());
    }

    @Test
    public void templateQuery() throws Exception {
        tag.setTemplateQuery("new-document");

        assertEquals(EVAL_PAGE, tag.doEndTag());

        assertEquals("<!-- {\"HST-Type\":\"MANAGE_CONTENT_LINK\",\"templateQuery\":\"new-document\"} -->",
                response.getContentAsString());
    }

    @Test
    public void documentFromHandle() throws Exception {
        final HippoBean document = createMock(HippoBean.class);
        tag.setDocument(document);

        final MockNode root = MockNode.root();
        final MockNode handle = root.addNode("document", HippoNodeType.NT_HANDLE);
        expect(document.getNode()).andReturn(handle);
        replay(document);

        assertEquals(EVAL_PAGE, tag.doEndTag());

        assertEquals("<!-- {\"HST-Type\":\"MANAGE_CONTENT_LINK\",\"uuid\":\"" + handle.getIdentifier() + "\"} -->",
                response.getContentAsString());
    }

    @Test
    public void documentFromVariantBelowHandle() throws Exception {
        final HippoBean document = createMock(HippoBean.class);
        tag.setDocument(document);

        final MockNode root = MockNode.root();
        final MockNode handle = root.addNode("document", HippoNodeType.NT_HANDLE);
        final MockNode variant = handle.addNode("document", "myproject:newsdocument");
        expect(document.getNode()).andReturn(variant);
        replay(document);

        assertEquals(EVAL_PAGE, tag.doEndTag());

        assertEquals("<!-- {\"HST-Type\":\"MANAGE_CONTENT_LINK\",\"uuid\":\"" + handle.getIdentifier() + "\"} -->",
                response.getContentAsString());
    }

    @Test
    public void documentWithoutCanonicalNodeOutputsNothing() throws Exception {
        final HippoBean document = createMock(HippoBean.class);
        tag.setDocument(document);

        final HippoNode handle = createMock(HippoNode.class);
        expect(document.getNode()).andReturn(handle);
        expect(handle.getCanonicalNode()).andReturn(null);
        expect(handle.getPath()).andReturn("/some-document");
        replay(document, handle);

        assertEquals(EVAL_PAGE, tag.doEndTag());

        assertEquals("", response.getContentAsString());
    }

    @Test
    public void documentWithoutHandleNodeOutputsNothing() throws Exception {
        final HippoBean document = createMock(HippoBean.class);
        tag.setDocument(document);

        final MockNode root = MockNode.root();
        expect(document.getNode()).andReturn(root);
        replay(document);

        assertEquals(EVAL_PAGE, tag.doEndTag());

        assertEquals("", response.getContentAsString());
    }

    @Test
    public void exceptionWhileReadingUuidOutputsNothing() throws Exception {
        final HippoBean document = createMock(HippoBean.class);
        tag.setDocument(document);

        final HippoNode brokenNode = createMock(HippoNode.class);
        expect(document.getNode()).andReturn(brokenNode).anyTimes();
        expect(brokenNode.getCanonicalNode()).andThrow(new RepositoryException());
        expect(brokenNode.getPath()).andReturn("/broken");
        replay(document, brokenNode);

        assertEquals(EVAL_PAGE, tag.doEndTag());

        assertEquals("", response.getContentAsString());
    }

    @Test
    public void allParameters() throws Exception {
        tag.setTemplateQuery("new-newsdocument");
        tag.setRootPath("news/amsterdam");
        tag.setDefaultPath("2018/09/23");
        tag.setComponentParameter("newsDocument");

        final HippoBean document = createMock(HippoBean.class);
        tag.setDocument(document);

        final MockNode root = MockNode.root();
        final MockNode handle = root.addNode("document", HippoNodeType.NT_HANDLE);
        expect(document.getNode()).andReturn(handle);
        replay(document);

        assertEquals(EVAL_PAGE, tag.doEndTag());

        assertEquals("<!-- {"
                        + "\"HST-Type\":\"MANAGE_CONTENT_LINK\","
                        + "\"uuid\":\"" + handle.getIdentifier() + "\","
                        + "\"templateQuery\":\"new-newsdocument\","
                        + "\"rootPath\":\"news/amsterdam\","
                        + "\"defaultPath\":\"2018/09/23\","
                        + "\"componentParameter\":\"newsDocument\""
                        + "} -->",
                    response.getContentAsString());
    }

    @Test(expected = JspException.class)
    public void exceptionWhileWritingToJspOutputsNothing() throws Exception {
        tag.setPageContext(new BrokenPageContext());
        assertEquals(EVAL_PAGE, tag.doEndTag());
    }

    private static class BrokenPageContext extends MockPageContext {

        @Override
        public JspWriter getOut() {
            return new MockJspWriter((Writer)null) {
                @Override
                public void print(final String value) throws IOException {
                    throw new IOException();
                }
            };
        }
    }
}