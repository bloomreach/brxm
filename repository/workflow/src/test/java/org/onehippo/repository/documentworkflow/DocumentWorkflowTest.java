/*
 * Copyright 2013-2020 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onehippo.repository.documentworkflow;

import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.Calendar;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.jcr.PropertyType;

import org.apache.commons.scxml2.model.EnterableState;
import org.apache.jackrabbit.util.ISO8601;
import org.assertj.core.api.Assertions;
import org.hippoecm.repository.HippoStdNodeType;
import org.hippoecm.repository.HippoStdPubWfNodeType;
import org.hippoecm.repository.api.HippoNodeType;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.onehippo.repository.mock.MockNode;
import org.onehippo.repository.mock.MockValue;
import org.onehippo.repository.scxml.MockAccessManagedSession;
import org.onehippo.repository.scxml.MockWorkflowContext;
import org.onehippo.repository.scxml.SCXMLWorkflowContext;
import org.onehippo.repository.scxml.SCXMLWorkflowData;
import org.onehippo.repository.scxml.SCXMLWorkflowExecutor;

import static org.hippoecm.repository.api.HippoNodeType.HIPPO_AVAILABILITY;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_MIXIN_BRANCH_INFO;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_PROPERTY_BRANCH_ID;

public class DocumentWorkflowTest extends BaseDocumentWorkflowTest {

    @BeforeClass
    public static void beforeClass() throws Exception {
        createDocumentWorkflowSCXMLRegistry();
    }

    @AfterClass
    public static void afterClass() throws Exception {
        destroyDocumentWorkflowSCXMLRegistry();
    }

    public DocumentWorkflowImpl getDocumentWorkflowImpl() throws RemoteException {
        return new DocumentWorkflowImpl();
    }

    protected static Set<String> getSCXMLStatusStateIds(SCXMLWorkflowExecutor<SCXMLWorkflowContext, SCXMLWorkflowData> executor) {
        Set<EnterableState> targets = executor.getSCXMLExecutor().getCurrentStatus().getStates();

        if (targets.isEmpty()) {
            return Collections.emptySet();
        }

        Set<String> ids = new TreeSet<>();

        for (EnterableState target : targets) {
            ids.add(target.getId());
        }

        return ids;
    }

    protected static void assertMatchingSCXMLStates(SCXMLWorkflowExecutor executor, TreeSet<String> states) {
        Set<String> stateIds = getSCXMLStatusStateIds(executor);
        Assertions.assertThat(stateIds).containsExactlyElementsOf(states);
    }

    @Test
    public void testInitializeSCXML() throws Exception {

        MockAccessManagedSession session = new MockAccessManagedSession(MockNode.root());
        MockWorkflowContext workflowContext = new MockWorkflowContext("testuser", session);
        DocumentWorkflowImpl wf = getDocumentWorkflowImpl();
        wf.setWorkflowContext(workflowContext);

        MockNode handleNode = session.getRootNode().addNode("test", HippoNodeType.NT_HANDLE);

        addVariant(handleNode, HippoStdNodeType.DRAFT);
        wf.setNode(handleNode);
        wf.getWorkflowExecutor().start(null);
        Assert.assertEquals("testuser", wf.getWorkflowContext().getUserIdentity());
    }

    @Test
    public void testVariants() throws Exception {

        MockAccessManagedSession session = new MockAccessManagedSession(MockNode.root());
        MockWorkflowContext workflowContext = new MockWorkflowContext("testuser", session);
        MockNode handleNode = session.getRootNode().addNode("test", HippoNodeType.NT_HANDLE);
        DocumentWorkflowImpl wf = getDocumentWorkflowImpl();
        wf.setWorkflowContext(workflowContext);
        wf.setNode(handleNode);

        // no document
        assertMatchingKeyValues(wf.hints(), HintsBuilder.build()
                .hints());
        assertMatchingSCXMLStates(wf.getWorkflowExecutor(), StatesBuilder.build()
                .noDocument()
                .states()
        );

        MockNode publishedVariant = addVariant(handleNode, HippoStdNodeType.PUBLISHED);

        // set (only) author permission
        session.setPermissions(publishedVariant.getPath(), "hippo:author", true);

        // only published
        assertMatchingKeyValues(wf.hints(), HintsBuilder.build()
                .status(true).isLive(true).previewAvailable(true).checkModified(false).noEdit().editable()
                .requestPublication(false).requestDepublication(true).listVersions()
                .listBranches().branch(true).getBranch(false).checkoutBranch(false).removeBranch(false)
                .campaign(false).removeCampaign(false)//.labelVersion(false).removeLabelVersion(false)
                .terminateable(false).copy()
                .saveUnpublished(false)
                .hints());

        // branch 'foo' does not exist
        // "foo" does not exist so not possible to depublish
        assertMatchingKeyValues(wf.hints("foo"), HintsBuilder.build()
                .status(true).isLive(false).previewAvailable(false).checkModified(false).noEdit()
                .requestPublication(false).requestDepublication(false).listVersions()
                .listBranches().branch(true).getBranch(false).checkoutBranch(false).removeBranch(false)
                .campaign(false).removeCampaign(false)//.labelVersion(false).removeLabelVersion(false)
                .terminateable(false)
                .saveUnpublished(false)
                .hints());

        // trigger states for 'master'
        wf.hints();
        assertMatchingSCXMLStates(wf.getWorkflowExecutor(), StatesBuilder.build()
                .status().logEvent().editable().noRequest().noPublish().depublishable().noVersioning().noTerminate().copyable()
                .branchable().noCheckoutBranch().noRemoveBranch().noReintegrateBranch().noPublishBranch().canDepublishBranch()
                .states()
        );

        MockNode unpublishedVariant = addVariant(handleNode, HippoStdNodeType.UNPUBLISHED);

        // set (only) author permission
        session.setPermissions(unpublishedVariant.getPath(), "hippo:author", true);

        // unpublished + published
        assertMatchingKeyValues(wf.hints(), HintsBuilder.build()
                .status(true).isLive(true).previewAvailable(true).checkModified(false).noEdit().editable()
                .requestPublication(false).requestDepublication(true).listVersions().retrieveVersion()
                .listBranches().branch(true).getBranch(false).checkoutBranch(false).removeBranch(false)
                .campaign(false).removeCampaign(false)//.labelVersion(true).removeLabelVersion(true)
                .terminateable(false).copy()
                .saveUnpublished(false)
                .hints());

        assertMatchingSCXMLStates(wf.getWorkflowExecutor(), StatesBuilder.build()
                .status().logEvent().editable().noRequest().noPublish().depublishable().versionable().noTerminate().copyable()
                .branchable().noCheckoutBranch().noRemoveBranch().noReintegrateBranch().noPublishBranch().canDepublishBranch()
                .states()
        );

        unpublishedVariant.setProperty(HippoStdPubWfNodeType.HIPPOSTDPUBWF_LAST_MODIFIED_DATE, Calendar.getInstance());
        // unpublished + published + changes
        assertMatchingKeyValues(wf.hints(), HintsBuilder.build()
                .status(true).isLive(true).previewAvailable(true).checkModified(false).noEdit().editable()
                .requestPublication(true).requestDepublication(true).listVersions().retrieveVersion()
                .listBranches().branch(true).getBranch(false).checkoutBranch(false).removeBranch(false)
                .campaign(false).removeCampaign(false)//.labelVersion(true).removeLabelVersion(true)
                .terminateable(false).copy()
                .saveUnpublished(false)
                .hints());

        assertMatchingSCXMLStates(wf.getWorkflowExecutor(), StatesBuilder.build()
                .status().logEvent().editable().noRequest().publishable().depublishable().versionable().noTerminate().copyable()
                .branchable().noCheckoutBranch().noRemoveBranch().noReintegrateBranch().canPublishBranch().canDepublishBranch()
                .states()
        );

        MockNode draftVariant = addVariant(handleNode, HippoStdNodeType.DRAFT);
        // set (only) author permission
        session.setPermissions(draftVariant.getPath(), "hippo:author", true);

        // draft + unpublished + published
        assertMatchingKeyValues(wf.hints(), HintsBuilder.build()
                .status(true).isLive(true).previewAvailable(true).checkModified(true).noEdit().editable()
                .requestPublication(true).requestDepublication(true).listVersions().retrieveVersion()
                .listBranches().branch(true).getBranch(false).checkoutBranch(false).removeBranch(false)
                .campaign(false).removeCampaign(false)//.labelVersion(true).removeLabelVersion(true)
                .terminateable(false).copy()
                .saveUnpublished(false)
                .hints());
        assertMatchingSCXMLStates(wf.getWorkflowExecutor(), StatesBuilder.build()
                .status().logEvent().editable().noRequest().publishable().depublishable().versionable().noTerminate().copyable()
                .branchable().noCheckoutBranch().noRemoveBranch().noReintegrateBranch().canPublishBranch().canDepublishBranch()
                .states()
        );

        publishedVariant.remove();

        draftVariant.setProperty(HippoStdNodeType.HIPPOSTD_TRANSFERABLE, (String) null);
        draftVariant.setProperty(HippoStdNodeType.HIPPOSTD_HOLDER,(String) null);

        // draft + unpublished
        assertMatchingKeyValues(wf.hints(), HintsBuilder.build()
                .status(true).isLive(false).previewAvailable(true).checkModified(true).noEdit().editable()
                .requestPublication(true).requestDepublication(false).listVersions().retrieveVersion()
                .listBranches().branch(true).getBranch(false).checkoutBranch(false).removeBranch(false)
                .campaign(false).removeCampaign(false)//.labelVersion(true).removeLabelVersion(true)
                .terminateable(true).copy()
                .saveUnpublished(false)
                .hints());
        assertMatchingSCXMLStates(wf.getWorkflowExecutor(), StatesBuilder.build()
                .status().logEvent().editable().noRequest().publishable().noDepublish().versionable().terminateable().copyable()
                .branchable().noCheckoutBranch().noRemoveBranch().noReintegrateBranch().canPublishBranch().noDepublishBranch()
                .states()
        );

        session.setPermissions(unpublishedVariant.getPath(), "hippo:editor", false);

        draftVariant.setProperty(HippoStdNodeType.HIPPOSTD_TRANSFERABLE, (String) null);
        draftVariant.setProperty(HippoStdNodeType.HIPPOSTD_HOLDER,(String) null);

        draftVariant.remove();

        // unpublished
        assertMatchingKeyValues(wf.hints(), HintsBuilder.build()
                .status(true).isLive(false).previewAvailable(true).checkModified(false).noEdit().editable()
                .requestPublication(true).requestDepublication(false).listVersions().retrieveVersion()
                .listBranches().branch(true).getBranch(false).checkoutBranch(false).removeBranch(false)
                .campaign(false).removeCampaign(false)//.labelVersion(true).removeLabelVersion(true)
                .terminateable(true).copy()
                .saveUnpublished(false)
                .hints());
        assertMatchingSCXMLStates(wf.getWorkflowExecutor(), StatesBuilder.build()
                .status().logEvent().editable().noRequest().publishable().noDepublish().versionable().terminateable().copyable()
                .branchable().noCheckoutBranch().noRemoveBranch().noReintegrateBranch().canPublishBranch().noDepublishBranch()
                .states()
        );

        unpublishedVariant.remove();
        draftVariant = addVariant(handleNode, HippoStdNodeType.DRAFT);
        // set (only) author permission
        session.setPermissions(draftVariant.getPath(), "hippo:author", true);

        // draft
        assertMatchingKeyValues(wf.hints(), HintsBuilder.build()
                .status(true).isLive(false).previewAvailable(false).checkModified(false).noEdit().editable()
                .requestPublication(false).requestDepublication(false).listVersions()
                .listBranches().branch(false).getBranch(false).checkoutBranch(false).removeBranch(false)
                .campaign(false).removeCampaign(false)//.labelVersion(false).removeLabelVersion(false)
                .terminateable(true)
                .saveUnpublished(false)
                .hints());
        assertMatchingSCXMLStates(wf.getWorkflowExecutor(), StatesBuilder.build()
                .status().logEvent().editable().noRequest().noPublish().noDepublish().noVersioning().terminateable().noCopy()
                .noBranchable().noCheckoutBranch().noRemoveBranch().noReintegrateBranch().noPublishBranch().noDepublishBranch()
                .states()
        );


        // draft transferable ( same user )
        draftVariant.setProperty(HippoStdNodeType.HIPPOSTD_TRANSFERABLE, true);
        draftVariant.setProperty(HippoStdNodeType.HIPPOSTD_HOLDER,"testuser");

        assertMatchingKeyValues(wf.hints(), HintsBuilder.build().copy()
                .status(true).isLive(false).previewAvailable(false).checkModified(false).noEdit()
                .editDraft().obtainEditableInstance(false)
                .requestPublication(false).requestDepublication(false).listVersions()
                .listBranches().branch(false).getBranch(false).checkoutBranch(false).removeBranch(false)
                .campaign(false).removeCampaign(false)//.labelVersion(false).removeLabelVersion(false)
                .terminateable(true)
                .saveUnpublished(false)
                .hints());
        assertMatchingSCXMLStates(wf.getWorkflowExecutor(), StatesBuilder.build()
                .status().editable().logEvent().noRequest().noPublish().noDepublish().noVersioning().terminateable().copyable()
                .noBranchable().noCheckoutBranch().noRemoveBranch().noReintegrateBranch().noPublishBranch().noDepublishBranch()
                .states()
        );

        // draft transferable ( other user )
        draftVariant.setProperty(HippoStdNodeType.HIPPOSTD_HOLDER,"otheruser");
        assertMatchingKeyValues(wf.hints(), HintsBuilder.build()
                .status(true).isLive(false).previewAvailable(false).checkModified(false).noEdit()
                .editDraft().obtainEditableInstance(false)
                .requestPublication(false).requestDepublication(false).listVersions()
                .listBranches().branch(false).getBranch(false).checkoutBranch(false).removeBranch(false)
                .campaign(false).removeCampaign(false)//.labelVersion(false).removeLabelVersion(false)
                .terminateable(true).copy()
                .saveUnpublished(false)
                .hints());
        assertMatchingSCXMLStates(wf.getWorkflowExecutor(), StatesBuilder.build()
                .status().editable().logEvent().noRequest().noPublish().noDepublish().noVersioning().terminateable().copyable()
                .noBranchable().noCheckoutBranch().noRemoveBranch().noReintegrateBranch().noPublishBranch().noDepublishBranch()
                .states()
        );

        draftVariant.setProperty(HippoStdNodeType.HIPPOSTD_TRANSFERABLE, (String) null);
        draftVariant.setProperty(HippoStdNodeType.HIPPOSTD_HOLDER,(String) null);

        publishedVariant = addVariant(handleNode, HippoStdNodeType.PUBLISHED);
        // set (only) author permission
        session.setPermissions(publishedVariant.getPath(), "hippo:author", true);

        // draft + published
        assertMatchingKeyValues(wf.hints(), HintsBuilder.build()
                .status(true).isLive(true).previewAvailable(true).checkModified(false).noEdit().editable()
                .requestPublication(false).requestDepublication(true).listVersions()
                .listBranches().branch(true).getBranch(false).checkoutBranch(false).removeBranch(false)
                .campaign(false).removeCampaign(false)//.labelVersion(false).removeLabelVersion(false)
                .terminateable(false).copy()
                .saveUnpublished(false)
                .hints());
        assertMatchingSCXMLStates(wf.getWorkflowExecutor(), StatesBuilder.build()
                .status().logEvent().editable().noRequest().noPublish().depublishable().noVersioning().noTerminate().copyable()
                .branchable().noCheckoutBranch().noRemoveBranch().noReintegrateBranch().noPublishBranch().canDepublishBranch()
                .states()
        );

        draftVariant.setProperty(HippoStdNodeType.HIPPOSTD_TRANSFERABLE, (String) null);
        draftVariant.setProperty(HippoStdNodeType.HIPPOSTD_HOLDER,(String) null);

        publishedVariant.addMixin(HIPPO_MIXIN_BRANCH_INFO);
        publishedVariant.setProperty(HIPPO_PROPERTY_BRANCH_ID, "foo");

        // depublishBranch for master is false since there is only a master draft and published variant is foo
        assertMatchingKeyValues(wf.hints(), HintsBuilder.build()
                .status(true).isLive(false).previewAvailable(false).checkModified(false).noEdit().editable()
                .requestPublication(false).requestDepublication(false).listVersions()
                .listBranches().branch(true).getBranch(true).checkoutBranch(false).removeBranch(false)
                .campaign(false).removeCampaign(false)//.labelVersion(false).removeLabelVersion(false)
                .terminateable(false).copy()
                .saveUnpublished(false)
                .hints());

        assertMatchingKeyValues(wf.hints("foo"), HintsBuilder.build()
                .status(true).isLive(true).previewAvailable(true).checkModified(false).noEdit().editable()
                .requestPublication(false).requestDepublication(false).listVersions()
                .listBranches().branch(true).getBranch(true).checkoutBranch(false).removeBranch(false)
                .campaign(false).removeCampaign(false)//.labelVersion(false).removeLabelVersion(false)
                .terminateable(false).copy()
                .saveUnpublished(false)
                .hints());

        draftVariant.addMixin(HIPPO_MIXIN_BRANCH_INFO);
        draftVariant.setProperty(HIPPO_PROPERTY_BRANCH_ID, "foo");
        draftVariant.setProperty(HippoStdNodeType.HIPPOSTD_HOLDER, session.getUserID());

        // only branch 'foo' is left so #getBranch for master is disabled
        assertMatchingKeyValues(wf.hints(), HintsBuilder.build()
                .status(true).isLive(false).previewAvailable(false).checkModified(false).noEdit()
                .requestPublication(false).requestDepublication(false).listVersions()
                .listBranches().branch(false).getBranch(false).checkoutBranch(false).removeBranch(false)
                .campaign(false).removeCampaign(false)//.labelVersion(false).removeLabelVersion(false)
                .terminateable(false)
                .saveUnpublished(false)
                .hints());

        assertMatchingKeyValues(wf.hints("foo"), HintsBuilder.build()
                .status(true).isLive(true).previewAvailable(true).checkModified(false).noEdit().editable()
                .requestPublication(false).requestDepublication(false).listVersions()
                .listBranches().branch(false).getBranch(true).checkoutBranch(false).removeBranch(false)
                .campaign(false).removeCampaign(false)//.labelVersion(false).removeLabelVersion(false)
                .terminateable(false).copy()
                .saveUnpublished(false)
                .hints());

        draftVariant.remove();

        unpublishedVariant = addVariant(handleNode, HippoStdNodeType.UNPUBLISHED);
        // set (only) author permission
        session.setPermissions(unpublishedVariant.getPath(), "hippo:author", true);

        // after this, branch should be disallowed because we only allow branching from Master
        unpublishedVariant.addMixin(HIPPO_MIXIN_BRANCH_INFO);
        unpublishedVariant.setProperty(HIPPO_PROPERTY_BRANCH_ID, "foo");

        assertMatchingKeyValues(wf.hints(), HintsBuilder.build()
                .status(true).isLive(false).previewAvailable(false).checkModified(false).noEdit()
                .requestPublication(false).requestDepublication(false).listVersions().retrieveVersion()
                .listBranches().branch(false).getBranch(false).checkoutBranch(false).removeBranch(false)
                .campaign(false).removeCampaign(false)//.labelVersion(true).removeLabelVersion(true)
                .terminateable(false)
                .saveUnpublished(false)
                .hints());

        assertMatchingSCXMLStates(wf.getWorkflowExecutor(), StatesBuilder.build()
                .status().logEvent().editable().noRequest().noPublish().noDepublish().versionable().noTerminate().noCopy()
                .noBranchable().noCheckoutBranch().noRemoveBranch().noReintegrateBranch().noPublishBranch().noDepublishBranch()
                .states()
        );

        assertMatchingKeyValues(wf.hints("foo"), HintsBuilder.build()
                .status(true).isLive(true).previewAvailable(true).checkModified(false).noEdit().editable()
                .requestPublication(false).requestDepublication(false).listVersions().retrieveVersion()
                .listBranches().branch(false).getBranch(true).checkoutBranch(true).removeBranch(false)
                .campaign(false).removeCampaign(false)//.labelVersion(true).removeLabelVersion(true)
                .terminateable(false).copy()
                .saveUnpublished(false)
                .hints());

        assertMatchingSCXMLStates(wf.getWorkflowExecutor(), StatesBuilder.build()
                .status().logEvent().editable().noRequest().noDepublish().noPublish().versionable().noTerminate().copyable()
                .noBranchable().canCheckoutBranch().noRemoveBranch().canReintegrateBranch().noPublishBranch().canDepublishBranch()
                .states()
        );

        assertMatchingKeyValues(wf.hints(), HintsBuilder.build()
                .status(true).isLive(false).previewAvailable(false).checkModified(false).noEdit()
                .requestPublication(false).requestDepublication(false).listVersions().retrieveVersion()
                .listBranches().branch(false).getBranch(false).checkoutBranch(false).removeBranch(false)
                .campaign(false).removeCampaign(false)//.labelVersion(true).removeLabelVersion(true)
                .terminateable(false)
                .saveUnpublished(false)
                .hints());

        assertMatchingSCXMLStates(wf.getWorkflowExecutor(), StatesBuilder.build()
                .status().logEvent().editable().noRequest().noPublish().noDepublish().versionable().noTerminate().noCopy()
                .noBranchable().noCheckoutBranch().noRemoveBranch().noReintegrateBranch().noPublishBranch().noDepublishBranch()
                .states()
        );

        assertMatchingKeyValues(wf.hints("foo"), HintsBuilder.build()
                .status(true).isLive(true).previewAvailable(true).checkModified(false).noEdit().editable()
                .requestPublication(false).requestDepublication(false).listVersions().retrieveVersion()
                .listBranches().branch(false).getBranch(true).checkoutBranch(true).removeBranch(false)
                .campaign(false).removeCampaign(false)//.labelVersion(true).removeLabelVersion(true)
                .terminateable(false).copy()
                .saveUnpublished(false)
                .hints());

        assertMatchingSCXMLStates(wf.getWorkflowExecutor(), StatesBuilder.build()
                .status().logEvent().editable().noRequest().noDepublish().noPublish().versionable().noTerminate().copyable()
                .noBranchable().canCheckoutBranch().noRemoveBranch().canReintegrateBranch().noPublishBranch().canDepublishBranch()
                .states()
        );

        // make 'live' for master again
        publishedVariant.getProperty(HIPPO_PROPERTY_BRANCH_ID).remove();
        publishedVariant.removeMixin(HIPPO_MIXIN_BRANCH_INFO);

        assertMatchingKeyValues(wf.hints("foo"), HintsBuilder.build()
                .status(true).isLive(false).previewAvailable(true).checkModified(false).noEdit().editable()
                .requestPublication(false).requestDepublication(false).listVersions().retrieveVersion()
                .listBranches().branch(true).getBranch(true).checkoutBranch(true).removeBranch(true)
                .campaign(false).removeCampaign(false)//.labelVersion(true).removeLabelVersion(true)
                .terminateable(false).copy()
                .saveUnpublished(false)
                .hints());

        assertMatchingSCXMLStates(wf.getWorkflowExecutor(), StatesBuilder.build()
                .status().logEvent().editable().noRequest().noDepublish().noPublish().versionable().noTerminate().copyable()
                .branchable().canCheckoutBranch().canRemoveBranch().canReintegrateBranch().canPublishBranch().noDepublishBranch()
                .states()
        );
    }


    @Test
    public void testStatusState() throws Exception {

        MockAccessManagedSession session = new MockAccessManagedSession(MockNode.root());
        MockWorkflowContext workflowContext = new MockWorkflowContext("testuser", session);
        MockNode handleNode = session.getRootNode().addNode("test", HippoNodeType.NT_HANDLE);
        DocumentWorkflowImpl wf = getDocumentWorkflowImpl();
        wf.setWorkflowContext(workflowContext);
        wf.setNode(handleNode);

        MockNode draftVariant = addVariant(handleNode, HippoStdNodeType.DRAFT);
        // set (only) author permission
        session.setPermissions(draftVariant.getPath(), "hippo:author", true);

        // status=true (no holder)
        assertMatchingKeyValues(wf.hints(), HintsBuilder.build()
                .status(true).isLive(false).previewAvailable(false).checkModified(false).noEdit().editable()
                .requestPublication(false).requestDepublication(false).listVersions()
                .listBranches().branch(false).getBranch(false).checkoutBranch(false).removeBranch(false)
                .campaign(false).removeCampaign(false)//.labelVersion(false).removeLabelVersion(false)
                .terminateable(true)
                .saveUnpublished(false)
                .hints());

        draftVariant.setProperty(HippoStdNodeType.HIPPOSTD_HOLDER, "testuser");

        // status=true (holder == editor)
        assertMatchingKeyValues(wf.hints(), HintsBuilder.build()
                .status(true).isLive(false).previewAvailable(false).checkModified(false).editing().saveDraft()
                .requestPublication(false).requestDepublication(false).listVersions()
                .listBranches().branch(false).getBranch(false).checkoutBranch(false).removeBranch(false)
                .campaign(false).removeCampaign(false)//.labelVersion(false).removeLabelVersion(false)
                .terminateable(false)
                .saveUnpublished(false)
                .hints());

        draftVariant.setProperty(HippoStdNodeType.HIPPOSTD_HOLDER, "otheruser");

        // status=false (holder != editor, inUseBy=otheruser)
        assertMatchingKeyValues(wf.hints(), HintsBuilder.build()
                .status(false).isLive(false).previewAvailable(false).checkModified(false).noEdit().inUseBy("otheruser")
                .requestPublication(false).requestDepublication(false).listVersions()
                .listBranches().branch(false).getBranch(false).checkoutBranch(false).removeBranch(false)
                .campaign(false).removeCampaign(false)//.labelVersion(false).removeLabelVersion(false)
                .terminateable(false)
                .saveUnpublished(false)
                .hints());

        MockNode publishedVariant = addVariant(handleNode, HippoStdNodeType.PUBLISHED);
        // set (only) author permission
        session.setPermissions(publishedVariant.getPath(), "hippo:author", true);

        // no/empty availability published: live + preview
        assertMatchingKeyValues(wf.hints(), HintsBuilder.build()
                .status(false).isLive(true).previewAvailable(true).checkModified(false).noEdit().inUseBy("otheruser")
                .requestPublication(false).requestDepublication(false).listVersions()
                .listBranches().branch(false).getBranch(false).checkoutBranch(false).removeBranch(false)
                .campaign(false).removeCampaign(false)//.labelVersion(false).removeLabelVersion(false)
                .terminateable(false).copy()
                .saveUnpublished(false)
                .hints());

        publishedVariant.getProperty(HIPPO_AVAILABILITY).remove();

        // no live|preview availability on published
        assertMatchingKeyValues(wf.hints(), HintsBuilder.build()
                .status(false).isLive(false).previewAvailable(false).checkModified(false).noEdit().inUseBy("otheruser")
                .requestPublication(false).requestDepublication(false).listVersions()
                .listBranches().branch(false).getBranch(false).checkoutBranch(false).removeBranch(false)
                .campaign(false).removeCampaign(false)//.labelVersion(false).removeLabelVersion(false)
                .terminateable(false).copy()
                .saveUnpublished(false)
                .hints());

        publishedVariant.setProperty(HIPPO_AVAILABILITY, new String[]{"live"});

        // live availability on published
        assertMatchingKeyValues(wf.hints(), HintsBuilder.build()
                .status(false).isLive(true).previewAvailable(true).checkModified(false).noEdit().inUseBy("otheruser")
                .requestPublication(false).requestDepublication(false).listVersions()
                .listBranches().branch(false).getBranch(false).checkoutBranch(false).removeBranch(false)
                .campaign(false).removeCampaign(false)//.labelVersion(false).removeLabelVersion(false)
                .terminateable(false).copy()
                .saveUnpublished(false)
                .hints());

        publishedVariant.setProperty(HIPPO_AVAILABILITY, new String[]{"preview", "live"});

        // live|preview availability on published
        assertMatchingKeyValues(wf.hints(), HintsBuilder.build()
                .status(false).isLive(true).previewAvailable(true).checkModified(false).noEdit().inUseBy("otheruser")
                .requestPublication(false).requestDepublication(false).listVersions()
                .listBranches().branch(false).getBranch(false).checkoutBranch(false).removeBranch(false)
                .campaign(false).removeCampaign(false)//.labelVersion(false).removeLabelVersion(false)
                .terminateable(false).copy()
                .saveUnpublished(false)
                .hints());

        MockNode unpublishedVariant = addVariant(handleNode, HippoStdNodeType.UNPUBLISHED);
        // set (only) author permission
        session.setPermissions(unpublishedVariant.getPath(), "hippo:author", true);
        unpublishedVariant.setProperty(HIPPO_AVAILABILITY, new String[]{"preview"});
        publishedVariant.setProperty(HIPPO_AVAILABILITY, new String[]{"live"});

        // live availability on published, preview availability on unpublished
        assertMatchingKeyValues(wf.hints(), HintsBuilder.build()
                .status(false).isLive(true).previewAvailable(true).checkModified(true).noEdit().inUseBy("otheruser")
                .requestPublication(false).requestDepublication(false).listVersions().retrieveVersion()
                .listBranches().branch(false).getBranch(false).checkoutBranch(false).removeBranch(false)
                .campaign(false).removeCampaign(false)//.labelVersion(true).removeLabelVersion(true)
                .terminateable(false).copy()
                .saveUnpublished(false)
                .hints());

        publishedVariant.remove();

        // only preview availability on unpublished
        assertMatchingKeyValues(wf.hints(), HintsBuilder.build()
                .status(false).isLive(false).previewAvailable(true).checkModified(true).noEdit().inUseBy("otheruser")
                .requestPublication(false).requestDepublication(false).listVersions().retrieveVersion()
                .listBranches().branch(false).getBranch(false).checkoutBranch(false).removeBranch(false)
                .campaign(false).removeCampaign(false)//.labelVersion(true).removeLabelVersion(true)
                .terminateable(false).copy()
                .saveUnpublished(false)
                .hints());

        draftVariant.remove();

        // only preview present, no more editing
        assertMatchingKeyValues(wf.hints(), HintsBuilder.build()
                .status(true).isLive(false).previewAvailable(true).checkModified(false).noEdit().editable()
                .requestPublication(true).requestDepublication(false).listVersions().retrieveVersion()
                .listBranches().branch(true).getBranch(false).checkoutBranch(false).removeBranch(false)
                .campaign(false).removeCampaign(false)//.labelVersion(true).removeLabelVersion(true)
                .terminateable(true).copy()
                .saveUnpublished(false)
                .hints());
    }

    @Test
    public void testEditState() throws Exception {

        MockAccessManagedSession session = new MockAccessManagedSession(MockNode.root());
        MockWorkflowContext workflowContext = new MockWorkflowContext("testuser", session);
        MockNode handleNode = session.getRootNode().addNode("test", HippoNodeType.NT_HANDLE);
        DocumentWorkflowImpl wf = getDocumentWorkflowImpl();
        wf.setWorkflowContext(workflowContext);
        wf.setNode(handleNode);

        MockNode draftVariant = addVariant(handleNode, HippoStdNodeType.DRAFT);
        // set (only) author permission
        session.setPermissions(draftVariant.getPath(), "hippo:author", true);

        // not editing, no request pending: editable
        assertMatchingKeyValues(wf.hints(), HintsBuilder.build()
                .status(true).isLive(false).previewAvailable(false).checkModified(false).noEdit().editable()
                .requestPublication(false).requestDepublication(false).listVersions()
                .listBranches().branch(false).getBranch(false).checkoutBranch(false).removeBranch(false)
                .campaign(false).removeCampaign(false)//.labelVersion(false).removeLabelVersion(false)
                .terminateable(true)
                .saveUnpublished(false)
                .hints());
        assertMatchingSCXMLStates(wf.getWorkflowExecutor(), StatesBuilder.build()
                .status().logEvent().editable().noRequest().noPublish().noDepublish().noVersioning().terminateable().noCopy()
                .noBranchable().noCheckoutBranch().noRemoveBranch().noReintegrateBranch().noPublishBranch().noDepublishBranch()
                .states()
        );

        draftVariant.setProperty(HippoStdNodeType.HIPPOSTD_HOLDER, "otheruser");

        // editing, no request pending: editing
        assertMatchingKeyValues(wf.hints(), HintsBuilder.build()
                .status(false).isLive(false).previewAvailable(false).checkModified(false).noEdit().inUseBy("otheruser")
                .requestPublication(false).requestDepublication(false).listVersions()
                .listBranches().branch(false).getBranch(false).checkoutBranch(false).removeBranch(false)
                .campaign(false).removeCampaign(false)//.labelVersion(false).removeLabelVersion(false)
                .terminateable(false)
                .saveUnpublished(false)
                .hints());
        assertMatchingSCXMLStates(wf.getWorkflowExecutor(), StatesBuilder.build()
                .status().logEvent().editing().noRequest().noPublish().noDepublish().noVersioning().noTerminate().noCopy()
                .noBranchable().noCheckoutBranch().noRemoveBranch().noReintegrateBranch().noPublishBranch().noDepublishBranch()
                .states()
        );

        session.setPermissions(draftVariant.getPath(), "hippo:admin", true);

        // editing, no request pending, admin: editing, unlock
        assertMatchingKeyValues(wf.hints(), HintsBuilder.build()
                .status(false).isLive(false).previewAvailable(false).checkModified(false).noEdit().inUseBy("otheruser").unlock(true)
                .requestPublication(false).requestDepublication(false).listVersions()
                .listBranches().branch(false).getBranch(false).checkoutBranch(false).removeBranch(false)
                .campaign(false).removeCampaign(false)//.labelVersion(false).removeLabelVersion(false)
                .saveUnpublished(false)
                .hints());
        assertMatchingSCXMLStates(wf.getWorkflowExecutor(), StatesBuilder.build()
                .status().logEvent().editing().noRequest().noPublish().noDepublish().noVersioning().noTerminate().noCopy()
                .noBranchable().noCheckoutBranch().noRemoveBranch().noReintegrateBranch().noPublishBranch().noDepublishBranch()
                .states()
        );

        draftVariant.setProperty(HippoStdNodeType.HIPPOSTD_HOLDER, "testuser");

        // editor, no request pending (, admin): editing, edit
        assertMatchingKeyValues(wf.hints(), HintsBuilder.build()
                .status(true).isLive(false).previewAvailable(false).checkModified(false).editing().saveDraft()
                .requestPublication(false).requestDepublication(false).listVersions()
                .listBranches().branch(false).getBranch(false).checkoutBranch(false).removeBranch(false)
                .campaign(false).removeCampaign(false)//.labelVersion(false).removeLabelVersion(false)
                .saveUnpublished(false)
                .hints());
        assertMatchingSCXMLStates(wf.getWorkflowExecutor(), StatesBuilder.build()
                .status().logEvent().editing().noRequest().noPublish().noDepublish().noVersioning().noTerminate().noCopy()
                .noBranchable().noCheckoutBranch().noRemoveBranch().noReintegrateBranch().noPublishBranch().noDepublishBranch()
                .states()
        );

        draftVariant.getProperty(HippoStdNodeType.HIPPOSTD_HOLDER).remove();
        MockNode rejectedRequest = addRequest(handleNode, HippoStdPubWfNodeType.REJECTED, true);

        // not editing, request rejected, admin: editable, unlock=false
        assertMatchingKeyValues(wf.hints(), HintsBuilder.build()
                .status(true).isLive(false).previewAvailable(false).checkModified(false).noEdit().editable().unlock(false)
                .cancelRequest(rejectedRequest.getIdentifier())
                .infoRequest(rejectedRequest.getIdentifier())
                .requestPublication(false).requestDepublication(false).listVersions()
                .listBranches().branch(false).getBranch(false).checkoutBranch(false).removeBranch(false)
                .campaign(false).removeCampaign(false)//.labelVersion(false).removeLabelVersion(false)
                .saveUnpublished(false)
                .hints());
        assertMatchingSCXMLStates(wf.getWorkflowExecutor(), StatesBuilder.build()
                .status().logEvent().editable().requested().noPublish().noDepublish().noVersioning().terminateable().noCopy()
                .noBranchable().noCheckoutBranch().noRemoveBranch().noReintegrateBranch().noPublishBranch().noDepublishBranch()
                .states()
        );

        session.setPermissions(draftVariant.getPath(), "hippo:author", true);

        MockNode publishRequest = addRequest(handleNode, HippoStdPubWfNodeType.PUBLISH, true);

        // not editing, publish request: no-edit
        assertMatchingKeyValues(wf.hints(), HintsBuilder.build()
                .status(true).isLive(false).previewAvailable(false).checkModified(false).noEdit()
                .cancelRequest(rejectedRequest.getIdentifier())
                .infoRequest(rejectedRequest.getIdentifier())
                .acceptRequest(publishRequest.getIdentifier(), false)
                .cancelRequest(publishRequest.getIdentifier())
                .rejectRequest(publishRequest.getIdentifier())
                .infoRequest(publishRequest.getIdentifier())
                .requestPublication(false).requestDepublication(false).listVersions()
                .listBranches().branch(false).getBranch(false).checkoutBranch(false).removeBranch(false)
                .campaign(false).removeCampaign(false)//.labelVersion(false).removeLabelVersion(false)
                .terminateable(false)
                .saveUnpublished(false)
                .hints());
        assertMatchingSCXMLStates(wf.getWorkflowExecutor(), StatesBuilder.build()
                .status().logEvent().noEdit().requested().noPublish().noDepublish().noVersioning().terminateable().noCopy()
                .noBranchable().noCheckoutBranch().noRemoveBranch().noReintegrateBranch().noPublishBranch().noDepublishBranch()
                .states()
        );
    }

    @Test
    public void testRequestState() throws Exception {
        MockAccessManagedSession session = new MockAccessManagedSession(MockNode.root());
        MockWorkflowContext workflowContext = new MockWorkflowContext("testuser", session);
        MockNode handleNode = session.getRootNode().addNode("test", HippoNodeType.NT_HANDLE);
        DocumentWorkflowImpl wf = getDocumentWorkflowImpl();
        wf.setWorkflowContext(workflowContext);
        wf.setNode(handleNode);

        // test state not-requested
        MockNode unpublishedVariant = addVariant(handleNode, HippoStdNodeType.UNPUBLISHED);
        session.setPermissions(unpublishedVariant.getPath(), "hippo:author", true);

        // no-request
        assertMatchingKeyValues(wf.hints(), HintsBuilder.build()
                .status(true).isLive(false).previewAvailable(true).checkModified(false).noEdit().editable()
                .requestPublication(true).requestDepublication(false).listVersions().retrieveVersion()
                .listBranches().branch(true).getBranch(false).checkoutBranch(false).removeBranch(false)
                .campaign(false).removeCampaign(false)//.labelVersion(true).removeLabelVersion(true)
                .terminateable(true).copy()
                .saveUnpublished(false)
                .hints());
        assertMatchingSCXMLStates(wf.getWorkflowExecutor(), StatesBuilder.build()
                .status().logEvent().editable().noRequest().publishable().noDepublish().versionable().terminateable().copyable()
                .branchable().noCheckoutBranch().noRemoveBranch().noReintegrateBranch().canPublishBranch().noDepublishBranch()
                .states()
        );

        MockNode publishRequest = addRequest(handleNode, HippoStdPubWfNodeType.PUBLISH, true);
        publishRequest.setProperty(HippoStdPubWfNodeType.HIPPOSTDPUBWF_USERNAME, "testuser");
        session.setPermissions(publishRequest.getPath(), "hippo:author", true);

        // author user publish request: cancelRequest=true
        assertMatchingKeyValues(wf.hints(), HintsBuilder.build()
                .status(true).isLive(false).previewAvailable(true).checkModified(false).noEdit()
                .cancelRequest(publishRequest.getIdentifier())
                .infoRequest(publishRequest.getIdentifier())
                .requestPublication(false).requestDepublication(false).listVersions().retrieveVersion()
                .listBranches().branch(true).getBranch(false).checkoutBranch(false).removeBranch(false)
                .campaign(false).removeCampaign(false)//.labelVersion(true).removeLabelVersion(true)
                .terminateable(false).copy()
                .saveUnpublished(false)
                .hints());
        assertMatchingSCXMLStates(wf.getWorkflowExecutor(), StatesBuilder.build()
                .status().logEvent().noEdit().requested().publishable().noDepublish().versionable().terminateable().copyable()
                .branchable().noCheckoutBranch().noRemoveBranch().noReintegrateBranch().canPublishBranch().noDepublishBranch()
                .states()
        );

        publishRequest.setProperty(HippoStdPubWfNodeType.HIPPOSTDPUBWF_USERNAME, "otheruser");

        // author and other user publish request: no-op
        assertMatchingKeyValues(wf.hints(), HintsBuilder.build()
                .status(true).isLive(false).previewAvailable(true).checkModified(false).noEdit()
                .requestPublication(false).requestDepublication(false)
                .infoRequest(publishRequest.getIdentifier())
                .listVersions().retrieveVersion()
                .listBranches().branch(true).getBranch(false).checkoutBranch(false).removeBranch(false)
                .campaign(false).removeCampaign(false)//.labelVersion(true).removeLabelVersion(true)
                .terminateable(false).copy()
                .saveUnpublished(false)
                .hints());
        assertMatchingSCXMLStates(wf.getWorkflowExecutor(), StatesBuilder.build()
                .status().logEvent().noEdit().requested().publishable().noDepublish().versionable().terminateable().copyable()
                .branchable().noCheckoutBranch().noRemoveBranch().noReintegrateBranch().canPublishBranch().noDepublishBranch()
                .states()
        );

        session.setPermissions(publishRequest.getPath(), "hippo:editor", true);

        // editor other user publish request: acceptRequest=true,rejectRequest=true
        assertMatchingKeyValues(wf.hints(), HintsBuilder.build()
                .status(true).isLive(false).previewAvailable(true).checkModified(false).noEdit()
                .acceptRequest(publishRequest.getIdentifier(), true)
                .rejectRequest(publishRequest.getIdentifier())
                .infoRequest(publishRequest.getIdentifier())
                .requestPublication(false).requestDepublication(false).listVersions().retrieveVersion()
                .listBranches().branch(true).getBranch(false).checkoutBranch(false).removeBranch(false)
                .campaign(false).removeCampaign(false)//.labelVersion(true).removeLabelVersion(true)
                .terminateable(false).copy()
                .saveUnpublished(false)
                .hints());
        assertMatchingSCXMLStates(wf.getWorkflowExecutor(), StatesBuilder.build()
                .status().logEvent().noEdit().requested().publishable().noDepublish().versionable().terminateable().copyable()
                .branchable().noCheckoutBranch().noRemoveBranch().noReintegrateBranch().canPublishBranch().noDepublishBranch()
                .states()
        );

        // make the unpublished also editor : now we expect (de)publishBranch enabled but false since there is a request
        session.setPermissions(unpublishedVariant.getPath(), "hippo:editor", true);
        assertMatchingKeyValues(wf.hints(), HintsBuilder.build()
                .status(true).isLive(false).previewAvailable(true).checkModified(false).noEdit().versionable()
                .acceptRequest(publishRequest.getIdentifier(), true)
                .rejectRequest(publishRequest.getIdentifier())
                .infoRequest(publishRequest.getIdentifier())
                .requestPublication(false).requestDepublication(false).listVersions().retrieveVersion()
                .listBranches().branch(true).getBranch(false).checkoutBranch(false).removeBranch(false)
                .publish(false).depublish(false).publishBranch(false).depublishBranch(false).reintegrateBranch(false)
                .campaign(true).removeCampaign(true)//.labelVersion(true).removeLabelVersion(true)
                .saveUnpublished(false)
                .hints());
        assertMatchingSCXMLStates(wf.getWorkflowExecutor(), StatesBuilder.build()
                .status().logEvent().noEdit().requested().publishable().noDepublish().versionable().terminateable().noCopy()
                .branchable().noCheckoutBranch().noRemoveBranch().noReintegrateBranch().canPublishBranch().noDepublishBranch()
                .states()
        );

        // make the unpublished for branch 'foo' : Now despite the request, (de)publishBranch should be enabled

        unpublishedVariant.addMixin(HIPPO_MIXIN_BRANCH_INFO);
        unpublishedVariant.setProperty(HIPPO_PROPERTY_BRANCH_ID, "foo");
        assertMatchingKeyValues(wf.hints("foo"), HintsBuilder.build()
                .status(true).isLive(false).previewAvailable(true).checkModified(false)
                .noEdit().editable().versionable()
                .acceptRequest(publishRequest.getIdentifier(), true)
                .rejectRequest(publishRequest.getIdentifier())
                .infoRequest(publishRequest.getIdentifier())
                .requestPublication(false).requestDepublication(false).listVersions().retrieveVersion()
                .listBranches().branch(false).getBranch(true).checkoutBranch(true).removeBranch(true)
                .campaign(true).removeCampaign(true)//.labelVersion(true).removeLabelVersion(true)
                .publish(false).depublish(false).publishBranch(true).depublishBranch(false).reintegrateBranch(true)
                .saveUnpublished(false)
                .hints());
        assertMatchingSCXMLStates(wf.getWorkflowExecutor(), StatesBuilder.build()
                .status().logEvent().editable().requested().noPublish().noDepublish().versionable().terminateable().noCopy()
                .noBranchable().canCheckoutBranch().canRemoveBranch().canReintegrateBranch().canPublishBranch().noDepublishBranch()
                .states()
        );

        // reset to author
        session.setPermissions(unpublishedVariant.getPath(), "hippo:author", true);
        // reset to 'master'
        unpublishedVariant.getProperty(HIPPO_PROPERTY_BRANCH_ID).remove();
        unpublishedVariant.removeMixin(HIPPO_MIXIN_BRANCH_INFO);

        MockNode publishedVariant = addVariant(handleNode, HippoStdNodeType.PUBLISHED);
        session.setPermissions(publishedVariant.getPath(), "hippo:author", true);

        // editor other user publish request but unmodified: acceptRequest=false,rejectRequest=true
        assertMatchingKeyValues(wf.hints(), HintsBuilder.build()
                .status(true).isLive(true).previewAvailable(true).checkModified(false).noEdit()
                .acceptRequest(publishRequest.getIdentifier(), false)
                .rejectRequest(publishRequest.getIdentifier())
                .infoRequest(publishRequest.getIdentifier())
                .requestPublication(false).requestDepublication(false).listVersions().retrieveVersion()
                .listBranches().branch(true).getBranch(false).checkoutBranch(false).removeBranch(false)
                .campaign(false).removeCampaign(false)//.labelVersion(true).removeLabelVersion(true)
                .terminateable(false).copy()
                .saveUnpublished(false)
                .hints());
        assertMatchingSCXMLStates(wf.getWorkflowExecutor(), StatesBuilder.build()
                .status().logEvent().noEdit().requested().noPublish().depublishable().versionable().noTerminate().copyable()
                .branchable().noCheckoutBranch().noRemoveBranch().noReintegrateBranch().noPublishBranch().canDepublishBranch()
                .states()
        );

        publishRequest.setProperty(HippoStdPubWfNodeType.HIPPOSTDPUBWF_USERNAME, "testuser");

        // editor own publish request but unmodified: acceptRequest=false,rejectRequest=true,cancelRequest=true
        assertMatchingKeyValues(wf.hints(), HintsBuilder.build()
                .status(true).isLive(true).previewAvailable(true).checkModified(false).noEdit()
                .acceptRequest(publishRequest.getIdentifier(), false)
                .rejectRequest(publishRequest.getIdentifier())
                .cancelRequest(publishRequest.getIdentifier())
                .infoRequest(publishRequest.getIdentifier())
                .requestPublication(false).requestDepublication(false).listVersions().retrieveVersion()
                .listBranches().branch(true).getBranch(false).checkoutBranch(false).removeBranch(false)
                .campaign(false).removeCampaign(false)//.labelVersion(true).removeLabelVersion(true)
                .terminateable(false).copy()
                .saveUnpublished(false)
                .hints());
        assertMatchingSCXMLStates(wf.getWorkflowExecutor(), StatesBuilder.build()
                .status().logEvent().noEdit().requested().noPublish().depublishable().versionable().noTerminate().copyable()
                .branchable().noCheckoutBranch().noRemoveBranch().noReintegrateBranch().noPublishBranch().canDepublishBranch()
                .states()
        );

        publishRequest.remove();

        MockNode depublishRequest = addRequest(handleNode, HippoStdPubWfNodeType.DEPUBLISH, true);
        depublishRequest.setProperty(HippoStdPubWfNodeType.HIPPOSTDPUBWF_USERNAME, "testuser");
        session.setPermissions(depublishRequest.getPath(), "hippo:editor", true);

        // editor own depublish request live: acceptRequest=true,rejectRequest=true,cancelRequest=true
        assertMatchingKeyValues(wf.hints(), HintsBuilder.build()
                .status(true).isLive(true).previewAvailable(true).checkModified(false).noEdit()
                .acceptRequest(depublishRequest.getIdentifier(), true)
                .rejectRequest(depublishRequest.getIdentifier())
                .cancelRequest(depublishRequest.getIdentifier())
                .infoRequest(depublishRequest.getIdentifier())
                .requestPublication(false).requestDepublication(false).listVersions().retrieveVersion()
                .listBranches().branch(true).getBranch(false).checkoutBranch(false).removeBranch(false)
                .campaign(false).removeCampaign(false)//.labelVersion(true).removeLabelVersion(true)
                .terminateable(false).copy()
                .saveUnpublished(false)
                .hints());
        assertMatchingSCXMLStates(wf.getWorkflowExecutor(), StatesBuilder.build()
                .status().logEvent().noEdit().requested().noPublish().depublishable().versionable().noTerminate().copyable()
                .branchable().noCheckoutBranch().noRemoveBranch().noReintegrateBranch().noPublishBranch().canDepublishBranch()
                .states()
        );

        publishedVariant.setProperty(HIPPO_AVAILABILITY, new String[]{"preview"});

        // editor own depublish request !live: acceptRequest=false,rejectRequest=true,cancelRequest=true
        // current user is author who cannot terminate
        assertMatchingKeyValues(wf.hints(), HintsBuilder.build()
                .status(true).isLive(false).previewAvailable(true).checkModified(false).noEdit()
                .acceptRequest(depublishRequest.getIdentifier(), false)
                .rejectRequest(depublishRequest.getIdentifier())
                .cancelRequest(depublishRequest.getIdentifier())
                .infoRequest(depublishRequest.getIdentifier())
                .requestPublication(false).requestDepublication(false).listVersions().retrieveVersion()
                .listBranches().branch(true).getBranch(false).checkoutBranch(false).removeBranch(false)
                .campaign(false).removeCampaign(false)//.labelVersion(true).removeLabelVersion(true)
                .terminateable(false).copy()
                .saveUnpublished(false)
                .hints());
        assertMatchingSCXMLStates(wf.getWorkflowExecutor(), StatesBuilder.build()
                .status().logEvent().noEdit().requested().publishable().noDepublish().versionable().terminateable().copyable()
                .branchable().noCheckoutBranch().noRemoveBranch().noReintegrateBranch().canPublishBranch().noDepublishBranch()
                .states()
        );

        depublishRequest.remove();

        MockNode deleteRequest = addRequest(handleNode, HippoStdPubWfNodeType.DELETE, true);
        deleteRequest.setProperty(HippoStdPubWfNodeType.HIPPOSTDPUBWF_USERNAME, "testuser");
        session.setPermissions(deleteRequest.getPath(), "hippo:editor", true);

        // editor own delete request !live: acceptRequest=true,rejectRequest=true,cancelRequest=true
        assertMatchingKeyValues(wf.hints(), HintsBuilder.build()
                .status(true).isLive(false).previewAvailable(true).checkModified(false).noEdit()
                .acceptRequest(deleteRequest.getIdentifier(), true)
                .rejectRequest(deleteRequest.getIdentifier())
                .cancelRequest(deleteRequest.getIdentifier())
                .infoRequest(deleteRequest.getIdentifier())
                .requestPublication(false).requestDepublication(false).listVersions().retrieveVersion()
                .listBranches().branch(true).getBranch(false).checkoutBranch(false).removeBranch(false)
                .campaign(false).removeCampaign(false)//.labelVersion(true).removeLabelVersion(true)
                .terminateable(false).copy()
                .saveUnpublished(false)
                .hints());
        assertMatchingSCXMLStates(wf.getWorkflowExecutor(), StatesBuilder.build()
                .status().logEvent().noEdit().requested().publishable().noDepublish().versionable().terminateable().copyable()
                .branchable().noCheckoutBranch().noRemoveBranch().noReintegrateBranch().canPublishBranch().noDepublishBranch()
                .states()
        );

        publishedVariant.setProperty(HIPPO_AVAILABILITY, new String[]{"preview", "live"});

        // editor own delete request !live: acceptRequest=false,rejectRequest=true,cancelRequest=true
        assertMatchingKeyValues(wf.hints(), HintsBuilder.build()
                .status(true).isLive(true).previewAvailable(true).checkModified(false).noEdit()
                .acceptRequest(deleteRequest.getIdentifier(), false)
                .rejectRequest(deleteRequest.getIdentifier())
                .cancelRequest(deleteRequest.getIdentifier())
                .infoRequest(deleteRequest.getIdentifier())
                .requestPublication(false).requestDepublication(false).listVersions().retrieveVersion()
                .listBranches().branch(true).getBranch(false).checkoutBranch(false).removeBranch(false)
                .campaign(false).removeCampaign(false)//.labelVersion(true).removeLabelVersion(true)
                .terminateable(false).copy()
                .saveUnpublished(false)
                .hints());
        assertMatchingSCXMLStates(wf.getWorkflowExecutor(), StatesBuilder.build()
                .status().logEvent().noEdit().requested().noPublish().depublishable().versionable().noTerminate().copyable()
                .branchable().noCheckoutBranch().noRemoveBranch().noReintegrateBranch().noPublishBranch().canDepublishBranch()
                .states()
        );

        deleteRequest.remove();
        publishedVariant.remove();

        MockNode rejectedRequest = addRequest(handleNode, HippoStdPubWfNodeType.REJECTED, true);
        rejectedRequest.setProperty(HippoStdPubWfNodeType.HIPPOSTDPUBWF_USERNAME, "otheruser");
        session.setPermissions(rejectedRequest.getPath(), "hippo:editor", true);

        // editor other user rejected request: no-op
        assertMatchingKeyValues(wf.hints(), HintsBuilder.build()
                .status(true).isLive(false).previewAvailable(true).checkModified(false).noEdit().editable()
                .infoRequest(rejectedRequest.getIdentifier())
                .requestPublication(true).requestDepublication(false).listVersions().retrieveVersion()
                .listBranches().branch(true).getBranch(false).checkoutBranch(false).removeBranch(false)
                .campaign(false).removeCampaign(false)//.labelVersion(true).removeLabelVersion(true)
                .terminateable(true).copy()
                .saveUnpublished(false)
                .hints());
        assertMatchingSCXMLStates(wf.getWorkflowExecutor(), StatesBuilder.build()
                .status().logEvent().editable().requested().publishable().noDepublish().versionable().terminateable().copyable()
                .branchable().noCheckoutBranch().noRemoveBranch().noReintegrateBranch().canPublishBranch().noDepublishBranch()
                .states()
        );

        rejectedRequest.setProperty(HippoStdPubWfNodeType.HIPPOSTDPUBWF_USERNAME, "testuser");

        // editor own rejected request: cancelRequest=true
        assertMatchingKeyValues(wf.hints(), HintsBuilder.build()
                .status(true).isLive(false).previewAvailable(true).checkModified(false).noEdit().editable()
                .cancelRequest(rejectedRequest.getIdentifier())
                .infoRequest(rejectedRequest.getIdentifier())
                .requestPublication(true).requestDepublication(false).listVersions().retrieveVersion()
                .listBranches().branch(true).getBranch(false).checkoutBranch(false).removeBranch(false)
                .campaign(false).removeCampaign(false)//.labelVersion(true).removeLabelVersion(true)
                .terminateable(true).copy()
                .saveUnpublished(false)
                .hints());
        assertMatchingSCXMLStates(wf.getWorkflowExecutor(), StatesBuilder.build()
                .status().logEvent().editable().requested().publishable().noDepublish().versionable().terminateable().copyable()
                .branchable().noCheckoutBranch().noRemoveBranch().noReintegrateBranch().canPublishBranch().noDepublishBranch()
                .states()
        );

        session.setPermissions(rejectedRequest.getPath(), "hippo:author", true);

        // author own rejected request: cancelRequest=true
        assertMatchingKeyValues(wf.hints(), HintsBuilder.build()
                .status(true).isLive(false).previewAvailable(true).checkModified(false).noEdit().editable()
                .cancelRequest(rejectedRequest.getIdentifier())
                .infoRequest(rejectedRequest.getIdentifier())
                .requestPublication(true).requestDepublication(false).listVersions().retrieveVersion()
                .listBranches().branch(true).getBranch(false).checkoutBranch(false).removeBranch(false)
                .campaign(false).removeCampaign(false)//.labelVersion(true).removeLabelVersion(true)
                .terminateable(true).copy()
                .saveUnpublished(false)
                .hints());
        assertMatchingSCXMLStates(wf.getWorkflowExecutor(), StatesBuilder.build()
                .status().logEvent().editable().requested().publishable().noDepublish().versionable().terminateable().copyable()
                .branchable().noCheckoutBranch().noRemoveBranch().noReintegrateBranch().canPublishBranch().noDepublishBranch()
                .states()
        );

        rejectedRequest.setProperty(HippoStdPubWfNodeType.HIPPOSTDPUBWF_USERNAME, "otheruser");

        // author other user rejected request: no-op
        assertMatchingKeyValues(wf.hints(), HintsBuilder.build()
                .status(true).isLive(false).previewAvailable(true).checkModified(false).noEdit().editable()
                .requestPublication(true).requestDepublication(false).listVersions().retrieveVersion()
                .infoRequest(rejectedRequest.getIdentifier())
                .listBranches().branch(true).getBranch(false).checkoutBranch(false).removeBranch(false)
                .campaign(false).removeCampaign(false)//.labelVersion(true).removeLabelVersion(true)
                .terminateable(true).copy()
                .saveUnpublished(false)
                .hints());
        assertMatchingSCXMLStates(wf.getWorkflowExecutor(), StatesBuilder.build()
                .status().logEvent().editable().requested().publishable().noDepublish().versionable().terminateable().copyable()
                .branchable().noCheckoutBranch().noRemoveBranch().noReintegrateBranch().canPublishBranch().noDepublishBranch()
                .states()
        );

        rejectedRequest.remove();

        MockNode scheduledRequest = addRequest(handleNode, HippoStdPubWfNodeType.PUBLISH, false);
        session.setPermissions(scheduledRequest.getPath(), "hippo:editor", true);

        // editor scheduled request: cancelRequest=true
        assertMatchingKeyValues(wf.hints(), HintsBuilder.build()
                .status(true).isLive(false).previewAvailable(true).checkModified(false).noEdit()
                .cancelRequest(scheduledRequest.getIdentifier())
                .infoRequest(scheduledRequest.getIdentifier())
                .requestPublication(false).requestDepublication(false).listVersions().retrieveVersion()
                .listBranches().branch(true).getBranch(false).checkoutBranch(false).removeBranch(false)
                .campaign(false).removeCampaign(false)//.labelVersion(true).removeLabelVersion(true)
                .terminateable(false).copy()
                .saveUnpublished(false)
                .hints());
        assertMatchingSCXMLStates(wf.getWorkflowExecutor(), StatesBuilder.build()
                .status().logEvent().noEdit().requested().publishable().noDepublish().versionable().terminateable().copyable()
                .branchable().noCheckoutBranch().noRemoveBranch().noReintegrateBranch().canPublishBranch().noDepublishBranch()
                .states()
        );

        session.setPermissions(scheduledRequest.getPath(), "hippo:author", true);

        // author scheduled request: no-op
        assertMatchingKeyValues(wf.hints(), HintsBuilder.build()
                .status(true).isLive(false).previewAvailable(true).checkModified(false).noEdit()
                .requestPublication(false).requestDepublication(false).listVersions().retrieveVersion()
                .infoRequest(scheduledRequest.getIdentifier())
                .listBranches().branch(true).getBranch(false).checkoutBranch(false).removeBranch(false)
                .campaign(false).removeCampaign(false)//.labelVersion(true).removeLabelVersion(true)
                .terminateable(false).copy()
                .saveUnpublished(false)
                .hints());
        assertMatchingSCXMLStates(wf.getWorkflowExecutor(), StatesBuilder.build()
                .status().logEvent().noEdit().requested().publishable().noDepublish().versionable().terminateable().copyable()
                .branchable().noCheckoutBranch().noRemoveBranch().noReintegrateBranch().canPublishBranch().noDepublishBranch()
                .states()
        );
    }

    @Test
    public void testPublishState() throws Exception {
        MockAccessManagedSession session = new MockAccessManagedSession(MockNode.root());
        MockNode handleNode = session.getRootNode().addNode("test", HippoNodeType.NT_HANDLE);
        MockWorkflowContext workflowContext = new MockWorkflowContext("testuser", session);
        DocumentWorkflowImpl wf = getDocumentWorkflowImpl();
        wf.setWorkflowContext(workflowContext);
        wf.setNode(handleNode);

        MockNode draftVariant = addVariant(handleNode, HippoStdNodeType.DRAFT);
        session.setPermissions(draftVariant.getPath(), "hippo:author", true);
        draftVariant.setProperty(HippoStdNodeType.HIPPOSTD_HOLDER, "testuser");

        MockNode unpublishedVariant = addVariant(handleNode, HippoStdNodeType.UNPUBLISHED);
        session.setPermissions(unpublishedVariant.getPath(), "hippo:author", true);

        // author, no request, editing, modified (unpublished): requestPublication=false
        assertMatchingKeyValues(wf.hints(), HintsBuilder.build()
                .status(true).isLive(false).previewAvailable(true).checkModified(true).editing()
                .requestPublication(false).requestDepublication(false).listVersions().retrieveVersion()
                .listBranches().branch(false).getBranch(false).checkoutBranch(false).removeBranch(false)
                .campaign(false).removeCampaign(false)//.labelVersion(true).removeLabelVersion(true)
                .terminateable(false).copy()
                .saveUnpublished(false)
                .hints());
        assertMatchingSCXMLStates(wf.getWorkflowExecutor(), StatesBuilder.build()
                .status().logEvent().editing().noRequest().noPublish().noDepublish().versionable().noTerminate().copyable()
                .noBranchable().canPublishBranch().noDepublishBranch().noCheckoutBranch().noReintegrateBranch().noRemoveBranch()
                .states()
        );

        session.setPermissions(unpublishedVariant.getPath(), "hippo:editor,hippo:author", true);
        session.setPermissions(draftVariant.getPath(), "hippo:editor,hippo:author", true);

        // editor, no request, editing, modified (unpublished): requestPublication=false,publish=false
        assertMatchingKeyValues(wf.hints(), HintsBuilder.build()
                .status(true).isLive(false).previewAvailable(true).checkModified(true).editing()
                .requestPublication(false).publish(false).requestDepublication(false).depublish(false)
                .listVersions().retrieveVersion().versionable().terminateable(false).copy()
                .listBranches().branch(false).getBranch(false).checkoutBranch(false).removeBranch(false)
                .campaign(true).removeCampaign(true)//.labelVersion(true).removeLabelVersion(true)
                .reintegrateBranch(false).publishBranch(true).depublishBranch(false)
                .saveUnpublished(false)
                .hints());
        assertMatchingSCXMLStates(wf.getWorkflowExecutor(), StatesBuilder.build().copyable()
                .status().logEvent().editing().noRequest().noPublish().noDepublish().versionable().noTerminate()
                .noBranchable().canPublishBranch().noDepublishBranch().noCheckoutBranch().noReintegrateBranch().noRemoveBranch()
                .states()
        );

        MockNode publishedVariant = addVariant(handleNode, HippoStdNodeType.PUBLISHED);
        session.setPermissions(publishedVariant.getPath(), "hippo:editor,hippo:author", true);
        draftVariant.getProperty(HippoStdNodeType.HIPPOSTD_HOLDER).remove();

        // editor, no request, !editing, !modified (unpublished==published): requestPublication=false,publish=false
        assertMatchingKeyValues(wf.hints(), HintsBuilder.build()
                .status(true).isLive(true).previewAvailable(true).checkModified(true).noEdit().editable().copy()
                .requestPublication(false).publish(false).depublish(true).requestDepublication(true)
                .listVersions().retrieveVersion().versionable().terminateable(false)
                .listBranches().branch(true).getBranch(false).checkoutBranch(false).removeBranch(false)
                .campaign(true).removeCampaign(true)//.labelVersion(true).removeLabelVersion(true)
                .reintegrateBranch(false).publishBranch(false).depublishBranch(true)
                .saveUnpublished(false)
                .hints());
        assertMatchingSCXMLStates(wf.getWorkflowExecutor(), StatesBuilder.build().branchable()
                .status().logEvent().editable().noRequest().noPublish().depublishable().versionable().noTerminate().copyable()
                .noCheckoutBranch().noRemoveBranch().noReintegrateBranch().canDepublishBranch().noPublishBranch()
                .states()
        );

        // make unpublished modified
        Calendar publishedModified = Calendar.getInstance();
        Calendar unpublishedModified = Calendar.getInstance();
        publishedModified.setTimeInMillis(unpublishedModified.getTimeInMillis() - 1000);
        unpublishedVariant.setProperty(HippoStdPubWfNodeType.HIPPOSTDPUBWF_LAST_MODIFIED_DATE, new MockValue(PropertyType.DATE, ISO8601.format(unpublishedModified)));
        publishedVariant.setProperty(HippoStdPubWfNodeType.HIPPOSTDPUBWF_LAST_MODIFIED_DATE, new MockValue(PropertyType.DATE, ISO8601.format(publishedModified)));

        // editor, no request, !editing, modified (unpublished!=published): requestPublication=true,publish=true
        assertMatchingKeyValues(wf.hints(), HintsBuilder.build()
                .status(true).isLive(true).previewAvailable(true).checkModified(true).noEdit().editable().copy()
                .requestPublication(true).publish(true).depublish(true).requestDepublication(true)
                .listVersions().retrieveVersion().versionable().terminateable(false)
                .listBranches().branch(true).getBranch(false).checkoutBranch(false).removeBranch(false)
                .campaign(true).removeCampaign(true)//.labelVersion(true).removeLabelVersion(true)
                .reintegrateBranch(false).publishBranch(true).depublishBranch(true)
                .saveUnpublished(false)
                .hints());
        assertMatchingSCXMLStates(wf.getWorkflowExecutor(), StatesBuilder.build()
                .status().logEvent().editable().noRequest().publishable().depublishable().versionable().noTerminate().copyable()
                .branchable().noCheckoutBranch().noRemoveBranch().noReintegrateBranch().canPublishBranch().canDepublishBranch()
                .states()
        );

        session.setPermissions(publishedVariant.getPath(), "hippo:author", true);
        session.setPermissions(unpublishedVariant.getPath(), "hippo:author", true);
        session.setPermissions(draftVariant.getPath(), "hippo:author", true);

        // author, no request, !editing, modified (unpublished!=published): requestPublication=true
        assertMatchingKeyValues(wf.hints(), HintsBuilder.build()
                .status(true).isLive(true).previewAvailable(true).checkModified(true).noEdit().editable()
                .requestPublication(true).requestDepublication(true)
                .listVersions().retrieveVersion()
                .listBranches().branch(true).getBranch(false).checkoutBranch(false).removeBranch(false)
                .campaign(false).removeCampaign(false)//.labelVersion(true).removeLabelVersion(true)
                .terminateable(false).copy()
                .saveUnpublished(false)
                .hints());
        assertMatchingSCXMLStates(wf.getWorkflowExecutor(), StatesBuilder.build()
                .status().logEvent().editable().noRequest().publishable().depublishable().versionable().noTerminate().copyable()
                .branchable().noCheckoutBranch().noRemoveBranch().noReintegrateBranch().canPublishBranch().canDepublishBranch()
                .states()
        );

        MockNode scheduledRequest = addRequest(handleNode, HippoStdPubWfNodeType.PUBLISH, false);
        session.setPermissions(scheduledRequest.getPath(), "hippo:author", true);

        // author, request, !editing, modified (unpublished!=published): requestPublication=false
        assertMatchingKeyValues(wf.hints(), HintsBuilder.build()
                .status(true).isLive(true).previewAvailable(true).checkModified(true).noEdit()
                .requestPublication(false).requestDepublication(false)
                .infoRequest(scheduledRequest.getIdentifier())
                .listVersions().retrieveVersion()
                .listBranches().branch(true).getBranch(false).checkoutBranch(false).removeBranch(false)
                .campaign(false).removeCampaign(false)//.labelVersion(true).removeLabelVersion(true)
                .terminateable(false).copy()
                .saveUnpublished(false)
                .hints());
        assertMatchingSCXMLStates(wf.getWorkflowExecutor(), StatesBuilder.build()
                .status().logEvent().noEdit().requested().publishable().depublishable().versionable().noTerminate().copyable()
                .branchable().noCheckoutBranch().noRemoveBranch().noReintegrateBranch().canPublishBranch().canDepublishBranch()
                .states()
        );

        workflowContext.setUserIdentity("workflowuser");
        session.setPermissions(publishedVariant.getPath(), "hippo:editor,hippo:author", true);
        session.setPermissions(unpublishedVariant.getPath(), "hippo:editor,hippo:author", true);
        session.setPermissions(draftVariant.getPath(), "hippo:editor,hippo:author", true);

        // workflowuser, request, !editing, modified (unpublished!=published): requestPublication=true,publish=true
        assertMatchingKeyValues(wf.hints(), HintsBuilder.build()
                .status(true).isLive(true).previewAvailable(true).checkModified(true).noEdit().copy()
                .requestPublication(true).publish(true).depublish(true).requestDepublication(true)
                .infoRequest(scheduledRequest.getIdentifier())
                .listVersions().retrieveVersion().versionable().terminateable(false)
                .listBranches().branch(true).getBranch(false).checkoutBranch(false).removeBranch(false)
                .reintegrateBranch(false).publishBranch(true).depublishBranch(true)
                .campaign(true).removeCampaign(true)//.labelVersion(true).removeLabelVersion(true)
                .saveUnpublished(false)
                .hints());
        assertMatchingSCXMLStates(wf.getWorkflowExecutor(), StatesBuilder.build()
                .status().logEvent().noEdit().requested().publishable().depublishable().versionable().noTerminate().copyable()
                .branchable().noCheckoutBranch().noRemoveBranch().noReintegrateBranch().canPublishBranch().canDepublishBranch()
                .states()
        );

        publishedVariant.addMixin(HIPPO_MIXIN_BRANCH_INFO);
        publishedVariant.setProperty(HIPPO_PROPERTY_BRANCH_ID, "foo");

        // workflowuser, request, !editing, modified (unpublished!=published): requestPublication=true,publish=true
        // published = foo
        assertMatchingKeyValues(wf.hints("foo"), HintsBuilder.build()
                .status(true).isLive(true).previewAvailable(true).checkModified(true).noEdit().editable().copy()
                .requestPublication(false).publish(false).depublish(false).requestDepublication(false)
                .infoRequest(scheduledRequest.getIdentifier())
                .listVersions().retrieveVersion().versionable().terminateable(false)
                .listBranches().branch(true).getBranch(true).checkoutBranch(true).removeBranch(false)
                .reintegrateBranch(true).publishBranch(false).depublishBranch(true)
                .campaign(true).removeCampaign(true)//.labelVersion(true).removeLabelVersion(true)
                .saveUnpublished(false)
                .hints());
        assertMatchingSCXMLStates(wf.getWorkflowExecutor(), StatesBuilder.build()
                .status().logEvent().editable().requested().versionable().noTerminate().copyable()
                .noPublish().noDepublish()
                .branchable().canCheckoutBranch().noRemoveBranch().canReintegrateBranch().noPublishBranch().canDepublishBranch()
                .states()
        );

        // remove the request
        scheduledRequest.remove();

        // workflowuser, request, !editing, modified (unpublished!=published): requestPublication=true,publish=true
        // published = foo
        assertMatchingKeyValues(wf.hints("foo"), HintsBuilder.build()
                .status(true).isLive(true).previewAvailable(true).checkModified(true).noEdit().editable().copy()
                .requestPublication(false).publish(false).depublish(false).requestDepublication(false)
                .listVersions().retrieveVersion().versionable().terminateable(false)
                .listBranches().branch(true).getBranch(true).checkoutBranch(true).removeBranch(false)
                .campaign(true).removeCampaign(true)//.labelVersion(true).removeLabelVersion(true)
                .reintegrateBranch(true).publishBranch(false).depublishBranch(true)
                .saveUnpublished(false)
                .hints());
        assertMatchingSCXMLStates(wf.getWorkflowExecutor(), StatesBuilder.build()
                .status().logEvent().editable().noRequest().versionable().noTerminate().copyable()
                .noPublish().noDepublish()
                .branchable().canCheckoutBranch().noRemoveBranch().canReintegrateBranch().noPublishBranch().canDepublishBranch()
                .states()
        );

        assertMatchingKeyValues(wf.hints(), HintsBuilder.build()
                .status(true).isLive(false).previewAvailable(true).checkModified(true).noEdit().editable().copy()
                .requestPublication(true).publish(true).depublish(false).requestDepublication(false)
                .listVersions().retrieveVersion().versionable().terminateable(false)
                .listBranches().branch(true).getBranch(true).checkoutBranch(true).removeBranch(false)
                .reintegrateBranch(false).publishBranch(true).depublishBranch(false)
                .campaign(true).removeCampaign(true)//.labelVersion(true).removeLabelVersion(true)
                .saveUnpublished(false)
                .hints());
        assertMatchingSCXMLStates(wf.getWorkflowExecutor(), StatesBuilder.build()
                .status().logEvent().editable().noRequest().versionable().noTerminate().copyable()
                .publishable().noDepublish()
                .branchable().canCheckoutBranch().noRemoveBranch().noReintegrateBranch().canPublishBranch().noDepublishBranch()
                .states()
        );
    }

    @Test
    public void testDePublishState() throws Exception {
        MockAccessManagedSession session = new MockAccessManagedSession(MockNode.root());
        MockNode handleNode = session.getRootNode().addNode("test", HippoNodeType.NT_HANDLE);
        MockWorkflowContext workflowContext = new MockWorkflowContext("testuser", session);
        DocumentWorkflowImpl wf = getDocumentWorkflowImpl();
        wf.setWorkflowContext(workflowContext);
        wf.setNode(handleNode);

        MockNode draftVariant = addVariant(handleNode, HippoStdNodeType.DRAFT);
        session.setPermissions(draftVariant.getPath(), "hippo:author", true);
        draftVariant.setProperty(HippoStdNodeType.HIPPOSTD_HOLDER, "testuser");

        MockNode unpublishedVariant = addVariant(handleNode, HippoStdNodeType.UNPUBLISHED);
        session.setPermissions(unpublishedVariant.getPath(), "hippo:author", true);

        MockNode publishedVariant = addVariant(handleNode, HippoStdNodeType.PUBLISHED);
        session.setPermissions(publishedVariant.getPath(), "hippo:author", true);

        // author, no request, editing, live: requestDepublication=false
        assertMatchingKeyValues(wf.hints(), HintsBuilder.build()
                .status(true).isLive(true).previewAvailable(true).checkModified(true).editing()
                .requestPublication(false).requestDepublication(false).listVersions().retrieveVersion()
                .listBranches().branch(false).getBranch(false).checkoutBranch(false).removeBranch(false)
                .campaign(false).removeCampaign(false)//.labelVersion(true).removeLabelVersion(true)
                .terminateable(false).copy()
                .saveUnpublished(false)
                .hints());
        assertMatchingSCXMLStates(wf.getWorkflowExecutor(), StatesBuilder.build()
                .status().logEvent().editing().noRequest().noPublish().noDepublish().versionable().noTerminate().copyable()
                .noBranchable().noCheckoutBranch().noRemoveBranch().noReintegrateBranch().noPublishBranch().canDepublishBranch()
                .states()
        );

        session.setPermissions(publishedVariant.getPath(), "hippo:editor,hippo:author", true);
        session.setPermissions(unpublishedVariant.getPath(), "hippo:editor,hippo:author", true);
        session.setPermissions(draftVariant.getPath(), "hippo:editor,hippo:author", true);

        // editor, no request, editing, live: requestDepublication=false,depublish=false
        assertMatchingKeyValues(wf.hints(), HintsBuilder.build()
                .status(true).isLive(true).previewAvailable(true).checkModified(true).editing()
                .requestPublication(false).requestDepublication(false).depublish(false).publish(false)
                .listVersions().retrieveVersion().copy().versionable().terminateable(false)
                .listBranches().branch(false).getBranch(false).checkoutBranch(false).removeBranch(false)
                .campaign(true).removeCampaign(true)//.labelVersion(true).removeLabelVersion(true)
                .reintegrateBranch(false).publishBranch(false).depublishBranch(true)
                .saveUnpublished(false)
                .hints());
        assertMatchingSCXMLStates(wf.getWorkflowExecutor(), StatesBuilder.build()
                .status().logEvent().editing().noRequest().noPublish().noDepublish().versionable().noTerminate().copyable()
                .noBranchable().noCheckoutBranch().noRemoveBranch().noReintegrateBranch().noPublishBranch().canDepublishBranch()
                .states()
        );

        draftVariant.getProperty(HippoStdNodeType.HIPPOSTD_HOLDER).remove();
        publishedVariant.setProperty(HIPPO_AVAILABILITY, new String[]{});

        // editor, no request, !editing, !live: requestDepublication=false,depublish=false
        assertMatchingKeyValues(wf.hints(), HintsBuilder.build()
                .status(true).isLive(false).previewAvailable(true).checkModified(true).noEdit().editable()
                .requestPublication(true).requestDepublication(false).depublish(false).publish(true)
                .listVersions().retrieveVersion().copy().versionable().terminateable(true)
                .listBranches().branch(true).getBranch(false).checkoutBranch(false).removeBranch(false)
                .reintegrateBranch(false).publishBranch(true).depublishBranch(false)
                .campaign(true).removeCampaign(true)//.labelVersion(true).removeLabelVersion(true)
                .saveUnpublished(false)
                .hints());
        assertMatchingSCXMLStates(wf.getWorkflowExecutor(), StatesBuilder.build()
                .status().logEvent().editable().noRequest().publishable().noDepublish().versionable().terminateable().copyable()
                .branchable().noCheckoutBranch().noRemoveBranch().noReintegrateBranch().canPublishBranch().noDepublishBranch()
                .states()
        );

        publishedVariant.setProperty(HIPPO_AVAILABILITY, new String[]{"live"});

        // editor, no request, !editing, live: requestDepublication=true,depublish=true
        assertMatchingKeyValues(wf.hints(), HintsBuilder.build()
                .status(true).isLive(true).previewAvailable(true).checkModified(true).noEdit().editable()
                .requestPublication(false).requestDepublication(true).depublish(true).publish(false)
                .listVersions().retrieveVersion().copy().versionable().terminateable(false)
                .listBranches().branch(true).getBranch(false).checkoutBranch(false).removeBranch(false)
                .reintegrateBranch(false).publishBranch(false).depublishBranch(true)
                .campaign(true).removeCampaign(true)//.labelVersion(true).removeLabelVersion(true)
                .saveUnpublished(false)
                .hints());
        assertMatchingSCXMLStates(wf.getWorkflowExecutor(), StatesBuilder.build()
                .status().logEvent().editable().noRequest().noPublish().depublishable().versionable().noTerminate().copyable()
                .branchable().noCheckoutBranch().noRemoveBranch().noReintegrateBranch().noPublishBranch().canDepublishBranch()
                .states()
        );

        session.setPermissions(publishedVariant.getPath(), "hippo:author", true);
        session.setPermissions(unpublishedVariant.getPath(), "hippo:author", true);
        session.setPermissions(draftVariant.getPath(), "hippo:author", true);

        // author, no request, !editing, live: requestDepublication=true
        assertMatchingKeyValues(wf.hints(), HintsBuilder.build()
                .status(true).isLive(true).previewAvailable(true).checkModified(true).noEdit().editable()
                .requestPublication(false).requestDepublication(true)
                .listVersions().retrieveVersion()
                .listBranches().branch(true).getBranch(false).checkoutBranch(false).removeBranch(false)
                .campaign(false).removeCampaign(false)//.labelVersion(true).removeLabelVersion(true)
                .terminateable(false).copy()
                .saveUnpublished(false)
                .hints());
        assertMatchingSCXMLStates(wf.getWorkflowExecutor(), StatesBuilder.build()
                .status().logEvent().editable().noRequest().noPublish().depublishable().versionable().noTerminate().copyable()
                .branchable().noCheckoutBranch().noRemoveBranch().noReintegrateBranch().noPublishBranch().canDepublishBranch()
                .states()
        );

        MockNode scheduledRequest = addRequest(handleNode, HippoStdPubWfNodeType.PUBLISH, false);
        session.setPermissions(scheduledRequest.getPath(), "hippo:author", true);

        // author, request, !editing, live: requestDepublication=false
        assertMatchingKeyValues(wf.hints(), HintsBuilder.build()
                .status(true).isLive(true).previewAvailable(true).checkModified(true).noEdit()
                .requestPublication(false).requestDepublication(false)
                .infoRequest(scheduledRequest.getIdentifier())
                .listVersions().retrieveVersion()
                .listBranches().branch(true).getBranch(false).checkoutBranch(false).removeBranch(false)
                .campaign(false).removeCampaign(false)//.labelVersion(true).removeLabelVersion(true)
                .terminateable(false).copy()
                .saveUnpublished(false)
                .hints());
        assertMatchingSCXMLStates(wf.getWorkflowExecutor(), StatesBuilder.build()
                .status().logEvent().noEdit().requested().noPublish().depublishable().versionable().noTerminate().copyable()
                .branchable().noCheckoutBranch().noRemoveBranch().noReintegrateBranch().noPublishBranch().canDepublishBranch()
                .states()
        );

        workflowContext.setUserIdentity("workflowuser");
        session.setPermissions(publishedVariant.getPath(), "hippo:editor,hippo:author", true);

        // workflowuser, request, !editing, live: requestDepublication=true,depublish=true
        assertMatchingKeyValues(wf.hints(), HintsBuilder.build()
                .status(true).isLive(true).previewAvailable(true).checkModified(true).noEdit()
                .requestPublication(false).requestDepublication(true).depublish(true)
                .infoRequest(scheduledRequest.getIdentifier())
                .listVersions().retrieveVersion().copy()
                .listBranches().branch(true).getBranch(false).checkoutBranch(false).removeBranch(false)
                .campaign(false).removeCampaign(false)//.labelVersion(true).removeLabelVersion(true)
                .terminateable(false)
                .saveUnpublished(false)
                .hints());
        assertMatchingSCXMLStates(wf.getWorkflowExecutor(), StatesBuilder.build()
                .status().logEvent().noEdit().requested().noPublish().depublishable().versionable().noTerminate().copyable()
                .branchable().noCheckoutBranch().noRemoveBranch().noReintegrateBranch().noPublishBranch().canDepublishBranch()
                .states()
        );
    }

    @Test
    public void testVersioningState() throws Exception {
        MockAccessManagedSession session = new MockAccessManagedSession(MockNode.root());
        MockNode handleNode = session.getRootNode().addNode("test", HippoNodeType.NT_HANDLE);
        MockWorkflowContext workflowContext = new MockWorkflowContext("testuser", session);
        DocumentWorkflowImpl wf = getDocumentWorkflowImpl();
        wf.setWorkflowContext(workflowContext);
        wf.setNode(handleNode);

        MockNode draftVariant = addVariant(handleNode, HippoStdNodeType.DRAFT);
        session.setPermissions(draftVariant.getPath(), "hippo:author", true);

        // no unpublished: (only) listVersions=true
        assertMatchingKeyValues(wf.hints(), HintsBuilder.build()
                .status(true).isLive(false).previewAvailable(false).checkModified(false).noEdit().editable()
                .requestPublication(false).requestDepublication(false).listVersions()
                .listBranches().branch(false).getBranch(false).checkoutBranch(false).removeBranch(false)
                .campaign(false).removeCampaign(false)//.labelVersion(false).removeLabelVersion(false)
                .terminateable(true)
                .saveUnpublished(false)
                .hints());
        assertMatchingSCXMLStates(wf.getWorkflowExecutor(), StatesBuilder.build()
                .status().logEvent().editable().noRequest().noPublish().noDepublish().noVersioning().terminateable().noCopy()
                .noBranchable().noCheckoutBranch().noRemoveBranch().noReintegrateBranch().noPublishBranch().noDepublishBranch()
                .states()
        );

        draftVariant.remove();
        MockNode unpublishedVariant = addVariant(handleNode, HippoStdNodeType.UNPUBLISHED);
        session.setPermissions(unpublishedVariant.getPath(), "hippo:author", true);

        // author, unpublished: listVersions=true, retrieveVersion=true
        assertMatchingKeyValues(wf.hints(), HintsBuilder.build()
                .status(true).isLive(false).previewAvailable(true).checkModified(false).noEdit().editable()
                .requestPublication(true).requestDepublication(false).listVersions().retrieveVersion()
                .listBranches().branch(true).getBranch(false).checkoutBranch(false).removeBranch(false)
                .campaign(false).removeCampaign(false)//.labelVersion(true).removeLabelVersion(true)
                .terminateable(true).copy()
                .saveUnpublished(false)
                .hints());
        assertMatchingSCXMLStates(wf.getWorkflowExecutor(), StatesBuilder.build()
                .status().logEvent().editable().noRequest().publishable().noDepublish().versionable().terminateable().copyable()
                .branchable().noCheckoutBranch().noRemoveBranch().noReintegrateBranch().canPublishBranch().noDepublishBranch()
                .states()
        );

        session.setPermissions(unpublishedVariant.getPath(), "hippo:editor,hippo:author", true);

        // editor, unpublished: listVersions=true, retrieveVersion=true, versionable
        assertMatchingKeyValues(wf.hints(), HintsBuilder.build()
                .status(true).isLive(false).previewAvailable(true).checkModified(false).noEdit().editable()
                .requestPublication(true).publish(true).requestDepublication(false).depublish(false)
                .listVersions().retrieveVersion().versionable().terminateable(true).copy()
                .listBranches().branch(true).getBranch(false).checkoutBranch(false).removeBranch(false)
                .reintegrateBranch(false).publishBranch(true).depublishBranch(false)
                .campaign(true).removeCampaign(true)//.labelVersion(true).removeLabelVersion(true)
                .saveUnpublished(false)
                .hints());
        assertMatchingSCXMLStates(wf.getWorkflowExecutor(), StatesBuilder.build()
                .status().logEvent().editable().noRequest().publishable().noDepublish().versionable().terminateable().copyable()
                .branchable().noCheckoutBranch().noRemoveBranch().noReintegrateBranch().canPublishBranch().noDepublishBranch()
                .states()
        );
    }

    @Test
    public void testTerminateState() throws Exception {
        MockAccessManagedSession session = new MockAccessManagedSession(MockNode.root());
        MockNode folderNode = session.getRootNode().addNode("folder", HippoNodeType.NT_DOCUMENT);
        session.setPermissions(folderNode.getPath(), "jcr:write", false);
        MockNode handleNode = folderNode.addNode("test", HippoNodeType.NT_HANDLE);
        MockWorkflowContext workflowContext = new MockWorkflowContext("testuser", session);
        DocumentWorkflowImpl wf = getDocumentWorkflowImpl();
        wf.setWorkflowContext(workflowContext);
        wf.setNode(handleNode);

        MockNode draftVariant = addVariant(handleNode, HippoStdNodeType.DRAFT);
        session.setPermissions(draftVariant.getPath(), "hippo:author", true);
        draftVariant.setProperty(HippoStdNodeType.HIPPOSTD_HOLDER, "testuser");
        MockNode publishedVariant = addVariant(handleNode, HippoStdNodeType.PUBLISHED);
        session.setPermissions(publishedVariant.getPath(), "hippo:author", true);

        // author, editing, live: requestDelete=false
        assertMatchingKeyValues(wf.hints(), HintsBuilder.build()
                .status(true).isLive(true).previewAvailable(true).checkModified(false).editing()
                .requestPublication(false).requestDepublication(false)
                .listVersions()
                .listBranches().branch(false).getBranch(false).checkoutBranch(false).removeBranch(false)
                .campaign(false).removeCampaign(false)//.labelVersion(false).removeLabelVersion(false)
                .terminateable(false).copy()
                .saveUnpublished(false)
                .hints());
        assertMatchingSCXMLStates(wf.getWorkflowExecutor(), StatesBuilder.build()
                .status().logEvent().editing().noRequest().noPublish().noDepublish().noVersioning().noTerminate().copyable()
                .noBranchable().noCheckoutBranch().noRemoveBranch().noReintegrateBranch().noPublishBranch().canDepublishBranch()
                .states()
        );

        session.setPermissions(publishedVariant.getPath(), "hippo:editor,hippo:author", true);

        // editor, editing, live: requestDelete=false
        final Map<String, Serializable> hintsNoJcrWriteOnFolder = wf.hints();
        assertMatchingKeyValues(hintsNoJcrWriteOnFolder, HintsBuilder.build()
                .status(true).isLive(true).previewAvailable(true).checkModified(false).editing()
                .requestPublication(false).publish(false).requestDepublication(false).depublish(false)
                .listVersions().terminateable(false).copy()
                .listBranches().branch(false).getBranch(false).checkoutBranch(false).removeBranch(false)
                .campaign(false).removeCampaign(false)//.labelVersion(false).removeLabelVersion(false)
                .reintegrateBranch(false).publishBranch(false).depublishBranch(true)
                .saveUnpublished(false)
                .hints());
        assertMatchingSCXMLStates(wf.getWorkflowExecutor(), StatesBuilder.build()
                .status().logEvent().editing().noRequest().noPublish().noDepublish().noVersioning().noTerminate().copyable()
                .noBranchable().noCheckoutBranch().noRemoveBranch().noReintegrateBranch().noPublishBranch().canDepublishBranch()
                .states()
        );


        session.setPermissions(folderNode.getPath(), "jcr:write", true);

        Assertions.assertThat(wf.hints())
                .as("Having jcr:write permissions on a folder should not in any way impact what an " +
                        "editor/author are allowed to do on that folder: The role hippo:editor or hippo:author defines " +
                        "that")
                .isEqualTo(hintsNoJcrWriteOnFolder);

        session.setPermissions(publishedVariant.getPath(), "hippo:author", true);

        // author + writable containing folder, editing, live: requestDelete=false
        assertMatchingKeyValues(wf.hints(), HintsBuilder.build()
                .status(true).isLive(true).previewAvailable(true).checkModified(false).editing()
                .requestPublication(false).requestDepublication(false)
                .listVersions()
                .listBranches().branch(false).getBranch(false).checkoutBranch(false).removeBranch(false)
                .campaign(false).removeCampaign(false)//.labelVersion(false).removeLabelVersion(false)
                .terminateable(false).copy()
                .saveUnpublished(false)
                .hints());
        assertMatchingSCXMLStates(wf.getWorkflowExecutor(), StatesBuilder.build()
                .status().logEvent().editing().noRequest().noPublish().noDepublish().noVersioning().noTerminate().copyable()
                .noBranchable().noCheckoutBranch().noRemoveBranch().noReintegrateBranch().noPublishBranch().canDepublishBranch()
                .states()
        );

        draftVariant.getProperty(HippoStdNodeType.HIPPOSTD_HOLDER).remove();
        publishedVariant.setProperty(HIPPO_AVAILABILITY, new String[0]);

        MockNode scheduledRequest = addRequest(handleNode, HippoStdPubWfNodeType.PUBLISH, false);

        // author + writable containing folder, !editing, !live, request: requestDelete=false
        assertMatchingKeyValues(wf.hints(), HintsBuilder.build()
                .status(true).isLive(false).previewAvailable(false).checkModified(false).noEdit()
                .cancelRequest(scheduledRequest.getIdentifier())
                .requestPublication(false).requestDepublication(false)
                .infoRequest(scheduledRequest.getIdentifier())
                .listVersions()
                .listBranches().branch(true).getBranch(false).checkoutBranch(false).removeBranch(false)
                .campaign(false).removeCampaign(false)//.labelVersion(false).removeLabelVersion(false)
                .terminateable(false).copy()
                .saveUnpublished(false)
                .hints());
        assertMatchingSCXMLStates(wf.getWorkflowExecutor(), StatesBuilder.build()
                .status().logEvent().noEdit().requested().noPublish().noDepublish().noVersioning().terminateable().copyable()
                .branchable().noCheckoutBranch().noRemoveBranch().noReintegrateBranch().noPublishBranch().noDepublishBranch()
                .states()
        );

        scheduledRequest.remove();

        // author + writable containing folder, !editing, !live: requestDelete=true
        assertMatchingKeyValues(wf.hints(), HintsBuilder.build()
                .status(true).isLive(false).previewAvailable(false).checkModified(false).noEdit().editable()
                .requestPublication(false).requestDepublication(false)
                .listVersions()
                .listBranches().branch(true).getBranch(false).checkoutBranch(false).removeBranch(false)
                .campaign(false).removeCampaign(false)//.labelVersion(false).removeLabelVersion(false)
                .terminateable(true).copy()
                .saveUnpublished(false)
                .hints());
        assertMatchingSCXMLStates(wf.getWorkflowExecutor(), StatesBuilder.build()
                .status().logEvent().editable().noRequest().noPublish().noDepublish().noVersioning().terminateable().copyable()
                .branchable().noCheckoutBranch().noRemoveBranch().noReintegrateBranch().noPublishBranch().noDepublishBranch()
                .states()
        );

        session.setPermissions(publishedVariant.getPath(), "hippo:editor,hippo:author", true);

        // editor + writable containing folder, !editing, !live: requestDelete=true, terminateable
        assertMatchingKeyValues(wf.hints(), HintsBuilder.build()
                .status(true).isLive(false).previewAvailable(false).checkModified(false).noEdit().editable()
                .requestPublication(false).publish(false).requestDepublication(false).depublish(false)
                .listVersions().terminateable(true).copy()
                .listBranches().branch(true).getBranch(false).checkoutBranch(false).removeBranch(false)
                .campaign(false).removeCampaign(false)//.labelVersion(false).removeLabelVersion(false)
                .reintegrateBranch(false).publishBranch(false).depublishBranch(false)
                .saveUnpublished(false)
                .hints());
        assertMatchingSCXMLStates(wf.getWorkflowExecutor(), StatesBuilder.build()
                .status().logEvent().editable().noRequest().noPublish().noDepublish().noVersioning().terminateable().copyable()
                .branchable().noCheckoutBranch().noRemoveBranch().noReintegrateBranch().noPublishBranch().noDepublishBranch()
                .states()
        );
    }

    @Test
    public void testCopyState() throws Exception {
        MockAccessManagedSession session = new MockAccessManagedSession(MockNode.root());
        MockNode handleNode = session.getRootNode().addNode("test", HippoNodeType.NT_HANDLE);
        MockWorkflowContext workflowContext = new MockWorkflowContext("testuser", session);
        DocumentWorkflowImpl wf = getDocumentWorkflowImpl();
        wf.setWorkflowContext(workflowContext);
        wf.setNode(handleNode);

        MockNode publishedVariant = addVariant(handleNode, HippoStdNodeType.PUBLISHED);
        session.setPermissions(publishedVariant.getPath(), "hippo:author", true);

        // author, published: no-copy
        assertMatchingKeyValues(wf.hints(), HintsBuilder.build()
                .status(true).isLive(true).previewAvailable(true).checkModified(false).noEdit().editable()
                .requestPublication(false).requestDepublication(true)
                .listVersions()
                .listBranches().branch(true).getBranch(false).checkoutBranch(false).removeBranch(false)
                .campaign(false).removeCampaign(false)////.labelVersion(false).removeLabelVersion(false)
                .terminateable(false).copy()
                .saveUnpublished(false)
                .hints());
        assertMatchingSCXMLStates(wf.getWorkflowExecutor(), StatesBuilder.build()
                .status().logEvent().editable().noRequest().noPublish().depublishable().noVersioning().noTerminate().copyable()
                .branchable().noCheckoutBranch().noRemoveBranch().noReintegrateBranch().noPublishBranch().canDepublishBranch()
                .states()
        );

        session.setPermissions(publishedVariant.getPath(), "hippo:editor,hippo:author", true);

        // editor, published: copy
        assertMatchingKeyValues(wf.hints(), HintsBuilder.build()
                .status(true).isLive(true).previewAvailable(true).checkModified(false).noEdit().editable()
                .requestPublication(false).publish(false).requestDepublication(true).depublish(true)
                .listVersions().terminateable(false).copy()
                .listBranches().branch(true).getBranch(false).checkoutBranch(false).removeBranch(false)
                .reintegrateBranch(false).publishBranch(false).depublishBranch(true)
                .campaign(false).removeCampaign(false)////.labelVersion(false).removeLabelVersion(false)
                .saveUnpublished(false)
                .hints());
        assertMatchingSCXMLStates(wf.getWorkflowExecutor(), StatesBuilder.build()
                .status().logEvent().editable().noRequest().noPublish().depublishable().noVersioning().noTerminate().copyable()
                .branchable().noCheckoutBranch().noRemoveBranch().noReintegrateBranch().noPublishBranch().canDepublishBranch()
                .states()
        );
    }
}
