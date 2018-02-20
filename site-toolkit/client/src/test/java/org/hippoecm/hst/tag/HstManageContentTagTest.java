/*
 * Copyright 2017-2018 Hippo B.V. (http://www.onehippo.com)
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
import java.util.List;
import java.util.stream.Collectors;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;

import org.hippoecm.hst.configuration.ConfigurationUtils;
import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.container.ModifiableRequestContextProvider;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.core.component.GenericHstComponent;
import org.hippoecm.hst.core.container.ContainerConstants;
import org.hippoecm.hst.core.parameters.JcrPath;
import org.hippoecm.hst.core.parameters.Parameter;
import org.hippoecm.hst.core.parameters.ParametersInfo;
import org.hippoecm.hst.core.request.ResolvedMount;
import org.hippoecm.hst.mock.core.container.MockHstComponentWindow;
import org.hippoecm.hst.mock.core.request.MockComponentConfiguration;
import org.hippoecm.hst.mock.core.request.MockHstRequestContext;
import org.hippoecm.repository.HippoStdNodeType;
import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.HippoNodeType;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onehippo.repository.mock.MockNode;
import org.onehippo.testutils.log4j.Log4jInterceptor;
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
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hippoecm.hst.core.container.ContainerConstants.RENDER_VARIANT;
import static org.junit.Assert.assertThat;

public class HstManageContentTagTest {

    private MockHstRequestContext hstRequestContext;
    private MockPageContext pageContext;
    private MockHttpServletResponse response;
    private MockHstComponentWindow window;
    private HstManageContentTag tag;

    @Before
    public void setUp() {
        hstRequestContext = new MockHstRequestContext();
        hstRequestContext.setCmsRequest(true);
        ModifiableRequestContextProvider.set(hstRequestContext);

        final MockServletContext servletContext = new MockServletContext();
        final MockHttpServletRequest request = new MockHttpServletRequest();

        window = new MockHstComponentWindow();

        final TestComponent component = new TestComponent();
        final MockComponentConfiguration componentConfig = new MockComponentConfiguration();
        componentConfig.setRenderPath("webfile:/freemarker/test.ftl");
        component.init(servletContext, componentConfig);
        window.setComponent(component);
        request.setAttribute(ContainerConstants.HST_COMPONENT_WINDOW, window);

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
        assertThat(tag.doStartTag(), is(EVAL_BODY_INCLUDE));
        assertThat(response.getContentAsString(), is(""));
    }

    @Test
    public void noHstRequestOutputsNothingAndLogsWarning() throws Exception {
        ModifiableRequestContextProvider.set(null);

        try (Log4jInterceptor listener = Log4jInterceptor.onWarn().trap(HstManageContentTag.class).build()) {
            assertThat(tag.doEndTag(), is(EVAL_PAGE));
            assertThat(response.getContentAsString(), is(""));
            assertLogged(listener, "Cannot create a manageContent button outside the hst request.");
        }
    }

    @Test
    public void noCmsRequestOutputsNothingAndLogsDebug() throws Exception {
        hstRequestContext.setCmsRequest(false);

        try (Log4jInterceptor listener = Log4jInterceptor.onDebug().trap(HstManageContentTag.class).build()) {
            assertThat(tag.doEndTag(), is(EVAL_PAGE));
            assertThat(response.getContentAsString(), is(""));
            assertLogged(listener, "Skipping manageContent tag because not in cms preview.");
        }
    }

    @Test
    public void noParametersOutputsNothingAndDoesNotLogWarnings() throws Exception {
        try (Log4jInterceptor listener = Log4jInterceptor.onWarn().trap(HstManageContentTag.class).build()) {
            assertThat(tag.doEndTag(), is(EVAL_PAGE));
            assertThat(response.getContentAsString(), is(""));
            assertThat(listener.getEvents().size(), is(0));
        }
    }

    @Test
    public void templateQuery() throws Exception {
        tag.setTemplateQuery("new-document");

        assertThat(tag.doEndTag(), is(EVAL_PAGE));

        assertThat(response.getContentAsString(), is("<!-- {"
                + "\"HST-Type\":\"MANAGE_CONTENT_LINK\","
                + "\"templateQuery\":\"new-document\""
                + "} -->"));
    }

    @Test
    public void documentFromHandle() throws Exception {
        final HippoBean document = createMock(HippoBean.class);
        tag.setHippobean(document);

        final MockNode root = MockNode.root();
        final MockNode handle = root.addNode("document", HippoNodeType.NT_HANDLE);
        expect(document.getNode()).andReturn(handle);
        replay(document);

        assertThat(tag.doEndTag(), is(EVAL_PAGE));

        assertThat(response.getContentAsString(), is("<!-- {"
                + "\"HST-Type\":\"MANAGE_CONTENT_LINK\","
                + "\"uuid\":\"" + handle.getIdentifier()
                + "\"} -->"));
    }

    @Test
    public void documentFromVariantBelowHandle() throws Exception {
        final HippoBean document = createMock(HippoBean.class);
        tag.setHippobean(document);

        final MockNode root = MockNode.root();
        final MockNode handle = root.addNode("document", HippoNodeType.NT_HANDLE);
        final MockNode variant = handle.addNode("document", "myproject:newsdocument");
        expect(document.getNode()).andReturn(variant);
        replay(document);

        assertThat(tag.doEndTag(), is(EVAL_PAGE));

        assertThat(response.getContentAsString(), is("<!-- {"
                + "\"HST-Type\":\"MANAGE_CONTENT_LINK\","
                + "\"uuid\":\"" + handle.getIdentifier()
                + "\"} -->"));
    }

    @Test
    public void documentWithoutCanonicalNodeOutputsNothing() throws Exception {
        final HippoBean document = createMock(HippoBean.class);
        tag.setHippobean(document);

        final HippoNode handle = createMock(HippoNode.class);
        expect(document.getNode()).andReturn(handle);
        expect(handle.getCanonicalNode()).andReturn(null);
        expect(handle.getPath()).andReturn("/some-document");
        replay(document, handle);

        assertThat(tag.doEndTag(), is(EVAL_PAGE));

        assertThat(response.getContentAsString(), is(""));
    }

    @Test
    public void documentWithoutHandleNodeOutputsNothing() throws Exception {
        final HippoBean document = createMock(HippoBean.class);
        tag.setHippobean(document);

        final MockNode root = MockNode.root();
        expect(document.getNode()).andReturn(root);
        replay(document);

        assertThat(tag.doEndTag(), is(EVAL_PAGE));

        assertThat(response.getContentAsString(), is(""));
    }

    @Test
    public void exceptionWhileReadingUuidOutputsNothing() throws Exception {
        final HippoBean document = createMock(HippoBean.class);
        tag.setHippobean(document);

        final HippoNode brokenNode = createMock(HippoNode.class);
        expect(document.getNode()).andReturn(brokenNode).anyTimes();
        expect(brokenNode.getCanonicalNode()).andThrow(new RepositoryException());
        expect(brokenNode.getPath()).andReturn("/broken");
        replay(document, brokenNode);

        assertThat(tag.doEndTag(), is(EVAL_PAGE));

        assertThat(response.getContentAsString(), is(""));
    }

    @Test
    public void parameterWithAbsoluteJcrPath() throws Exception {
        tag.setTemplateQuery("new-document");
        tag.setParameterName("absPath");

        final ResolvedMount resolvedMount = createMock(ResolvedMount.class);
        final Mount mount = createMock(Mount.class);
        expect(resolvedMount.getMount()).andReturn(mount).anyTimes();
        expect(mount.getContentPath()).andReturn("/my/channel/path").anyTimes();
        hstRequestContext.setResolvedMount(resolvedMount);

        replay(resolvedMount, mount);

        assertThat(tag.doEndTag(), is(EVAL_PAGE));

        assertThat(response.getContentAsString(), is("<!-- {"
                + "\"HST-Type\":\"MANAGE_CONTENT_LINK\","
                + "\"templateQuery\":\"new-document\","
                + "\"parameterName\":\"absPath\","
                + "\"parameterValueIsRelativePath\":\"false\","
                + "\"pickerConfiguration\":\"cms-pickers/documents\","
                + "\"pickerRemembersLastVisited\":\"true\","
                + "\"pickerRootPath\":\"/my/channel/path\""
                + "} -->"));
    }

    @Test
    public void parameterWithRelativeJcrPath() throws Exception {
        tag.setTemplateQuery("new-document");
        tag.setParameterName("relPath");

        final ResolvedMount resolvedMount = createMock(ResolvedMount.class);
        final Mount mount = createMock(Mount.class);
        expect(resolvedMount.getMount()).andReturn(mount).anyTimes();
        expect(mount.getContentPath()).andReturn("/my/channel/path").anyTimes();
        hstRequestContext.setResolvedMount(resolvedMount);

        replay(resolvedMount, mount);

        assertThat(tag.doEndTag(), is(EVAL_PAGE));

        assertThat(response.getContentAsString(), is("<!-- {"
                + "\"HST-Type\":\"MANAGE_CONTENT_LINK\","
                + "\"templateQuery\":\"new-document\","
                + "\"parameterName\":\"relPath\","
                + "\"parameterValueIsRelativePath\":\"true\","
                + "\"pickerConfiguration\":\"cms-pickers/documents\","
                + "\"pickerRemembersLastVisited\":\"true\","
                + "\"pickerRootPath\":\"/my/channel/path\""
                + "} -->"));
    }

    @Test
    public void parameterWithoutJcrPathIsAbsolute() throws Exception {
        tag.setTemplateQuery("new-document");
        tag.setParameterName("string");

        assertThat(tag.doEndTag(), is(EVAL_PAGE));

        assertThat(response.getContentAsString(), is("<!-- {"
                + "\"HST-Type\":\"MANAGE_CONTENT_LINK\","
                + "\"templateQuery\":\"new-document\","
                + "\"parameterName\":\"string\","
                + "\"parameterValueIsRelativePath\":\"false\""
                + "} -->"));
    }

    @Test
    public void componentParameterWithAbsoluteJcrPathAndRelativeRootPath() throws Exception {
        tag.setTemplateQuery("new-document");
        tag.setParameterName("relPath");
        tag.setRootPath("/some/absolute/path");

        try (Log4jInterceptor listener = Log4jInterceptor.onWarn().trap(HstManageContentTag.class).build()) {
            assertThat(tag.doEndTag(), is(EVAL_PAGE));

            assertThat(response.getContentAsString(), is(""));
            assertLogged(listener, "Ignoring manageContent tag in template 'webfile:/freemarker/test.ftl'"
                    + " for component parameter 'relPath': the @JcrPath annotation of the parameter"
                    + " makes it store a relative path to the content root of the channel while the 'rootPath'"
                    + " attribute of the manageContent tag points to the absolute path '/some/absolute/path'."
                    + " Either make the root path relative to the channel content root,"
                    + " or make the component parameter store an absolute path.");
        }
    }

    @Test
    public void parameterWithoutDocumentOrTemplateQuery() throws Exception {
        tag.setParameterName("test");

        tag.doEndTag();

        assertThat(response.getContentAsString(), is("<!-- "
                + "{\"HST-Type\":\"MANAGE_CONTENT_LINK\","
                + "\"parameterName\":\"test\","
                + "\"parameterValueIsRelativePath\":\"false\""
                + "} -->"));
    }

    @Test
    public void parameterValueAbsolutePath() throws Exception {
        window.setParameter("absPath", "/absolute/path");

        tag.setParameterName("absPath");
        final HippoBean document = createMock(HippoBean.class);
        tag.setHippobean(document);

        final MockNode root = MockNode.root();
        final MockNode handle = root.addNode("document", HippoNodeType.NT_HANDLE);
        expect(document.getNode()).andReturn(handle);

        final ResolvedMount resolvedMount = createMock(ResolvedMount.class);
        final Mount mount = createMock(Mount.class);
        expect(resolvedMount.getMount()).andReturn(mount).anyTimes();
        expect(mount.getContentPath()).andReturn("/my/channel/path").anyTimes();
        hstRequestContext.setResolvedMount(resolvedMount);

        replay(document, resolvedMount, mount);

        assertThat(tag.doEndTag(), is(EVAL_PAGE));

        assertThat(response.getContentAsString(), is("<!-- "
                + "{\"HST-Type\":\"MANAGE_CONTENT_LINK\","
                + "\"uuid\":\"" + handle.getIdentifier() + "\","
                + "\"parameterName\":\"absPath\","
                + "\"parameterValueIsRelativePath\":\"false\","
                + "\"parameterValue\":\"/absolute/path\","
                + "\"pickerConfiguration\":\"cms-pickers/documents\","
                + "\"pickerRemembersLastVisited\":\"true\","
                + "\"pickerRootPath\":\"/my/channel/path\""
                + "} -->"));
    }

    @Test
    public void parameterValueRelativePath() throws Exception {
        final ResolvedMount resolvedMount = createMock(ResolvedMount.class);
        final Mount mount = createMock(Mount.class);
        expect(resolvedMount.getMount()).andReturn(mount).anyTimes();
        expect(mount.getContentPath()).andReturn("/mount/path").anyTimes();

        hstRequestContext.setResolvedMount(resolvedMount);
        window.setParameter("relPath", "relative/path");

        tag.setParameterName("relPath");
        final HippoBean document = createMock(HippoBean.class);
        tag.setHippobean(document);

        final MockNode root = MockNode.root();
        final MockNode handle = root.addNode("document", HippoNodeType.NT_HANDLE);
        expect(document.getNode()).andReturn(handle);

        replay(resolvedMount, mount, document);

        assertThat(tag.doEndTag(), is(EVAL_PAGE));

        assertThat(response.getContentAsString(), is("<!-- "
                + "{\"HST-Type\":\"MANAGE_CONTENT_LINK\","
                + "\"uuid\":\"" + handle.getIdentifier() + "\","
                + "\"parameterName\":\"relPath\","
                + "\"parameterValueIsRelativePath\":\"true\","
                + "\"parameterValue\":\"/mount/path/relative/path\","
                + "\"pickerConfiguration\":\"cms-pickers/documents\","
                + "\"pickerRemembersLastVisited\":\"true\","
                + "\"pickerRootPath\":\"/mount/path\""
                + "} -->"));
    }

    @Test
    public void prefixedParameterValue() throws Exception {
        final String prefixedParameterName = ConfigurationUtils.createPrefixedParameterName("prefix", "absPath");
        window.setParameter(prefixedParameterName, "/absolute/path");
        window.setAttribute(RENDER_VARIANT, "prefix");

        tag.setParameterName("absPath");
        final HippoBean document = createMock(HippoBean.class);
        tag.setHippobean(document);

        final MockNode root = MockNode.root();
        final MockNode handle = root.addNode("document", HippoNodeType.NT_HANDLE);
        expect(document.getNode()).andReturn(handle);

        final ResolvedMount resolvedMount = createMock(ResolvedMount.class);
        final Mount mount = createMock(Mount.class);
        expect(resolvedMount.getMount()).andReturn(mount).anyTimes();
        expect(mount.getContentPath()).andReturn("/my/channel/path").anyTimes();
        hstRequestContext.setResolvedMount(resolvedMount);

        replay(document, resolvedMount, mount);

        assertThat(tag.doEndTag(), is(EVAL_PAGE));

        assertThat(response.getContentAsString(), is("<!-- "
                + "{\"HST-Type\":\"MANAGE_CONTENT_LINK\","
                + "\"uuid\":\"" + handle.getIdentifier() + "\","
                + "\"parameterName\":\"absPath\","
                + "\"parameterValueIsRelativePath\":\"false\","
                + "\"parameterValue\":\"/absolute/path\","
                + "\"pickerConfiguration\":\"cms-pickers/documents\","
                + "\"pickerRemembersLastVisited\":\"true\","
                + "\"pickerRootPath\":\"/my/channel/path\""
                + "} -->"));
    }

    @Test
    public void emptyPrefixParameterValue() throws Exception {
        window.setParameter("absPath", "/absolute/path");
        window.setAttribute(RENDER_VARIANT, "");

        tag.setParameterName("absPath");
        final HippoBean document = createMock(HippoBean.class);
        tag.setHippobean(document);

        final MockNode root = MockNode.root();
        final MockNode handle = root.addNode("document", HippoNodeType.NT_HANDLE);
        expect(document.getNode()).andReturn(handle);

        final ResolvedMount resolvedMount = createMock(ResolvedMount.class);
        final Mount mount = createMock(Mount.class);
        expect(resolvedMount.getMount()).andReturn(mount).anyTimes();
        expect(mount.getContentPath()).andReturn("/my/channel/path").anyTimes();
        hstRequestContext.setResolvedMount(resolvedMount);

        replay(document, resolvedMount, mount);

        assertThat(tag.doEndTag(), is(EVAL_PAGE));

        assertThat(response.getContentAsString(), is("<!-- "
                + "{\"HST-Type\":\"MANAGE_CONTENT_LINK\","
                + "\"uuid\":\"" + handle.getIdentifier() + "\","
                + "\"parameterName\":\"absPath\","
                + "\"parameterValueIsRelativePath\":\"false\","
                + "\"parameterValue\":\"/absolute/path\","
                + "\"pickerConfiguration\":\"cms-pickers/documents\","
                + "\"pickerRemembersLastVisited\":\"true\","
                + "\"pickerRootPath\":\"/my/channel/path\""
                + "} -->"));
    }

    @Test
    public void pickerConfiguration() throws Exception {
        tag.setParameterName("pickerPath");

        final ResolvedMount resolvedMount = createMock(ResolvedMount.class);
        final Mount mount = createMock(Mount.class);
        expect(resolvedMount.getMount()).andReturn(mount).anyTimes();
        expect(mount.getContentPath()).andReturn("/my/channel/path").anyTimes();
        hstRequestContext.setResolvedMount(resolvedMount);

        replay(resolvedMount, mount);

        assertThat(tag.doEndTag(), is(EVAL_PAGE));

        assertThat(response.getContentAsString(), is("<!-- {"
                + "\"HST-Type\":\"MANAGE_CONTENT_LINK\","
                + "\"parameterName\":\"pickerPath\","
                + "\"parameterValueIsRelativePath\":\"true\","
                + "\"pickerConfiguration\":\"picker-config\","
                + "\"pickerInitialPath\":\"initial-path\","
                + "\"pickerRemembersLastVisited\":\"false\","
                + "\"pickerRootPath\":\"/root-path\","
                + "\"pickerSelectableNodeTypes\":\"node-type-1,node-type-2\""
                + "} -->"));
    }

    @Test
    public void allParameters() throws Exception {
        tag.setTemplateQuery("new-newsdocument");
        tag.setRootPath("news/amsterdam");
        tag.setDefaultPath("2018/09/23");
        tag.setParameterName("newsDocument");

        final HippoBean document = createMock(HippoBean.class);
        tag.setHippobean(document);

        final MockNode root = MockNode.root();
        final MockNode handle = root.addNode("document", HippoNodeType.NT_HANDLE);
        expect(document.getNode()).andReturn(handle);

        Session jcrSession = createMock(Session.class);
        hstRequestContext.setSession(jcrSession);
        hstRequestContext.setSiteContentBasePath("my/channel/path");
        Node folderNode = createMock(Node.class);
        expect(folderNode.isNodeType(HippoStdNodeType.NT_FOLDER)).andReturn(false);
        expect(folderNode.isNodeType(HippoStdNodeType.NT_DIRECTORY)).andReturn(true);
        expect(jcrSession.getNode("/my/channel/path/news/amsterdam")).andReturn(folderNode);

        replay(document, jcrSession, folderNode);

        assertThat(tag.doEndTag(), is(EVAL_PAGE));

        assertThat(response.getContentAsString(), is("<!-- {"
                + "\"HST-Type\":\"MANAGE_CONTENT_LINK\","
                + "\"uuid\":\"" + handle.getIdentifier() + "\","
                + "\"templateQuery\":\"new-newsdocument\","
                + "\"rootPath\":\"news/amsterdam\","
                + "\"defaultPath\":\"2018/09/23\","
                + "\"parameterName\":\"newsDocument\","
                + "\"parameterValueIsRelativePath\":\"false\""
                + "} -->"));
    }

    @Test(expected = JspException.class)
    public void exceptionWhileWritingToJspOutputsNothing() throws Exception {
        tag.setTemplateQuery("new-document");
        tag.setPageContext(new BrokenPageContext());
        assertThat(tag.doEndTag(), is(EVAL_PAGE));
    }

    @Test
    public void setComponentParameterToNull() throws Exception {
        try (Log4jInterceptor listener = Log4jInterceptor.onWarn().trap(HstManageContentTag.class).build()) {
            tag.setParameterName(null);
            tag.doEndTag();

            assertLogged(listener, "The parameterName attribute of a manageContent tag"
                    + " in template 'webfile:/freemarker/test.ftl' is set to 'null'."
                    + " Expected the name of an HST component parameter instead.");
        }
    }

    @Test
    public void setComponentParameterToEmpty() throws Exception {
        try (Log4jInterceptor listener = Log4jInterceptor.onWarn().trap(HstManageContentTag.class).build()) {
            tag.setParameterName("");
            tag.doEndTag();

            assertLogged(listener, "The parameterName attribute of a manageContent tag"
                    + " in template 'webfile:/freemarker/test.ftl' is set to ''."
                    + " Expected the name of an HST component parameter instead.");
        }
    }

    @Test
    public void setComponentParameterToSpaces() throws Exception {
        try (Log4jInterceptor listener = Log4jInterceptor.onWarn().trap(HstManageContentTag.class).build()) {
            tag.setParameterName("  ");
            tag.doEndTag();

            assertLogged(listener, "The parameterName attribute of a manageContent tag in template"
                    + " 'webfile:/freemarker/test.ftl' is set to '  '."
                    + " Expected the name of an HST component parameter instead.");
        }
    }

    @Test
    public void setTemplateQueryToNull() throws Exception {
        try (Log4jInterceptor listener = Log4jInterceptor.onWarn().trap(HstManageContentTag.class).build()) {
            tag.setTemplateQuery(null);
            tag.doEndTag();

            assertLogged(listener, "The templateQuery attribute of a manageContent tag"
                    + " in template 'webfile:/freemarker/test.ftl' is set to 'null'." +
                    " Expected the name of a template query instead.");
        }
    }

    @Test
    public void setTemplateQueryToEmpty() throws Exception {
        try (Log4jInterceptor listener = Log4jInterceptor.onWarn().trap(HstManageContentTag.class).build()) {
            tag.setTemplateQuery("");
            tag.doEndTag();

            assertLogged(listener, "The templateQuery attribute of a manageContent tag in template"
                    + " 'webfile:/freemarker/test.ftl' is set to ''. Expected the name of a template query instead.");
        }
    }

    @Test
    public void setTemplateQueryToSpaces() throws Exception {
        try (Log4jInterceptor listener = Log4jInterceptor.onWarn().trap(HstManageContentTag.class).build()) {
            tag.setTemplateQuery("  ");
            tag.doEndTag();

            assertLogged(listener, "The templateQuery attribute of a manageContent tag in template"
                    + " 'webfile:/freemarker/test.ftl' is set to '  '. Expected the name of a template query instead.");
        }
    }

    @Test
    public void rootPathNodeNotFound() throws Exception {
        tag.setRootPath("/exists/not");
        tag.setDefaultPath("2018/09/23");
        tag.setTemplateQuery("new-newsdocument");

        Session jcrSession = createMock(Session.class);
        hstRequestContext.setSession(jcrSession);

        expect(jcrSession.getNode("/exists/not")).andThrow(new PathNotFoundException());
        replay(jcrSession);

        assertThat(tag.doEndTag(), is(EVAL_PAGE));

        assertThat(response.getContentAsString(), is("<!-- {"
                + "\"HST-Type\":\"MANAGE_CONTENT_LINK\","
                + "\"templateQuery\":\"new-newsdocument\""
                + "} -->"));
    }

    @Test
    public void rootPathNodeNotAFolder() throws Exception {
        tag.setRootPath("/not/a/folder");
        tag.setDefaultPath("2018/09/23");
        tag.setTemplateQuery("new-newsdocument");

        Session jcrSession = createMock(Session.class);
        hstRequestContext.setSession(jcrSession);

        Node notAFolderNode = createMock(Node.class);
        expect(notAFolderNode.isNodeType(HippoStdNodeType.NT_FOLDER)).andReturn(false);
        expect(notAFolderNode.isNodeType(HippoStdNodeType.NT_DIRECTORY)).andReturn(false);
        expect(jcrSession.getNode("/not/a/folder")).andReturn(notAFolderNode);

        replay(jcrSession, notAFolderNode);

        assertThat(tag.doEndTag(), is(EVAL_PAGE));

        assertThat(response.getContentAsString(), is("<!-- {"
                + "\"HST-Type\":\"MANAGE_CONTENT_LINK\","
                + "\"templateQuery\":\"new-newsdocument\""
                + "} -->"));
    }

    @Test
    public void pickerRootPathTest() throws Exception {
        tag.setParameterName("absPath");
        tag.setRootPath("relative/path");

        Session jcrSession = createMock(Session.class);
        hstRequestContext.setSession(jcrSession);
        hstRequestContext.setSiteContentBasePath("my/channel/path");
        Node folderNode = createMock(Node.class);
        expect(folderNode.isNodeType(HippoStdNodeType.NT_FOLDER)).andReturn(false);
        expect(folderNode.isNodeType(HippoStdNodeType.NT_DIRECTORY)).andReturn(true);
        expect(jcrSession.getNode("/my/channel/path/relative/path")).andReturn(folderNode);

        final ResolvedMount resolvedMount = createMock(ResolvedMount.class);
        final Mount mount = createMock(Mount.class);
        expect(resolvedMount.getMount()).andReturn(mount).anyTimes();
        expect(mount.getContentPath()).andReturn("/my/channel/path").anyTimes();
        hstRequestContext.setResolvedMount(resolvedMount);

        replay(jcrSession, folderNode, resolvedMount, mount);
        assertThat(tag.doEndTag(), is(EVAL_PAGE));

        assertThat(response.getContentAsString(), is("<!-- {"
                + "\"HST-Type\":\"MANAGE_CONTENT_LINK\","
                + "\"rootPath\":\"relative/path\","
                + "\"parameterName\":\"absPath\","
                + "\"parameterValueIsRelativePath\":\"false\","
                + "\"pickerConfiguration\":\"cms-pickers/documents\","
                + "\"pickerRemembersLastVisited\":\"true\","
                + "\"pickerRootPath\":\"/my/channel/path/relative/path\""
                + "} -->"));
    }

    @Test
    public void supplyChannelContentRootAsDefaultPickerRootPath() throws Exception {
        tag.setParameterName("docPathParameter");
        window.setComponent(new TestComponentWithoutPickerRootPath());

        final ResolvedMount resolvedMount = createMock(ResolvedMount.class);
        final Mount mount = createMock(Mount.class);
        expect(resolvedMount.getMount()).andReturn(mount).anyTimes();
        expect(mount.getContentPath()).andReturn("/my/channel/path").anyTimes();
        hstRequestContext.setResolvedMount(resolvedMount);

        replay(resolvedMount, mount);

        assertThat(tag.doEndTag(), is(EVAL_PAGE));

        assertThat(response.getContentAsString(), is("<!-- {"
                + "\"HST-Type\":\"MANAGE_CONTENT_LINK\","
                + "\"parameterName\":\"docPathParameter\","
                + "\"parameterValueIsRelativePath\":\"false\","
                + "\"pickerConfiguration\":\"cms-pickers/documents\","
                + "\"pickerRemembersLastVisited\":\"true\","
                + "\"pickerRootPath\":\"/my/channel/path\""
                + "} -->"));
    }

    private static void assertLogged(final Log4jInterceptor listener, final String expectedMessage) {
        final List<String> messages = listener.messages().collect(Collectors.toList());
        assertThat(messages.size(), is(1));
        assertThat(messages.get(0), equalTo(expectedMessage));
    }

    private static class BrokenPageContext extends MockPageContext {

        @Override
        public JspWriter getOut() {
            return new MockJspWriter((Writer) null) {
                @Override
                public void print(final String value) throws IOException {
                    throw new IOException();
                }
            };
        }
    }

    @ParametersInfo(type = TestComponentInfo.class)
    public class TestComponent extends GenericHstComponent {
    }

    private interface TestComponentInfo {

        @Parameter(name = "absPath")
        @JcrPath
        String getAbsPath();

        @Parameter(name = "relPath")
        @JcrPath(isRelative = true)
        String getRelPath();

        @Parameter(name = "pickerPath")
        @JcrPath(
                isRelative = true,
                pickerInitialPath = "initial-path",
                pickerRootPath = "/root-path",
                pickerConfiguration = "picker-config",
                pickerRemembersLastVisited = false,
                pickerSelectableNodeTypes = {"node-type-1", "node-type-2"}
        )
        String getPickerPath();

        @Parameter(name = "string")
        String getString();
    }

    @ParametersInfo(type = TestComponentInfoWithoutPickerRootPath.class)
    public class TestComponentWithoutPickerRootPath extends GenericHstComponent {
    }

    private interface TestComponentInfoWithoutPickerRootPath {

        @Parameter(name = "docPathParameter")
        @JcrPath
        String getAbsPath();

        @Parameter(name = "relPath")
        @JcrPath(isRelative = true)
        String getRelPath();

        @Parameter(name = "pickerPath")
        @JcrPath(
                isRelative = true,
                pickerInitialPath = "initial-path",
                pickerConfiguration = "picker-config",
                pickerRemembersLastVisited = false,
                pickerSelectableNodeTypes = {"node-type-1", "node-type-2"}
        )
        String getPickerPath();

        @Parameter(name = "string")
        String getString();
    }

}
