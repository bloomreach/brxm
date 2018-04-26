/*
 *  Copyright 2018 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.content.beans;

import java.rmi.RemoteException;
import java.util.List;
import java.util.Optional;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.jcr.version.Version;
import javax.jcr.version.VersionHistory;
import javax.jcr.version.VersionManager;

import org.hippoecm.hst.AbstractBeanTestCase;
import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.container.ModifiableRequestContextProvider;
import org.hippoecm.hst.content.beans.manager.ObjectBeanManager;
import org.hippoecm.hst.content.beans.manager.ObjectBeanManagerImpl;
import org.hippoecm.hst.content.beans.manager.ObjectConverter;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.content.beans.standard.HippoFolderBean;
import org.hippoecm.hst.content.beans.standard.HippoHtml;
import org.hippoecm.hst.content.beans.version.HippoBeanFrozenNode;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.core.request.ResolvedMount;
import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.util.JcrUtils;
import org.hippoecm.repository.util.WorkflowUtils;
import org.junit.Before;
import org.junit.Test;
import org.onehippo.cms7.services.hst.Channel;
import org.onehippo.repository.documentworkflow.DocumentWorkflow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_VERSION_HISTORY_PROPERTY;
import static org.hippoecm.repository.api.HippoNodeType.NT_HIPPO_VERSION_INFO;
import static org.onehippo.repository.util.JcrConstants.JCR_VERSION_HISTORY;
import static org.onehippo.repository.util.JcrConstants.MIX_VERSIONABLE;
import static org.onehippo.repository.util.JcrConstants.NT_VERSION_HISTORY;

public class TestVersionedBean extends AbstractBeanTestCase {

    private ObjectConverter objectConverter;
    private ObjectBeanManager obm;
    private Session previewUser;

    private Node homeHandle;
    private Node aboutUsHandle;
    private VersionManager versionManager;
    private VersionHistory versionHistoryHome;
    private VersionHistory versionHistoryAboutUs;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        versionManager = session.getWorkspace().getVersionManager();
        homeHandle = session.getNode("/unittestcontent/documents/unittestproject/common/homepage");

        aboutUsHandle = session.getNode("/unittestcontent/documents/unittestproject/common/aboutfolder/about-us");

        // make backups to restore later
        JcrUtils.copy(session, homeHandle.getPath(), homeHandle.getPath() + "-copy");
        JcrUtils.copy(session, aboutUsHandle.getPath(), aboutUsHandle.getPath() + "-copy");
        session.save();

        createVersionHistoryFixture();

        objectConverter = getObjectConverter();

        previewUser = session.getRepository().login(new SimpleCredentials("previewuser", "previewuserpass".toCharArray()));

        obm = new ObjectBeanManagerImpl(previewUser, objectConverter);
    }

    @Override
    public void tearDown() throws Exception {

        ModifiableRequestContextProvider.clear();

        if (previewUser != null && previewUser.isLive()) {
            previewUser.logout();
        }
        homeHandle.remove();
        aboutUsHandle.remove();

        JcrUtils.copy(session, "/unittestcontent/documents/unittestproject/common/homepage-copy",
                "/unittestcontent/documents/unittestproject/common/homepage");
        JcrUtils.copy(session, "/unittestcontent/documents/unittestproject/common/aboutfolder/about-us-copy",
                "/unittestcontent/documents/unittestproject/common/aboutfolder/about-us");

        session.save();

        super.tearDown();
    }

    /**
     * below creates version 1.0, 1.1 and 1.2 of the home page document and labels them with christmas-live or master-live
     * and it creates version 1.0, 1.1 and 1.2 of the about-us document and labels them also with christmas-live and
     * master-live
     */
    private void createVersionHistoryFixture() throws WorkflowException, RepositoryException, RemoteException {
        final DocumentWorkflow wf = WorkflowUtils.getWorkflow(homeHandle, "default", DocumentWorkflow.class)
                .orElseThrow(() -> new WorkflowException("Could not get workflow"));

        changeTitleContentAndCommit(homeHandle, wf, "The christmas Title", "This is the content of the christmas homepage");

        versionHistoryHome = versionManager.getVersionHistory(
                WorkflowUtils.getDocumentVariantNode(homeHandle, WorkflowUtils.Variant.UNPUBLISHED).get().getPath());

        final Document doc = wf.version();
        assertThat(doc.getNode(session).getName()).isEqualTo("1.1");
        versionHistoryHome.addVersionLabel("1.1", "christmas-live", true);

        changeTitleContentAndCommit(homeHandle, wf, "The master Title", "This is the content of the master homepage");

        wf.version();
        versionHistoryHome.addVersionLabel("1.2", "master-live", true);

        changeTitleAndCommit(homeHandle, wf, "The new master Title");

        // publish triggers new version
        wf.publish();
        // overrides the 1.1 master-live version
        versionHistoryHome.addVersionLabel("1.3", "master-live", true);

        final DocumentWorkflow wf2 = WorkflowUtils.getWorkflow(aboutUsHandle, "default", DocumentWorkflow.class)
                .orElseThrow(() -> new WorkflowException("Could not get workflow"));

        changeTitleAndCommit(aboutUsHandle, wf2, "The Christmas Title About us");

        versionHistoryAboutUs = versionManager.getVersionHistory(
                WorkflowUtils.getDocumentVariantNode(aboutUsHandle, WorkflowUtils.Variant.UNPUBLISHED).get().getPath());

        final Document doc2 = wf2.version();
        assertThat(doc2.getNode(session).getName()).isEqualTo("1.1");
        versionHistoryAboutUs.addVersionLabel("1.1", "christmas-live", true);

        changeTitleAndCommit(aboutUsHandle, wf2, "The master Title About us");

        wf2.version();
        versionHistoryAboutUs.addVersionLabel("1.2", "master-live", true);

        changeTitleAndCommit(aboutUsHandle, wf2, "The new Christmas Title About us");
        // publish triggers new version
        wf2.publish();
        // overrides the 1.0 christmas-live version
        versionHistoryAboutUs.addVersionLabel("1.3", "christmas-live", true);

        confirm_version_fixture();
    }

    private void changeTitleAndCommit(final Node handle, final DocumentWorkflow wf, final String newTitle) throws RepositoryException, WorkflowException, RemoteException {
        changeTitleContentAndCommit(handle, wf, newTitle, null);
    }

    private void changeTitleContentAndCommit(final Node handle, final DocumentWorkflow wf, final String newTitle,
                                             final String newContent) throws RepositoryException, WorkflowException, RemoteException {
        wf.obtainEditableInstance();
        // assert after obtainEditableInstance the preview variant is present, is versionable AND that the handle also
        // has version info
        final Optional<Node> previewVariant = WorkflowUtils.getDocumentVariantNode(handle, WorkflowUtils.Variant.UNPUBLISHED);
        assertThat(previewVariant.orElse(null)).isNotNull();
        assertThat(previewVariant.get().isNodeType(MIX_VERSIONABLE)).isTrue();
        final Node versionHistoryNode = previewVariant.get().getProperty(JCR_VERSION_HISTORY).getNode();
        assertThat(versionHistoryNode.getPrimaryNodeType().getName())
                .isEqualTo(NT_VERSION_HISTORY);

        assertThat(handle.isNodeType(NT_HIPPO_VERSION_INFO)).isTrue();
        assertThat(handle.getProperty(HIPPO_VERSION_HISTORY_PROPERTY).getString())
                .isEqualTo(versionHistoryNode.getIdentifier());

        final Node draft = WorkflowUtils.getDocumentVariantNode(handle, WorkflowUtils.Variant.DRAFT).get();
        draft.setProperty("unittestproject:title", newTitle);
        if (newContent != null) {
            draft.getNode("unittestproject:body").setProperty("hippostd:content", newContent);
        }

        session.save();
        wf.commitEditableInstance();
    }

    private void confirm_version_fixture() throws RepositoryException {

        final Version homeMaster = versionHistoryHome.getVersionByLabel("master-live");
        assertThat(homeMaster.getName()).isEqualTo("1.3");

        final Version homeChristmas = versionHistoryHome.getVersionByLabel("christmas-live");
        assertThat(homeChristmas.getName()).isEqualTo("1.1");

        final Version aboutMaster = versionHistoryAboutUs.getVersionByLabel("master-live");
        assertThat(aboutMaster.getName()).isEqualTo("1.2");

        final Version aboutChristmas = versionHistoryAboutUs.getVersionByLabel("christmas-live");
        assertThat(aboutChristmas.getName()).isEqualTo("1.3");

    }

    @Test
    public void get_non_versioned_homepage_bean() throws Exception {
        Object workspaceHomepage = obm.getObject("/unittestcontent/documents/unittestproject/common/homepage");
        assertThat(workspaceHomepage).isInstanceOf(PersistableTextPage.class);
    }


    private void initContext(final String branch) throws RepositoryException {

        final Channel channel = createNiceMock(Channel.class);
        expect(channel.getBranchOf()).andStubReturn("unittestproject");
        expect(channel.getBranchId()).andStubReturn(branch);

        final Mount mount = createNiceMock(Mount.class);
        expect(mount.getChannel()).andStubReturn(channel);
        expect(mount.isPreview()).andStubReturn(false);

        final ResolvedMount resolvedMount = createNiceMock(ResolvedMount.class);
        expect(resolvedMount.getMount()).andStubReturn(mount);

        final HstRequestContext requestContext = createNiceMock(HstRequestContext.class);
        expect(requestContext.getResolvedMount()).andStubReturn(resolvedMount);

        ModifiableRequestContextProvider.set(requestContext);

        replay(channel, mount, resolvedMount, requestContext);

    }

    @Test
    public void get_versioned_homepage_bean_for_christmas() throws Exception {

        // without branch context
        final PersistableTextPage workspaceHomePage = (PersistableTextPage) obm.getObject("/unittestcontent/documents/unittestproject/common/homepage");
        assertThat(workspaceHomePage.getBody().getContent()).isEqualTo("This is the content of the master homepage");

        assertThat(workspaceHomePage.getTitle())
                .as("Expected live workspace version")
                .isEqualTo("The new master Title");

        initContext("christmas");

        final PersistableTextPage versionedHomePage = (PersistableTextPage) obm.getObject("/unittestcontent/documents/unittestproject/common/homepage");

        assertThat(versionedHomePage.getTitle())
                .as("Expected versioned christmas edition")
                .isEqualTo("The christmas Title");

        final String versionedHomePagePath = versionedHomePage.getPath();

        assertThat(versionedHomePagePath)
                .as("Although the provided node is a versioned node, the path should still be the workspace path" +
                        "for example for link rewriting")
                .startsWith("/unittestcontent");

        assertThat(versionedHomePage.getNode()).isInstanceOf(HippoBeanFrozenNode.class);

        assertThat(((HippoBeanFrozenNode) versionedHomePage.getNode()).getFrozenNode().getPath())
                .as("The real backing node is the frozen node with a version history path")
                .startsWith("/jcr:system");

        assertThat(versionedHomePagePath).isEqualTo(workspaceHomePage.getPath());

        final Node node = versionedHomePage.getNode();

        assertThat(node).isInstanceOf(HippoBeanFrozenNode.class);

        assertThat(node.getName()).isEqualTo("homepage");

        final HippoHtml body = versionedHomePage.getBody();

        assertThat(body.getPath()).isEqualTo("/unittestcontent/documents/unittestproject/common/homepage/homepage/unittestproject:body");

        assertThat(body.getContent()).isEqualTo("This is the content of the christmas homepage");

        final Object bean = versionedHomePage.getBean("unittestproject:body");
        assertThat(bean instanceof HippoHtml).isTrue();

        assertThat(body.isSelf((HippoHtml) bean));

        final List<HippoHtml> childBeans = versionedHomePage.getChildBeans(HippoHtml.class);
        assertThat(childBeans.size()).isEqualTo(1);
        assertThat(childBeans.get(0).isSelf(body));

        final List<HippoBean> allChildren = versionedHomePage.getChildBeans(HippoBean.class);

        assertThat(allChildren.size()).isEqualTo(1);
        assertThat(allChildren.get(0).isSelf(body));

        final PersistableTextPage versionedHomePageViaBody = (PersistableTextPage) body.getParentBean();

        assertThat(versionedHomePageViaBody.getTitle())
                .as("Expected versioned christmas edition")
                .isEqualTo("The christmas Title");


        assertThat(versionedHomePageViaBody.isSelf(versionedHomePage)).isTrue();

        final HippoBean expectedFolderBean = versionedHomePage.getParentBean();

        assertThat(expectedFolderBean).isInstanceOf(HippoFolderBean.class);

        assertThat(expectedFolderBean.getNode()).isNotInstanceOfAny(HippoBeanFrozenNode.class);

        assertThat(expectedFolderBean.getNode().getPath())
                .as("Expected parent of a HippoDocumentBean from version history is the " +
                        "parent of the handle of document variant in workspace")
                .isEqualTo(workspaceHomePage.getNode().getParent().getParent().getPath());
        assertThat(versionedHomePagePath).startsWith(expectedFolderBean.getNode().getPath());


        final HippoBean expectedFolderBean2 = versionedHomePageViaBody.getParentBean();

        assertThat(expectedFolderBean.isSelf(expectedFolderBean2)).isTrue();

        assertThat(versionedHomePage.isAncestor(body)).isTrue();
        assertThat(body.isAncestor(versionedHomePage)).isFalse();
        assertThat(versionedHomePageViaBody.isAncestor(body)).isTrue();

        assertThat(versionedHomePage.isDescendant(body)).isFalse();
        assertThat(body.isDescendant(versionedHomePage)).isTrue();


    }

    @Test
    public void get_versioned_children_with_SNS_involved() throws Exception {

        final Node previewVariant = WorkflowUtils.getDocumentVariantNode(homeHandle, WorkflowUtils.Variant.UNPUBLISHED).get();
        final String bodyPath = previewVariant.getNode("unittestproject:body").getPath();
        // create SNS
        JcrUtils.copy(session, bodyPath, bodyPath);

        session.save();

        final DocumentWorkflow wf = WorkflowUtils.getWorkflow(homeHandle, "default", DocumentWorkflow.class)
                .orElseThrow(() -> new WorkflowException("Could not get workflow"));

        changeTitleContentAndCommit(homeHandle, wf, "The Christmas Title", "This is the content of the Christmas homepage");

        wf.version();
        versionHistoryHome.addVersionLabel("1.4", "christmas-live", true);

        initContext("christmas");

        final PersistableTextPage versionedHomePage = (PersistableTextPage) obm.getObject("/unittestcontent/documents/unittestproject/common/homepage");

        final List<HippoHtml> childBeans = versionedHomePage.getChildBeans(HippoHtml.class);
        assertThat(childBeans.size()).isEqualTo(2);

        assertThat(childBeans.get(0).getPath()).isEqualTo("/unittestcontent/documents/unittestproject/common/homepage/homepage/unittestproject:body");
        assertThat(childBeans.get(1).getPath()).isEqualTo("/unittestcontent/documents/unittestproject/common/homepage/homepage/unittestproject:body[2]");

    }
}
