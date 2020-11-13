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
package org.hippoecm.hst.platform.configuration.xpages;

import java.util.HashMap;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

import org.hippoecm.hst.configuration.HstNodeTypes;
import org.hippoecm.hst.configuration.components.HstComponentConfiguration;
import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.configuration.hosting.VirtualHosts;
import org.hippoecm.hst.configuration.model.HstManager;
import org.hippoecm.hst.container.HstContainerRequestImpl;
import org.hippoecm.hst.container.ModifiableRequestContextProvider;
import org.hippoecm.hst.content.beans.ObjectBeanManagerException;
import org.hippoecm.hst.content.beans.manager.ObjectBeanManager;
import org.hippoecm.hst.content.beans.manager.ObjectBeanManagerImpl;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.core.beans.AbstractBeanTestCase;
import org.hippoecm.hst.core.component.HstURLFactory;
import org.hippoecm.hst.core.container.ContainerException;
import org.hippoecm.hst.core.container.HstContainerURL;
import org.hippoecm.hst.core.request.ResolvedMount;
import org.hippoecm.hst.core.request.ResolvedSiteMapItem;
import org.hippoecm.hst.mock.core.request.MockHstRequestContext;
import org.hippoecm.hst.platform.configuration.components.HstComponentConfigurationService;
import org.hippoecm.hst.site.HstServices;
import org.hippoecm.hst.test.BannerComponentInfo;
import org.hippoecm.hst.test.HeaderComponentInfo;
import org.hippoecm.hst.util.GenericHttpServletRequestWrapper;
import org.hippoecm.hst.util.HstRequestUtils;
import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.HippoSession;
import org.hippoecm.repository.util.JcrUtils;
import org.hippoecm.repository.util.WorkflowUtils;
import org.jetbrains.annotations.NotNull;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.cms7.services.contenttype.ContentTypeService;
import org.onehippo.repository.documentworkflow.DocumentWorkflow;
import org.powermock.api.easymock.PowerMock;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_IDENTIFIER;
import static org.hippoecm.repository.util.WorkflowUtils.getDocumentVariantNode;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;


@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"javax.management.*", "com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*", "org.w3c.dom.*", "com.sun.org.apache.xalan.*", "javax.activation.*", "javax.net.ssl.*"})
@PrepareForTest(HstRequestUtils.class)
public class ExperiencePageIT extends AbstractBeanTestCase {

    private Repository repository;
    private HippoSession adminSession;

    private HstManager hstSitesManager;
    private HstURLFactory hstURLFactory;
    private Session requestContextSession;
    private MockHstRequestContext requestContext;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        this.hstSitesManager = getComponent(HstManager.class.getName());
        this.hstURLFactory = getComponent(HstURLFactory.class.getName());
        requestContext = new MockHstRequestContext();

        requestContext.setContentTypes(HippoServiceRegistry.getService(ContentTypeService.class).getContentTypes());

        requestContextSession = createSession();
        requestContext.setSession(requestContextSession);

        final ObjectBeanManagerImpl objectBeanManager = new ObjectBeanManagerImpl(requestContextSession, getObjectConverter());

        final Map<Session, ObjectBeanManager> objectBeanManagerMap = new HashMap<>();
        objectBeanManagerMap.put(requestContextSession, objectBeanManager);
        requestContext.setNonDefaultObjectBeanManagers(objectBeanManagerMap);
        requestContext.setDefaultObjectBeanManager(objectBeanManager);

        final ResolvedMount resolvedMount = createNiceMock(ResolvedMount.class);
        final Mount mount = createNiceMock(Mount.class);
        expect(resolvedMount.getMount()).andStubReturn(mount);
        expect(mount.isPreview()).andStubReturn(false);

        replay(resolvedMount, mount);

        requestContext.setResolvedMount(resolvedMount);

        ModifiableRequestContextProvider.set(requestContext);

        repository = HstServices.getComponentManager().getComponent(Repository.class.getName() + ".delegating");
        adminSession = (HippoSession)repository.login(new SimpleCredentials("admin", "admin".toCharArray()));
    }

    @After
    public void tearDown() throws Exception {
        requestContextSession.logout();
        adminSession.logout();
        super.tearDown();
    }

    @Test
    public void experience_page_component_assertions() throws Exception {

        assertionsForExperiencePage("/unittestcontent/documents/unittestproject/experiences/expPage1",
                "expPage1", null);
    }

    @Test
    public void news_document_as_experience_page_component_assertions() throws Exception {

        final String pathToExperiencePage = "/unittestcontent/documents/unittestproject/News/2009/May/articleAsExpPage";
        assertionsForExperiencePage(pathToExperiencePage, "articleAsExpPage",  null);
    }

    @Test
    public void experience_page_document_variants_skips_hst_page_child_to_and_from_draft() throws Exception {
        final String pathToExperiencePage = "/unittestcontent/documents/unittestproject/News/2009/May/articleAsExpPage";

        try {

            // backup expPage1 before changing it with workflow
            JcrUtils.copy(adminSession, pathToExperiencePage, "/backupArticleAsExpPage");
            adminSession.save();

            final Node handle = adminSession.getNode(pathToExperiencePage);
            final DocumentWorkflow workflow = (DocumentWorkflow) adminSession.getWorkspace().getWorkflowManager().getWorkflow("default", handle);

            final Document draft = workflow.obtainEditableInstance();

            assertTrue(getDocumentVariantNode(handle, WorkflowUtils.Variant.UNPUBLISHED).get().hasNode("hst:xpage"));

            assertFalse("Draft variant should not get the 'hst:xpage' child",
                    draft.getNode(adminSession).hasNode("hst:xpage"));

            workflow.commitEditableInstance();

            assertTrue(getDocumentVariantNode(handle, WorkflowUtils.Variant.UNPUBLISHED).get().hasNode("hst:xpage"));

            assertFalse(draft.getNode(adminSession).hasNode("hst:xpage"));


        } finally {
            adminSession.getNode(pathToExperiencePage).remove();
            adminSession.move("/backupArticleAsExpPage", pathToExperiencePage);
            adminSession.save();
        }

    }

    @Test
    public void versioned_news_document_as_experience_page_component_assertions() throws Exception {

        final String pathToExperiencePage = "/unittestcontent/documents/unittestproject/News/2009/May/articleAsExpPage";

        try {

            // backup expPage1 before changing it with workflow
            JcrUtils.copy(adminSession, pathToExperiencePage, "/backupArticleAsExpPage");
            adminSession.save();

            final Node handle = adminSession.getNode(pathToExperiencePage);
            final DocumentWorkflow workflow = (DocumentWorkflow) adminSession.getWorkspace().getWorkflowManager().getWorkflow("default", handle);

            // now the 'workspace version is branch 'foo', but 'assertionsForExperiencePage' will load the master, which
            // will be loaded from VERSION HISTORY!
            workflow.branch("foo", "Foo");

            // publish foo branch
            workflow.publishBranch("foo");

            // render branch 'foo': Because the published variant is for 'live', it is
            // a version history XPage that will be loaded
            assertionsForExperiencePage(pathToExperiencePage, "articleAsExpPage", "foo");

        } finally {
            adminSession.getNode(pathToExperiencePage).remove();
            adminSession.move("/backupArticleAsExpPage", pathToExperiencePage);
            adminSession.save();
        }

    }

    private void assertionsForExperiencePage(final String pathToExperiencePage, final String handleName, final String branch) throws Exception {

        final GenericHttpServletRequestWrapper containerRequest = createContainerRequest(handleName);

        initContext(pathToExperiencePage, branch);

        final ResolvedSiteMapItem resolvedSiteMapItem = resolve(containerRequest);

        final HstComponentConfigurationService root = (HstComponentConfigurationService)resolvedSiteMapItem.getHstComponentConfiguration();

        assertThat(root.getReferenceName())
                .as("The root component for the experience page expected to have namespace 'r'")
                .isEqualTo("p1");

        assertThat(root.isXPage())
                .as("Expected experience page although it inherits from which inherits from 'hst:abstractpages/basepage'")
                .isTrue();

        /**
         * Although the root component for an XPage really comes from the XPage Layout, the actual HST-Page-Id should be from
         * the XPage Document and not the XPage layout, hence the canonical identifier and stored location should be from
         * the XPage document
         */
        final Node docXPage = adminSession.getNode(pathToExperiencePage + "/" + handleName + "/hst:xpage");
        if (branch == null) {
            assertThat(docXPage.getIdentifier())
                    .as("Expected for the root XPage component the identifier of the  XPage document and not the " +
                            "layout")
                    .isEqualTo(root.getCanonicalIdentifier());
        } else {
            assertCanonicalIdentifierFromVersionHistory(root);
        }
        assertThat(docXPage.getPath())
                .as("Expected for the root XPage component the identifier of the  XPage document and not the " +
                        "layout, NOTE, even for XPage doc loaded from VERSION HISTORY we expect WORKSPACE docXPage path!")
                .isEqualTo(root.getCanonicalStoredLocation());

        // the XPage for a runtime request gets 'copied' into a request bound XPage and the 'root' is of course
        // always shared (although not the request runtime instance, but the backing hst config component is)
        assertThat(root.isShared()).isTrue();

        HstComponentConfiguration header = root.getChildByName("header");
        assertThat(header).as("'header' component expected to part of Page Layout (config)").isNotNull();
        assertThat(header.isExperiencePageComponent()).isFalse();
        assertThat(header.isShared()).isTrue();
        assertThat(header.getReferenceName()).startsWith("p");

        // copied header component also has the dynamic parameter REFERENCE copied!
        assertThat(header.getDynamicComponentParameters().size())
                .as("Expected one dynamic parameter see " + HeaderComponentInfo.class.getName())
                .isEqualTo(1);

        assertThat(header.getDynamicComponentParameter("header").get().getDefaultValue())
                .isEqualTo("Yes My Header");


        HstComponentConfiguration leftmenu = root.getChildByName("leftmenu");
        assertThat(leftmenu).as("'leftmenu' component expected to part of Page Layout (config)").isNotNull();
        assertThat(leftmenu.isExperiencePageComponent()).isFalse();
        assertThat(header.isShared()).isTrue();
        assertThat(leftmenu.getReferenceName()).startsWith("p");


        HstComponentConfiguration main = root.getChildByName("main");
        assertThat(main).as("'main' component expected to part of Page Layout (config)").isNotNull();
        assertThat(main.isExperiencePageComponent()).isFalse();
        assertThat(main.isShared()).isTrue();
        assertThat(main.getReferenceName()).startsWith("p");

        HstComponentConfiguration container1 = main.getChildByName("container1");

        // the NAME (and most other properties!!!) is inherited from the Page Layout!!!
        assertThat(container1.getName()).as("Although the container name in the XPage Document variant " +
                "is '430df2da-3dc8-40b5-bed5-bdc44b8445c6', we expect the Page Layout Container Name")
                .isEqualTo("container1");

        // the XType is inherited from the Page Layout!!!
        assertThat(container1.getXType()).isEqualTo("HST.vBox");

        // VERY IMPORTANT : the canonical ID and the canonical stored location should be from the XPage document
        // which OVERRIDES the container ID and location from the Page Layout Config

        final Node xPageDocContainer1 = docXPage.getNode("430df2da-3dc8-40b5-bed5-bdc44b8445c6");

        // even for xpage doc from version history, canonical stored location will be workspace location, see
        // org.hippoecm.hst.content.beans.version.HippoBeanFrozenNodeUtils
        assertThat(container1.getCanonicalStoredLocation())
                .as("The canonical stored location for an XPage should have been REPLACED by the " +
                        "one from the XPage document!")
                .isEqualTo(xPageDocContainer1.getPath());

        // in case of a branch, it should be an identifier of the frozen node
        if (branch == null) {
            assertThat(container1.getCanonicalIdentifier())
                    .as("The identifier for an XPage should have been REPLACED by the one from the XPage document!")
                    .isEqualTo(xPageDocContainer1.getIdentifier());
        } else {
            assertCanonicalIdentifierFromVersionHistory(container1);
        }

        // container should be marked Experience Page Component
        assertThat(container1.isExperiencePageComponent()).isTrue();
        // quite subtle, but a Container Item from an XPage is not marked as shared since it has a 'non-shared'
        // representative in the XPage document
        assertThat(container1.isShared()).isFalse();

        // container should never be marked as inherited because would mean not editable
        assertThat(container1.isInherited()).isFalse();
        assertThat(container1.getReferenceName()).startsWith("p");

        if (handleName.equals("expPage1")) {
            HstComponentConfiguration banner = container1.getChildByName("banner");
            assertThat(banner).as("'banner' component expected to be part of Experience Page explicitly").isNotNull();

            assertThat(banner.getDynamicComponentParameters().size())
                    .as("Expected one dynamic parameter see " + BannerComponentInfo.class.getName())
                    .isEqualTo(1);

            assertThat(banner.getDynamicComponentParameter("path").get().getDefaultValue())
                    .isEqualTo("/some/default");

            assertThat(banner.getCanonicalStoredLocation()).isEqualTo(xPageDocContainer1.getPath() + "/banner");
            if (branch == null) {
                assertThat(banner.getCanonicalIdentifier()).isEqualTo(xPageDocContainer1.getNode("banner").getIdentifier());
            } else {
                assertCanonicalIdentifierFromVersionHistory(banner);
            }
            assertThat(banner.isExperiencePageComponent()).isTrue();
            assertThat(banner.isShared()).isFalse();
            assertThat(banner.getReferenceName()).startsWith("p");
        } else if (handleName.equals("articleAsExpPage")) {
            // articleAsExpPage does not have a banner item
            assertThat(container1.getChildByName("banner"))
                    .as("")
                    .isNull();
        }

        HstComponentConfiguration container2 = main.getChildByName("container2");
        assertThat(container2).as("'container2' component expected to be part of Experience Page explicitly").isNotNull();
        assertThat(container2.isExperiencePageComponent()).isTrue();
        assertThat(container2.isShared()).isFalse();
        assertThat(container2.getReferenceName()).startsWith("p");

    }

    private ResolvedSiteMapItem resolve(final GenericHttpServletRequestWrapper containerRequest) throws ContainerException {
        VirtualHosts vhosts = hstSitesManager.getVirtualHosts();
        ResolvedMount mount = vhosts.matchMount(HstRequestUtils.getFarthestRequestHost(containerRequest),
                HstRequestUtils.getRequestPath(containerRequest));

        setHstServletPath(containerRequest, mount);
        final HstContainerURL hstContainerURL = hstURLFactory.getContainerURLProvider().parseURL(containerRequest, new MockHttpServletResponse(), mount);
        return mount.matchSiteMapItem(hstContainerURL.getPathInfo());
    }

    private void initContext(final String pathToExperiencePage, final String branch) throws ObjectBeanManagerException {
        if (branch != null) {
            // Mock that the right branch is loaded!
            PowerMock.mockStaticPartial(HstRequestUtils.class, "getBranchIdFromContext");
            expect(HstRequestUtils.getBranchIdFromContext(anyObject())).andStubReturn(branch);
            PowerMock.replay(HstRequestUtils.class);
        }

        HippoBean requestBean = (HippoBean)requestContext.getObjectBeanManager().getObject(pathToExperiencePage);
        requestContext.setContentBean(requestBean);
    }

    @NotNull
    private GenericHttpServletRequestWrapper createContainerRequest(final String handleName) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setScheme("http");
        request.setServerName("localhost");
        request.addHeader("Host", "localhost");
        setRequestInfo(request, "/site", "/experiences/" + handleName + ".html");

        return new HstContainerRequestImpl(request, hstSitesManager.getPathSuffixDelimiter());
    }

    private void assertCanonicalIdentifierFromVersionHistory(final HstComponentConfiguration config) throws RepositoryException {
        final String canonicalIdentifier = config.getCanonicalIdentifier();
        final Node nodeByIdentifier = adminSession.getNodeByIdentifier(canonicalIdentifier);
        assertThat(nodeByIdentifier.getPath())
                .as("Expected a backing node from version history")
                .startsWith("/jcr:system/jcr:versionStorage/");
    }

    @Test
    public void xpage_layout_contains_container_not_present_in_XPage_Document() throws Exception {

        // This is a really delicate but important situation that can easily happen as follows:
        // 1. A cms author creates a new XPage Doc from XPage Layout X
        // 2. A developer changes XPage Layout X to have an EXTRA container
        // Now a rendered XPage from a doc variant SHOULD also represent the new container BUT must skip any container
        // items in there present in the XPage Layout: The container items in XPage Layout are 'bootstrap' items and
        // should never be rendered! The 'container id' can only be the one from the XPage Layout and the 'shared' flag
        // should be false to support ITEMS being added to it in the CM, see 'SHARED' in CmsComponentComponentWindowAttributeContributor
        // The CM will when a new item gets ADDED, also CREATE a NEW container in the XPage Doc Variant. This test will
        // be covered in Page-Composer

        final String pathToExperiencePage = "/unittestcontent/documents/unittestproject/experiences/expPage1";

        try {

            // backup expPage1 before changing it with workflow
            JcrUtils.copy(adminSession, pathToExperiencePage, "/backupExpPage1");
            adminSession.save();


            // remove one of the containers from the XPage document and assert the behavior: The XPage layout Container
            // not represented in the Xpage document should be present with SHARED = FALSE...which is really a niche
            // situation...it must have SHARED = false since CM interactions are done against the XPage document and not
            // against the XPage Layout
            adminSession.getNode("/unittestcontent/documents/unittestproject/experiences/expPage1/expPage1/hst:xpage/430df2da-3dc8-40b5-bed5-bdc44b8445c6").remove();
            adminSession.save();



            final GenericHttpServletRequestWrapper containerRequest = createContainerRequest("expPage1");
            initContext(pathToExperiencePage, null);
            final ResolvedSiteMapItem resolvedSiteMapItem = resolve(containerRequest);

            final HstComponentConfiguration root = resolvedSiteMapItem.getHstComponentConfiguration();

            HstComponentConfiguration container1 = root.getChildByName("main").getChildByName("container1");

            // container1 is not represented in the XPage document, hence is expected to be taken from the XPage Layout!
            // HOWEVER it should not contain the banner item from the XPage Layout container!

            assertThat(container1.getCanonicalStoredLocation())
                    .as("The request based XPage config expected using XPage Layout container since document " +
                            "variant XPage misses the container1")
                    .isEqualTo("/hst:hst/hst:configurations/unittestproject/hst:xpages/xpage1/main/container1");

            assertThat(container1.getChildren().size())
            .as("Expected that the banner item was removed from the XPage Layout container represented in a " +
                    "request based XPage config").isEqualTo(0);

            // assert that the backing XPage Layout *really* had a child below the container1
            final Map<String, HstComponentConfiguration> xPages = resolvedSiteMapItem.getResolvedMount().getMount().getHstSite().getComponentsConfiguration().getXPages();

            assertThat(xPages.get("xpage1").getChildByName("main").getChildByName("container1").getChildren().size())
                    .as("The XPage Layout really had 1 container item which is REMOVED in the XPage Doc " +
                            "config model")
                    .isEqualTo(1);


            assertThat(container1.isExperiencePageComponent())
                    .as("Even though config from XPage layout hst config, still expected to be marked as " +
                            "experience component")
                    .isTrue();

            assertThat(container1.isShared())
                    .as("Even though config from XPage layout hst config, the purpose in the Xpage config for " +
                            "the request is that the container is NOT SHARED since a CM webmaster should be able to add " +
                            "a new container item in it")
                    .isFalse();


            assertThat(container1.isUnresolvedXpageLayoutContainer()).
                    as("container1 is unresolved since the XPage Doc does NOT have the 'container equivalent' ")
                    .isTrue();



            // container should never be marked as inherited because would mean not editable
            assertThat(container1.isInherited())
                    .as("XPage Container should even now not be inherited since it most be editable by a CM")
                    .isFalse();

            // container2 is represented in the XPage document
            HstComponentConfiguration container2 = root.getChildByName("main").getChildByName("container2");

            assertThat(container2.getCanonicalStoredLocation())
                    .as("Expected container from XPage document")
                    .isEqualTo("/unittestcontent/documents/unittestproject/experiences/expPage1/expPage1/hst:xpage/430df2da-3dc8-40b5-bed5-bdc44b8445c7");

            assertThat(container2.isUnresolvedXpageLayoutContainer()).
                    as("container2 is resolved since the XPage Doc DOES have the 'container equivalent' ")
                    .isFalse();

        } finally {
            adminSession.getNode(pathToExperiencePage).remove();
            adminSession.move( "/backupExpPage1", pathToExperiencePage);
            adminSession.save();
        }

    }


    @Test
    public void doc_variant_xpage_contains_container_not_present_in_xpage_layout() throws Exception {

        // This is a very delicate situation: Assume a developer decides to remove a specific container from an XPage
        // layout : If (s)he does so, it might be that XPage documents still have containers that referenced the removed
        // XPage layout container : In that case, the Request Based XPage HstComponentConfiguration instance should
        // NOT have the removed container! We simply can't know where the removed container belonged in the page any way

        // To mimic the above, we first add a container to the XPage document variant 'expPage1' : this container is thus
        // not represented in the XPage layout (hst config), and thus has equal interaction as if a in-use container would get removed
        // from the XPage layout (hst config)

        final String pathToExperiencePage = "/unittestcontent/documents/unittestproject/experiences/expPage1";

        try {

            // backup expPage1 before changing it with workflow
            JcrUtils.copy(adminSession, pathToExperiencePage, "/backupExpPage1");
            adminSession.save();


            // add a new container to Xpage Doc which is not represented in XPage layout
            JcrUtils.copy(adminSession,
                    "/unittestcontent/documents/unittestproject/experiences/expPage1/expPage1/hst:xpage/430df2da-3dc8-40b5-bed5-bdc44b8445c7",
                    "/unittestcontent/documents/unittestproject/experiences/expPage1/expPage1/hst:xpage/430df2da-3dc8-40b5-bed5-bdc44b8445c8");

            adminSession.save();

            final GenericHttpServletRequestWrapper containerRequest = createContainerRequest("expPage1");
            initContext(pathToExperiencePage, null);
            final ResolvedSiteMapItem resolvedSiteMapItem = resolve(containerRequest);

            final HstComponentConfiguration root = resolvedSiteMapItem.getHstComponentConfiguration();

            assertThat(root.getChildByName("main").getChildren().size())
                    .as("Although the XPage Document contains 3 containers, only 2 are expected since the " +
                            "Page Layout does only represent 2 containers")
                    .isEqualTo(2);

        } finally {
            adminSession.getNode(pathToExperiencePage).remove();
            adminSession.move("/backupExpPage1", pathToExperiencePage);
            adminSession.save();
        }
    }

    /**
     * xpage1 Page Layout inherits from : "hst:referencecomponent: hst:abstractpages/basepage" FROM common configuration!
     *
     * If we add a 'container' to /hst:hst/hst:configurations/unittestcommon/hst:abstractpages/basepage then
     * the hippo:identifier of this container WON'T be replaced with the document page container (for now! A future FEATURE
     * could be supporting hijacking a (shared/inherited) CONTAINER for a single XPage document which we now do not
     * support
     *
     * @throws Exception
     */
    @Test
    public void xPage_doc_supports_hippoIdentifier_from_xpage_layout_inherited_from_base() throws Exception {

        final String pathToExperiencePage = "/unittestcontent/documents/unittestproject/experiences/expPage1";

         try {

             JcrUtils.copy(adminSession, pathToExperiencePage, "/backupExpPage1");
             final Node newContainer = adminSession.getNode("/hst:hst/hst:configurations/unittestcommon/hst:abstractpages/basepage")
                     .addNode("test-container", HstNodeTypes.NODETYPE_HST_CONTAINERCOMPONENT);
             final String hippoIdentifier = newContainer.getProperty(HIPPO_IDENTIFIER).getString();

             // make sure the XPage document now uses the 'abstract base page' hippoIdentifier

             final String xpagePath = pathToExperiencePage + "/expPage1/hst:xpage";

             adminSession.move(xpagePath + "/430df2da-3dc8-40b5-bed5-bdc44b8445c6",
                     xpagePath + "/" + hippoIdentifier);

             adminSession.save();

             final GenericHttpServletRequestWrapper containerRequest = createContainerRequest("expPage1");
             initContext(pathToExperiencePage, null);
             final ResolvedSiteMapItem resolvedSiteMapItem = resolve(containerRequest);

             final HstComponentConfiguration root = resolvedSiteMapItem.getHstComponentConfiguration();

             HstComponentConfiguration testContainer = root.getChildByName("test-container");
             assertThat(testContainer).as("'test-container' component expected to part of inherited Page Layout (config)").isNotNull();
             assertThat(testContainer.isExperiencePageComponent())
                     .as("test-container is from shared and should not be possible to hijack by Experiece Page " +
                             "Document (yet, future feature?), even though the hippo:identifier matches")
                     .isFalse();
             assertThat(testContainer.isShared()).isTrue();

             assertThat(testContainer.isInherited())
                     .as("Expected to be inherited since not manageable in CM by webmaster since coming from " +
                             "inherited configuration")
                     .isTrue();

             HstComponentConfiguration banner = testContainer.getChildByName("banner");

             assertThat(banner)
                     .as("Banner from XPage Document should NOT be merged into the container from the abstract base page")
                     .isNull();

         } finally {

             adminSession.getNode(pathToExperiencePage).remove();
             adminSession.getNode("/hst:hst/hst:configurations/unittestcommon/hst:abstractpages/basepage/test-container").remove();
             adminSession.move( "/backupExpPage1", pathToExperiencePage);
             adminSession.save();

         }
    }

    @Test
    public void xPage_doc_results_in_stable_child_order_namespaces_and_id_if_no_changes_occur() throws Exception {

        final String pathToExperiencePage = "/unittestcontent/documents/unittestproject/experiences/expPage1";

        final GenericHttpServletRequestWrapper containerRequest = createContainerRequest("expPage1");
        initContext(pathToExperiencePage, null);

        final HstComponentConfiguration a = resolve(containerRequest).getHstComponentConfiguration();
        final HstComponentConfiguration b = resolve(containerRequest).getHstComponentConfiguration();

        compareReferenceNamesComponentOrderAndIds(a, b);
    }

    private void compareReferenceNamesComponentOrderAndIds(final HstComponentConfiguration a, final HstComponentConfiguration b) {

        assertEquals(format("Expected '%s' to have same name as '%s'", a.getCanonicalStoredLocation(), b.getCanonicalStoredLocation()),
                a.getName(), b.getName());
        assertEquals(format("Expected '%s' to have same reference name (namespace) as '%s'", a.getCanonicalStoredLocation(), b.getCanonicalStoredLocation()),
                a.getReferenceName(), b.getReferenceName());

        assertEquals(format("Expected '%s' to have same id as '%s'", a.getCanonicalStoredLocation(), b.getCanonicalStoredLocation()),
                a.getId(), b.getId());

        for (HstComponentConfiguration aChild : a.getChildren().values()) {
            HstComponentConfiguration bChild = b.getChildByName(aChild.getName());
            compareReferenceNamesComponentOrderAndIds(aChild, bChild);
        }
    }

}
