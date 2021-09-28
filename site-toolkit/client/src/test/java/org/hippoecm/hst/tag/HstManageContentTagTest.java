/*
 * Copyright 2017-2021 Hippo B.V. (http://www.onehippo.com)
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
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.List;
import java.util.stream.Collectors;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.security.AccessControlManager;
import javax.jcr.security.Privilege;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;

import org.easymock.EasyMock;
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
import org.hippoecm.hst.platform.services.channel.ChannelManagerPrivileges;
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
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hippoecm.hst.core.container.ContainerConstants.CMS_USER_SESSION_ATTR_NAME;
import static org.hippoecm.hst.core.container.ContainerConstants.RENDER_VARIANT;
import static org.hippoecm.hst.platform.services.channel.ChannelManagerPrivileges.DOCUMENT_EDIT_REQUIRED_PRIVILEGE_NAME;
import static org.junit.Assert.assertThat;

public class HstManageContentTagTest {

    private MockHstRequestContext hstRequestContext;
    private MockHttpServletResponse response;
    private MockHstComponentWindow window;
    private HstManageContentTag tag;

    private void assertManageContentResponse(final String... keysAndValues) throws UnsupportedEncodingException, JspException {
        final StringBuilder expected = new StringBuilder();

        expected.append("<!-- {");
        expected.append("\"HST-Type\":\"MANAGE_CONTENT_LINK\"");
        if (keysAndValues.length > 0) {
            expected.append(",");
        }
        for (int i = 0; i < keysAndValues.length; i++) {
            expected.append(String.format("\"%s\"", keysAndValues[i]));
            if (i % 2 == 0) {
                expected.append(":");
            } else if (i < keysAndValues.length - 1) {
                expected.append(",");
            }
        }
        expected.append("} -->");

        assertThat(tag.doEndTag(), is(EVAL_PAGE));
        assertThat(response.getContentAsString(), is(expected.toString()));
    }

    private void assertEmptyResponse() throws Exception {
        assertThat(tag.doEndTag(), is(EVAL_PAGE));
        assertThat(response.getContentAsString(), is(""));
    }

    @FunctionalInterface
    private interface Action {
        void execute() throws Exception;
    }

    private Action beforeEndTag(final Action action) {
        return () -> {
            action.execute();
            assertThat(tag.doEndTag(), is(EVAL_PAGE));
        };
    }

    private void assertLogIntercepted(final Log4jInterceptor.Builder builder, final String message, final Action action) throws Exception {
        try (final Log4jInterceptor listener = builder.trap(HstManageContentTag.class).build()) {
            if (action != null) {
                action.execute();
            }
            assertLogged(listener, message);
        }
    }

    private void assertWarned(final String message, final Action action) throws Exception {
        assertLogIntercepted(Log4jInterceptor.onWarn(), message, action);
    }

    private void assertDebugged(final String message, final Action action) throws Exception {
        assertLogIntercepted(Log4jInterceptor.onDebug(), message, action);
    }

    @Before
    public void setUp() {
        hstRequestContext = new MockHstRequestContext();
        hstRequestContext.setChannelManagerPreviewRequest();
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

        tag = new HstManageContentTag();
        tag.setPageContext(new MockPageContext(servletContext, request, response));
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

        assertWarned("Cannot create a manageContent button outside the hst request.", this::assertEmptyResponse);
    }

    @Test
    public void noChannelManagerPreviewRequestOutputsNothingAndLogsDebug() throws Exception {
        hstRequestContext = new MockHstRequestContext();
        ModifiableRequestContextProvider.set(hstRequestContext);

        assertDebugged("Skipping manageContent tag because not in cms preview.", this::assertEmptyResponse);
    }

    @Test
    public void noParametersOutputsNothingAndDoesNotLogWarnings() throws Exception {
        try (final Log4jInterceptor listener = Log4jInterceptor.onWarn().trap(HstManageContentTag.class).build()) {
            assertEmptyResponse();
            assertThat(listener.getEvents().size(), is(0));
        }
    }

    @Test
    public void documentTemplateQuery() throws Exception {
        tag.setDocumentTemplateQuery("new-document");

        assertManageContentResponse("documentTemplateQuery", "new-document");
    }

    @Test
    public void folderTemplateQuery() throws Exception {
        tag.setDocumentTemplateQuery("new-document"); // a mandatory parameter
        tag.setFolderTemplateQuery("new-folder");

        assertManageContentResponse(
            "documentTemplateQuery", "new-document",
            "folderTemplateQuery", "new-folder"
        );
    }

    @Test
    public void documentFromHandle() throws Exception {
        final HippoBean document = createMock(HippoBean.class);
        tag.setHippobean(document);

        final AccessControlManager acm = createMock(AccessControlManager.class);
        final Privilege docEditPrivilege = createMock(Privilege.class);

        final MockNode root = MockNode.root(null, acm);
        hstRequestContext.setAttribute(CMS_USER_SESSION_ATTR_NAME, root.getSession());
        final MockNode handle = root.addNode("document", HippoNodeType.NT_HANDLE);

        expect(docEditPrivilege.getName()).andStubReturn(DOCUMENT_EDIT_REQUIRED_PRIVILEGE_NAME);
        expect(acm.getPrivileges(eq(handle.getPath()))).andStubReturn(new Privilege[]{docEditPrivilege});
        expect(document.getNode()).andReturn(handle);
        replay(document, acm, docEditPrivilege);

        assertManageContentResponse("uuid", handle.getIdentifier());
    }

    @Test
    public void documentFromVariantBelowHandle() throws Exception {
        final HippoBean document = createMock(HippoBean.class);
        tag.setHippobean(document);

        final AccessControlManager acm = createMock(AccessControlManager.class);
        final Privilege docEditPrivilege = createMock(Privilege.class);


        final MockNode root = MockNode.root(null, acm);
        hstRequestContext.setAttribute(CMS_USER_SESSION_ATTR_NAME, root.getSession());
        final MockNode handle = root.addNode("document", HippoNodeType.NT_HANDLE);

        expect(docEditPrivilege.getName()).andStubReturn(DOCUMENT_EDIT_REQUIRED_PRIVILEGE_NAME);
        expect(acm.getPrivileges(eq(handle.getPath()))).andStubReturn(new Privilege[]{docEditPrivilege});

        final MockNode variant = handle.addNode("document", "myproject:newsdocument");
        expect(document.getNode()).andReturn(variant);
        replay(document, acm, docEditPrivilege);


        assertManageContentResponse("uuid", handle.getIdentifier());
    }

    @Test
    public void documentFromVariantBelowHandle_not_in_role_does_not_output_content_edit_UUID() throws Exception {
        final HippoBean document = createMock(HippoBean.class);
        tag.setHippobean(document);

        final AccessControlManager acm = createMock(AccessControlManager.class);
        final Privilege docEditPrivilege = createMock(Privilege.class);

        final MockNode root = MockNode.root(null, acm);
        hstRequestContext.setAttribute(CMS_USER_SESSION_ATTR_NAME, root.getSession());
        final MockNode handle = root.addNode("document", HippoNodeType.NT_HANDLE);

        expect(docEditPrivilege.getName()).andStubReturn("SomeRole");
        expect(acm.getPrivileges(eq(handle.getPath()))).andStubReturn(new Privilege[]{docEditPrivilege});

        final MockNode variant = handle.addNode("document", "myproject:newsdocument");
        expect(document.getNode()).andReturn(variant);
        replay(document, acm, docEditPrivilege);


        assertManageContentResponse();
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

        assertEmptyResponse();
    }

    @Test
    public void documentWithoutHandleNodeOutputsNothing() throws Exception {
        final HippoBean document = createMock(HippoBean.class);
        tag.setHippobean(document);

        final MockNode root = MockNode.root();
        expect(document.getNode()).andReturn(root);
        replay(document);

        assertEmptyResponse();
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

        assertEmptyResponse();
    }

    @Test
    public void parameterWithAbsoluteJcrPath() throws Exception {
        tag.setDocumentTemplateQuery("new-document");
        tag.setParameterName("absPath");

        final ResolvedMount resolvedMount = createMock(ResolvedMount.class);
        final Mount mount = createMock(Mount.class);
        expect(resolvedMount.getMount()).andReturn(mount).anyTimes();
        expect(mount.getContentPath()).andReturn("/my/channel/path").anyTimes();
        hstRequestContext.setResolvedMount(resolvedMount);

        replay(resolvedMount, mount);

        assertManageContentResponse(
            "documentTemplateQuery", "new-document",
            "parameterName", "absPath",
            "parameterValueIsRelativePath", "false",
            "pickerConfiguration", "cms-pickers/documents",
            "pickerRemembersLastVisited", "true",
            "pickerRootPath", "/my/channel/path"
        );
    }

    @Test
    public void parameterWithRelativeJcrPath() throws Exception {
        tag.setDocumentTemplateQuery("new-document");
        tag.setParameterName("relPath");

        final ResolvedMount resolvedMount = createMock(ResolvedMount.class);
        final Mount mount = createMock(Mount.class);
        expect(resolvedMount.getMount()).andReturn(mount).anyTimes();
        expect(mount.getContentPath()).andReturn("/my/channel/path").anyTimes();
        hstRequestContext.setResolvedMount(resolvedMount);

        replay(resolvedMount, mount);

        assertManageContentResponse(
            "documentTemplateQuery", "new-document",
            "parameterName", "relPath",
            "parameterValueIsRelativePath", "true",
            "pickerConfiguration", "cms-pickers/documents",
            "pickerRemembersLastVisited", "true",
            "pickerRootPath", "/my/channel/path"
        );
    }

    @Test
    public void parameterWithoutJcrPathIsAbsolute() throws Exception {
        tag.setDocumentTemplateQuery("new-document");
        tag.setParameterName("string");

        assertManageContentResponse(
            "documentTemplateQuery", "new-document",
            "parameterName", "string",
            "parameterValueIsRelativePath", "false"
        );
    }

    @Test
    public void componentParameterWithAbsoluteJcrPathAndRelativeRootPath() throws Exception {
        assertWarned("Ignoring manageContent tag in template 'webfile:/freemarker/test.ftl'"
                + " for component parameter 'relPath': the @JcrPath annotation of the parameter"
                + " makes it store a relative path to the content root of the channel while the 'rootPath'"
                + " attribute of the manageContent tag points to the absolute path '/some/absolute/path'."
                + " Either make the root path relative to the channel content root,"
                + " or make the component parameter store an absolute path.",
                () -> {
                    tag.setDocumentTemplateQuery("new-document");
                    tag.setParameterName("relPath");
                    tag.setRootPath("/some/absolute/path");

                    assertEmptyResponse();
                }
        );
    }

    @Test
    public void parameterWithoutDocumentOrTemplateQuery() throws Exception {
        tag.setParameterName("test");

        assertManageContentResponse(
            "parameterName", "test",
            "parameterValueIsRelativePath", "false"
        );
    }

    @Test
    public void parameterValueAbsolutePath() throws Exception {
        window.setParameter("absPath", "/absolute/path");

        tag.setParameterName("absPath");
        final HippoBean document = createMock(HippoBean.class);
        tag.setHippobean(document);

        final AccessControlManager acm = createMock(AccessControlManager.class);
        final Privilege docEditPrivilege = createMock(Privilege.class);

        final MockNode root = MockNode.root(null, acm);
        hstRequestContext.setAttribute(CMS_USER_SESSION_ATTR_NAME, root.getSession());
        final MockNode handle = root.addNode("document", HippoNodeType.NT_HANDLE);

        expect(docEditPrivilege.getName()).andStubReturn(DOCUMENT_EDIT_REQUIRED_PRIVILEGE_NAME);
        expect(acm.getPrivileges(eq(handle.getPath()))).andStubReturn(new Privilege[]{docEditPrivilege});

        expect(document.getNode()).andReturn(handle);

        final ResolvedMount resolvedMount = createMock(ResolvedMount.class);
        final Mount mount = createMock(Mount.class);
        expect(resolvedMount.getMount()).andReturn(mount).anyTimes();
        expect(mount.getContentPath()).andReturn("/my/channel/path").anyTimes();
        hstRequestContext.setResolvedMount(resolvedMount);

        replay(document, resolvedMount, mount, acm, docEditPrivilege);

        assertManageContentResponse(
            "parameterName", "absPath",
            "parameterValue", "/absolute/path",
            "parameterValueIsRelativePath", "false",
            "pickerConfiguration", "cms-pickers/documents",
            "pickerRemembersLastVisited", "true",
            "pickerRootPath", "/my/channel/path",
            "uuid", handle.getIdentifier()
        );
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

        final AccessControlManager acm = createMock(AccessControlManager.class);
        final Privilege docEditPrivilege = createMock(Privilege.class);

        final MockNode root = MockNode.root(null, acm);
        hstRequestContext.setAttribute(CMS_USER_SESSION_ATTR_NAME, root.getSession());
        final MockNode handle = root.addNode("document", HippoNodeType.NT_HANDLE);

        expect(docEditPrivilege.getName()).andStubReturn(DOCUMENT_EDIT_REQUIRED_PRIVILEGE_NAME);
        expect(acm.getPrivileges(eq(handle.getPath()))).andStubReturn(new Privilege[]{docEditPrivilege});
        expect(document.getNode()).andReturn(handle);

        replay(resolvedMount, mount, document, acm, docEditPrivilege);

        assertManageContentResponse(
            "parameterName", "relPath",
            "parameterValue", "/mount/path/relative/path",
            "parameterValueIsRelativePath", "true",
            "pickerConfiguration", "cms-pickers/documents",
            "pickerRemembersLastVisited", "true",
            "pickerRootPath", "/mount/path",
            "uuid", handle.getIdentifier()
        );
    }

    @Test
    public void prefixedParameterValue() throws Exception {
        final String prefixedParameterName = ConfigurationUtils.createPrefixedParameterName("prefix", "absPath");
        window.setParameter(prefixedParameterName, "/absolute/path");
        window.setAttribute(RENDER_VARIANT, "prefix");

        tag.setParameterName("absPath");
        final HippoBean document = createMock(HippoBean.class);
        tag.setHippobean(document);

        final AccessControlManager acm = createMock(AccessControlManager.class);
        final Privilege docEditPrivilege = createMock(Privilege.class);

        final MockNode root = MockNode.root(null, acm);
        hstRequestContext.setAttribute(CMS_USER_SESSION_ATTR_NAME, root.getSession());
        final MockNode handle = root.addNode("document", HippoNodeType.NT_HANDLE);

        expect(docEditPrivilege.getName()).andStubReturn(DOCUMENT_EDIT_REQUIRED_PRIVILEGE_NAME);
        expect(acm.getPrivileges(eq(handle.getPath()))).andStubReturn(new Privilege[]{docEditPrivilege});
        expect(document.getNode()).andReturn(handle);

        final ResolvedMount resolvedMount = createMock(ResolvedMount.class);
        final Mount mount = createMock(Mount.class);
        expect(resolvedMount.getMount()).andReturn(mount).anyTimes();
        expect(mount.getContentPath()).andReturn("/my/channel/path").anyTimes();
        hstRequestContext.setResolvedMount(resolvedMount);

        replay(document, resolvedMount, mount, acm, docEditPrivilege);

        assertManageContentResponse(
            "parameterName", "absPath",
            "parameterValue", "/absolute/path",
            "parameterValueIsRelativePath", "false",
            "pickerConfiguration", "cms-pickers/documents",
            "pickerRemembersLastVisited", "true",
            "pickerRootPath", "/my/channel/path",
            "uuid", handle.getIdentifier()
        );
    }

    @Test
    public void emptyPrefixParameterValue() throws Exception {
        window.setParameter("absPath", "/absolute/path");
        window.setAttribute(RENDER_VARIANT, "");

        tag.setParameterName("absPath");
        final HippoBean document = createMock(HippoBean.class);
        tag.setHippobean(document);

        final AccessControlManager acm = createMock(AccessControlManager.class);
        final Privilege docEditPrivilege = createMock(Privilege.class);

        final MockNode root = MockNode.root(null, acm);
        hstRequestContext.setAttribute(CMS_USER_SESSION_ATTR_NAME, root.getSession());
        final MockNode handle = root.addNode("document", HippoNodeType.NT_HANDLE);

        expect(docEditPrivilege.getName()).andStubReturn(DOCUMENT_EDIT_REQUIRED_PRIVILEGE_NAME);
        expect(acm.getPrivileges(eq(handle.getPath()))).andStubReturn(new Privilege[]{docEditPrivilege});

        expect(document.getNode()).andReturn(handle);

        final ResolvedMount resolvedMount = createMock(ResolvedMount.class);
        final Mount mount = createMock(Mount.class);
        expect(resolvedMount.getMount()).andReturn(mount).anyTimes();
        expect(mount.getContentPath()).andReturn("/my/channel/path").anyTimes();
        hstRequestContext.setResolvedMount(resolvedMount);

        replay(document, resolvedMount, mount, acm, docEditPrivilege);

        assertManageContentResponse(
            "parameterName", "absPath",
            "parameterValue", "/absolute/path",
            "parameterValueIsRelativePath", "false",
            "pickerConfiguration", "cms-pickers/documents",
            "pickerRemembersLastVisited", "true",
            "pickerRootPath", "/my/channel/path",
            "uuid", handle.getIdentifier()
        );
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

        assertManageContentResponse(
            "parameterName", "pickerPath",
            "parameterValueIsRelativePath", "true",
            "pickerConfiguration", "picker-config",
            "pickerInitialPath", "/root-path/initial-path",
            "pickerRemembersLastVisited", "false",
            "pickerRootPath", "/root-path",
            "pickerSelectableNodeTypes", "node-type-1,node-type-2"
        );
    }

    @Test
    public void allParameters() throws Exception {
        tag.setDocumentTemplateQuery("new-newsdocument");
        tag.setRootPath("news/amsterdam");
        tag.setDefaultPath("2018/09/23");
        tag.setParameterName("newsDocument");

        final HippoBean document = createMock(HippoBean.class);
        tag.setHippobean(document);

        final AccessControlManager acm = createMock(AccessControlManager.class);
        final Privilege docEditPrivilege = createMock(Privilege.class);

        final MockNode root = MockNode.root(null, acm);
        hstRequestContext.setAttribute(CMS_USER_SESSION_ATTR_NAME, root.getSession());
        final MockNode handle = root.addNode("document", HippoNodeType.NT_HANDLE);

        expect(docEditPrivilege.getName()).andStubReturn(DOCUMENT_EDIT_REQUIRED_PRIVILEGE_NAME);
        expect(acm.getPrivileges(eq(handle.getPath()))).andStubReturn(new Privilege[]{docEditPrivilege});

        expect(document.getNode()).andReturn(handle);

        final Session jcrSession = createMock(Session.class);
        hstRequestContext.setSession(jcrSession);
        hstRequestContext.setSiteContentBasePath("my/channel/path");
        final Node folderNode = createMock(Node.class);
        expect(folderNode.isNodeType(HippoStdNodeType.NT_FOLDER)).andReturn(false);
        expect(folderNode.isNodeType(HippoStdNodeType.NT_DIRECTORY)).andReturn(true);
        expect(jcrSession.getNode("/my/channel/path/news/amsterdam")).andReturn(folderNode);

        replay(document, jcrSession, folderNode, acm, docEditPrivilege);

        assertManageContentResponse(
            "defaultPath", "2018/09/23",
            "documentTemplateQuery", "new-newsdocument",
            "parameterName", "newsDocument",
            "parameterValueIsRelativePath", "false",
            "rootPath", "news/amsterdam",
            "uuid", handle.getIdentifier()
        );
    }

    @Test(expected = JspException.class)
    public void exceptionWhileWritingToJspOutputsNothing() throws Exception {
        tag.setDocumentTemplateQuery("new-document");
        tag.setPageContext(new BrokenPageContext());
        assertEmptyResponse();
    }

    @Test
    public void setComponentParameterToNull() throws Exception {
        assertWarned("The parameterName attribute of a manageContent tag in template 'webfile:/freemarker/test.ftl' " +
                "is set to 'null'. Expected the name of an HST component parameter instead.",
                beforeEndTag(() -> tag.setParameterName(null)));
    }

    @Test
    public void setComponentParameterToEmpty() throws Exception {
        assertWarned("The parameterName attribute of a manageContent tag in template 'webfile:/freemarker/test.ftl' " +
                "is set to ''. Expected the name of an HST component parameter instead.",
                beforeEndTag(() -> tag.setParameterName("")));
    }

    @Test
    public void setComponentParameterToSpaces() throws Exception {
        assertWarned("The parameterName attribute of a manageContent tag in template 'webfile:/freemarker/test.ftl' " +
                "is set to '  '. Expected the name of an HST component parameter instead.",
                beforeEndTag(() -> tag.setParameterName("  ")));
    }

    @Test
    public void setDocumentTemplateQueryToNull() throws Exception {
        assertWarned("The documentTemplateQuery attribute of a manageContent tag in template " +
                "'webfile:/freemarker/test.ftl' is set to 'null'. Expected the name of a template query instead.",
                beforeEndTag(() -> tag.setDocumentTemplateQuery(null)));
    }

    @Test
    public void setDocumentTemplateQueryToEmpty() throws Exception {
        assertWarned("The documentTemplateQuery attribute of a manageContent tag in template"
                + " 'webfile:/freemarker/test.ftl' is set to ''. Expected the name of a template query instead.",
                beforeEndTag(() -> tag.setDocumentTemplateQuery("")));
    }

    @Test
    public void setDocumentTemplateQueryToSpaces() throws Exception {
        assertWarned("The documentTemplateQuery attribute of a manageContent tag in template"
                + " 'webfile:/freemarker/test.ftl' is set to '  '. Expected the name of a template query instead.",
                beforeEndTag(() -> tag.setDocumentTemplateQuery("  ")));
    }

    @Test
    public void setFolderTemplateQueryToNull() throws Exception {
        assertWarned("The folderTemplateQuery attribute of a manageContent tag"
                + " in template 'webfile:/freemarker/test.ftl' is set to 'null'." +
                " Expected the name of a template query instead.",
                beforeEndTag(() -> tag.setFolderTemplateQuery(null)));
    }

    @Test
    public void setFolderTemplateQueryToEmpty() throws Exception {
        assertWarned("The folderTemplateQuery attribute of a manageContent tag in template"
                + " 'webfile:/freemarker/test.ftl' is set to ''. Expected the name of a template query instead.",
                beforeEndTag(() -> tag.setFolderTemplateQuery("")));
    }

    @Test
    public void setFolderTemplateQueryToSpaces() throws Exception {
        assertWarned("The folderTemplateQuery attribute of a manageContent tag in template"
                + " 'webfile:/freemarker/test.ftl' is set to '  '. Expected the name of a template query instead.",
                beforeEndTag(() -> tag.setFolderTemplateQuery("  ")));
    }

    @Test
    public void setFolderTemplateQueryWithoutDefaultPath() throws Exception {
        assertWarned("The folderTemplateQuery attribute 'new-folder' is set on a manageContent tag, " +
                "but the defaultPath attribute is not set. The folderTemplateQuery attribute in template " +
                "'webfile:/freemarker/test.ftl' is ignored.",
                beforeEndTag(() -> {
                    tag.setParameterName("a-mandatory-parameter");
                    tag.setFolderTemplateQuery("new-folder");
                }));
    }

    @Test
    public void setDefaultPathWithoutDocumentTemplateQuery() throws Exception {
        assertWarned("The defaultPath attribute '/default/path' is set on a manageContent tag, " +
                "but the documentTemplateQuery attribute is not set. The defaultPath attribute in template " +
                "'webfile:/freemarker/test.ftl' is ignored.",
                beforeEndTag(() -> {
                    tag.setParameterName("a-mandatory-parameter");
                    tag.setDefaultPath("/default/path");
                }));
    }

    @Test
    public void rootPathNodeNotFound() throws Exception {
        tag.setRootPath("/exists/not");
        tag.setDefaultPath("2018/09/23");
        tag.setDocumentTemplateQuery("new-newsdocument");

        final Session jcrSession = createMock(Session.class);
        hstRequestContext.setSession(jcrSession);

        expect(jcrSession.getNode("/exists/not")).andThrow(new PathNotFoundException());
        replay(jcrSession);

        assertManageContentResponse("documentTemplateQuery", "new-newsdocument");
    }

    @Test
    public void rootPathNodeNotAFolder() throws Exception {
        tag.setRootPath("/not/a/folder");
        tag.setDefaultPath("2018/09/23");
        tag.setDocumentTemplateQuery("new-newsdocument");

        final Session jcrSession = createMock(Session.class);
        hstRequestContext.setSession(jcrSession);

        final Node notAFolderNode = createMock(Node.class);
        expect(notAFolderNode.isNodeType(HippoStdNodeType.NT_FOLDER)).andReturn(false);
        expect(notAFolderNode.isNodeType(HippoStdNodeType.NT_DIRECTORY)).andReturn(false);
        expect(jcrSession.getNode("/not/a/folder")).andReturn(notAFolderNode);

        replay(jcrSession, notAFolderNode);

        assertManageContentResponse("documentTemplateQuery", "new-newsdocument");
    }

    @Test
    public void pickerRootPathTest() throws Exception {
        tag.setParameterName("absPath");
        tag.setRootPath("relative/path");

        final Session jcrSession = createMock(Session.class);
        hstRequestContext.setSession(jcrSession);
        hstRequestContext.setSiteContentBasePath("my/channel/path");
        final Node folderNode = createMock(Node.class);
        expect(folderNode.isNodeType(HippoStdNodeType.NT_FOLDER)).andReturn(false);
        expect(folderNode.isNodeType(HippoStdNodeType.NT_DIRECTORY)).andReturn(true);
        expect(jcrSession.getNode("/my/channel/path/relative/path")).andReturn(folderNode);

        final ResolvedMount resolvedMount = createMock(ResolvedMount.class);
        final Mount mount = createMock(Mount.class);
        expect(resolvedMount.getMount()).andReturn(mount).anyTimes();
        expect(mount.getContentPath()).andReturn("/my/channel/path").anyTimes();
        hstRequestContext.setResolvedMount(resolvedMount);

        replay(jcrSession, folderNode, resolvedMount, mount);

        assertManageContentResponse(
            "parameterName", "absPath",
            "parameterValueIsRelativePath", "false",
            "pickerConfiguration", "cms-pickers/documents",
            "pickerRemembersLastVisited", "true",
            "pickerRootPath", "/my/channel/path/relative/path",
            "rootPath", "relative/path"
        );
    }

    @Test
    public void supplyChannelContentRootAsDefaultPickerRootPath() throws Exception {
        tag.setParameterName("absPath");

        final ResolvedMount resolvedMount = createMock(ResolvedMount.class);
        final Mount mount = createMock(Mount.class);
        expect(resolvedMount.getMount()).andReturn(mount).anyTimes();
        expect(mount.getContentPath()).andReturn("/my/channel/path").anyTimes();
        hstRequestContext.setResolvedMount(resolvedMount);

        replay(resolvedMount, mount);

        assertManageContentResponse(
            "parameterName", "absPath",
            "parameterValueIsRelativePath", "false",
            "pickerConfiguration", "cms-pickers/documents",
            "pickerRemembersLastVisited", "true",
            "pickerRootPath", "/my/channel/path"
        );
    }

    @Test
    public void supplyChannelContentRootAsDefaultPickerRootPathAndPickerInitialPath() throws Exception {
        tag.setParameterName("pickerNoRootPath");

        final ResolvedMount resolvedMount = createMock(ResolvedMount.class);
        final Mount mount = createMock(Mount.class);
        expect(resolvedMount.getMount()).andReturn(mount).anyTimes();
        expect(mount.getContentPath()).andReturn("/my/channel/path").anyTimes();
        hstRequestContext.setResolvedMount(resolvedMount);

        replay(resolvedMount, mount);

        assertManageContentResponse(
            "parameterName", "pickerNoRootPath",
            "parameterValueIsRelativePath", "false",
            "pickerConfiguration", "cms-pickers/documents",
            "pickerInitialPath", "/my/channel/path/initial-path",
            "pickerRemembersLastVisited", "true",
            "pickerRootPath", "/my/channel/path"
        );
    }

    @Test
    public void supplyChannelContentRootAsDefaultPickerRootPathAndAbsolutePickerInitialPath() throws Exception {
        tag.setParameterName("pickerAbsoluteInitialPath");

        final ResolvedMount resolvedMount = createMock(ResolvedMount.class);
        final Mount mount = createMock(Mount.class);
        expect(resolvedMount.getMount()).andReturn(mount).anyTimes();
        expect(mount.getContentPath()).andReturn("/my/channel/path").anyTimes();
        hstRequestContext.setResolvedMount(resolvedMount);

        replay(resolvedMount, mount);

        assertManageContentResponse(
            "parameterName", "pickerAbsoluteInitialPath",
            "parameterValueIsRelativePath", "false",
            "pickerConfiguration", "cms-pickers/documents",
            "pickerInitialPath", "/initial-path",
            "pickerRemembersLastVisited", "true",
            "pickerRootPath", "/my/channel/path"
        );
    }

    @Test
    public void matchingJcrInitialPathAndManageContentRootPath() throws Exception {
        tag.setRootPath("news");
        tag.setParameterName("pickerNews");

        final ResolvedMount resolvedMount = createMock(ResolvedMount.class);
        final Mount mount = createMock(Mount.class);
        expect(resolvedMount.getMount()).andReturn(mount).anyTimes();
        expect(mount.getContentPath()).andReturn("/my/channel/path").anyTimes();

        final Session jcrSession = createMock(Session.class);
        hstRequestContext.setSession(jcrSession);
        hstRequestContext.setSiteContentBasePath("my/channel/path");
        hstRequestContext.setResolvedMount(resolvedMount);

        final Node folderNode = createMock(Node.class);
        expect(folderNode.isNodeType(HippoStdNodeType.NT_FOLDER)).andReturn(false);
        expect(folderNode.isNodeType(HippoStdNodeType.NT_DIRECTORY)).andReturn(true);
        expect(jcrSession.getNode("/my/channel/path/news")).andReturn(folderNode);

        replay(jcrSession, folderNode, resolvedMount, mount);

        assertManageContentResponse(
            "parameterName", "pickerNews",
            "parameterValueIsRelativePath", "false",
            "pickerConfiguration", "cms-pickers/documents",
            "pickerInitialPath", "/my/channel/path/news",
            "pickerRemembersLastVisited", "true",
            "pickerRootPath", "/my/channel/path/news",
            "rootPath", "news"
        );
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
    private static class TestComponent extends GenericHstComponent {
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

        @Parameter(name = "pickerNoRootPath")
        @JcrPath(
                pickerInitialPath = "initial-path"
        )
        String getPickerNoRootPath();

        @Parameter(name = "pickerAbsoluteInitialPath")
        @JcrPath(
                pickerInitialPath = "/initial-path"
        )
        String getPickerAbsoluteInitialPath();

        @Parameter(name = "pickerNews")
        @JcrPath(
                pickerInitialPath = "news"
        )
        String getPickerNews();

        @Parameter(name = "string")
        String getString();
    }

}
