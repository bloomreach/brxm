/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
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
import java.util.Calendar;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.jcr.PropertyType;

import org.apache.commons.scxml2.model.EnterableState;
import org.apache.jackrabbit.util.ISO8601;
import org.hippoecm.repository.HippoStdNodeType;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.HippoStdPubWfNodeType;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.onehippo.repository.mock.MockNode;
import org.onehippo.repository.mock.MockValue;
import org.onehippo.repository.scxml.MockAccessManagedSession;
import org.onehippo.repository.scxml.MockWorkflowContext;
import org.onehippo.repository.scxml.SCXMLWorkflowExecutor;

public class DocumentWorkflowTest extends BaseDocumentWorkflowTest {

    @BeforeClass
    public static void beforeClass() throws Exception {
        createDocumentWorkflowSCXMLRegistry();
    }

    @AfterClass
    public static void afterClass() throws Exception {
        destroyDocumentWorkflowSCXMLRegistry();
    }

    @SuppressWarnings("unchecked")
    protected static Map<?, ?> sortMap(Map<?, ?> map) {
        if (!(map instanceof SortedMap)) {
            SortedMap<Object, Object> sorted = new TreeMap<>(map);
            for (Map.Entry<Object,Object> entry : sorted.entrySet()) {
                if (entry.getValue() instanceof Map) {
                    entry.setValue(sortMap((Map<Object,Object>)entry.getValue()));
                }
            }
            return sorted;
        }
        return map;
    }

    protected static Set<String> getSCXMLStatusStateIds(SCXMLWorkflowExecutor executor) {
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
        if (!stateIds.equals(states)) {
            Assert.fail("Current SCXML states not matching expected states.\n"
                    + "States  : " + stateIds + "\n"
                    + "expected: " + states);
        }
    }

    protected static void assertMatchingHints(Map<String, Serializable> hints, Map<String, Serializable> expected) {
        if (!hints.equals(expected)) {
            Assert.fail("Hints map does not match expected map.\n"
                    +"Hint map: "+sortMap(hints)+"\n"
                    +"expected: "+sortMap(expected));
        }
    }

    @Test
    public void testInitializeSCXML() throws Exception {

        MockAccessManagedSession session = new MockAccessManagedSession(MockNode.root());
        MockWorkflowContext workflowContext = new MockWorkflowContext("testuser", session);
        DocumentWorkflowImpl wf = new DocumentWorkflowImpl();
        wf.setWorkflowContext(workflowContext);

        MockNode handleNode = (MockNode)session.getRootNode().addNode("test", HippoNodeType.NT_HANDLE);

        addVariant(handleNode, HippoStdNodeType.DRAFT);
        wf.setNode(handleNode);
        wf.getWorkflowExecutor().start();
    }

    @Test
    public void testVariants() throws Exception {

        MockAccessManagedSession session = new MockAccessManagedSession(MockNode.root());
        MockWorkflowContext workflowContext = new MockWorkflowContext("testuser", session);
        MockNode handleNode = (MockNode)session.getRootNode().addNode("test", HippoNodeType.NT_HANDLE);
        DocumentWorkflowImpl wf = new DocumentWorkflowImpl();
        wf.setWorkflowContext(workflowContext);
        wf.setNode(handleNode);

        // no document
        assertMatchingHints(wf.hints(), HintsBuilder.build()
                        .hints()
        );
        assertMatchingSCXMLStates(wf.getWorkflowExecutor(), StatesBuilder.build()
                        .noDocument()
                        .states()
        );

        MockNode publishedVariant = addVariant(handleNode, HippoStdNodeType.PUBLISHED);
        // set (only) author permission
        session.setPermissions(publishedVariant.getPath(), "hippo:author", true);

        // only published
        assertMatchingHints(wf.hints(), HintsBuilder.build()
                        .status(true).isLive(true).previewAvailable(true).checkModified(false).noEdit().editable()
                        .requestPublication(false).requestDepublication(true).listVersions().requestDelete(false)
                        .hints()
        );
        assertMatchingSCXMLStates(wf.getWorkflowExecutor(), StatesBuilder.build()
                        .status().logEvent().editable().noRequest().noPublish().depublishable().noVersioning().noTerminate().noCopy()
                        .states()
        );

        MockNode unpublishedVariant = addVariant(handleNode, HippoStdNodeType.UNPUBLISHED);
        // set (only) author permission
        session.setPermissions(unpublishedVariant.getPath(), "hippo:author", true);

        // unpublished + published
        assertMatchingHints(wf.hints(), HintsBuilder.build()
                        .status(true).isLive(true).previewAvailable(true).checkModified(false).noEdit().editable()
                        .requestPublication(false).requestDepublication(true).listVersions().retrieveVersion().requestDelete(false)
                        .hints()
        );
        assertMatchingSCXMLStates(wf.getWorkflowExecutor(), StatesBuilder.build()
                        .status().logEvent().editable().noRequest().noPublish().depublishable().versionable().noTerminate().noCopy()
                        .states()
        );

        MockNode draftVariant = addVariant(handleNode, HippoStdNodeType.DRAFT);
        // set (only) author permission
        session.setPermissions(draftVariant.getPath(), "hippo:author", true);

        // draft + unpublished + published
        assertMatchingHints(wf.hints(), HintsBuilder.build()
                        .status(true).isLive(true).previewAvailable(true).checkModified(true).noEdit().editable()
                        .requestPublication(false).requestDepublication(true).listVersions().retrieveVersion().requestDelete(false)
                        .hints()
        );
        assertMatchingSCXMLStates(wf.getWorkflowExecutor(), StatesBuilder.build()
                        .status().logEvent().editable().noRequest().noPublish().depublishable().versionable().noTerminate().noCopy()
                        .states()
        );

        publishedVariant.remove();

        // draft + unpublished
        assertMatchingHints(wf.hints(), HintsBuilder.build()
                        .status(true).isLive(false).previewAvailable(true).checkModified(true).noEdit().editable()
                        .requestPublication(true).requestDepublication(false).listVersions().retrieveVersion().requestDelete(true)
                        .hints()
        );
        assertMatchingSCXMLStates(wf.getWorkflowExecutor(), StatesBuilder.build()
                        .status().logEvent().editable().noRequest().publishable().noDepublish().versionable().terminateable().noCopy()
                        .states()
        );

        draftVariant.remove();

        // unpublished
        assertMatchingHints(wf.hints(), HintsBuilder.build()
                        .status(true).isLive(false).previewAvailable(true).checkModified(false).noEdit().editable()
                        .requestPublication(true).requestDepublication(false).listVersions().retrieveVersion().requestDelete(true)
                        .hints()
        );
        assertMatchingSCXMLStates(wf.getWorkflowExecutor(), StatesBuilder.build()
                        .status().logEvent().editable().noRequest().publishable().noDepublish().versionable().terminateable().noCopy()
                        .states()
        );

        unpublishedVariant.remove();
        addVariant(handleNode, HippoStdNodeType.DRAFT);
        // set (only) author permission
        session.setPermissions(draftVariant.getPath(), "hippo:author", true);

        // draft
        assertMatchingHints(wf.hints(), HintsBuilder.build()
                        .status(true).isLive(false).previewAvailable(false).checkModified(false).noEdit().editable()
                        .requestPublication(false).requestDepublication(false).listVersions().requestDelete(true)
                        .hints()
        );
        assertMatchingSCXMLStates(wf.getWorkflowExecutor(), StatesBuilder.build()
                        .status().logEvent().editable().noRequest().noPublish().noDepublish().noVersioning().terminateable().noCopy()
                        .states()
        );

        addVariant(handleNode, HippoStdNodeType.PUBLISHED);
        // set (only) author permission
        session.setPermissions(publishedVariant.getPath(), "hippo:author", true);

        // draft + published
        assertMatchingHints(wf.hints(), HintsBuilder.build()
                        .status(true).isLive(true).previewAvailable(true).checkModified(false).noEdit().editable()
                        .requestPublication(false).requestDepublication(true).listVersions().requestDelete(false)
                        .hints()
        );
        assertMatchingSCXMLStates(wf.getWorkflowExecutor(), StatesBuilder.build()
                        .status().logEvent().editable().noRequest().noPublish().depublishable().noVersioning().noTerminate().noCopy()
                        .states()
        );
    }

    @Test
    public void testStatusState() throws Exception {

        MockAccessManagedSession session = new MockAccessManagedSession(MockNode.root());
        MockWorkflowContext workflowContext = new MockWorkflowContext("testuser", session);
        MockNode handleNode = (MockNode)session.getRootNode().addNode("test", HippoNodeType.NT_HANDLE);
        DocumentWorkflowImpl wf = new DocumentWorkflowImpl();
        wf.setWorkflowContext(workflowContext);
        wf.setNode(handleNode);

        MockNode draftVariant = addVariant(handleNode, HippoStdNodeType.DRAFT);
        // set (only) author permission
        session.setPermissions(draftVariant.getPath(), "hippo:author", true);

        // status=true (no holder)
        assertMatchingHints(wf.hints(), HintsBuilder.build()
                        .status(true).isLive(false).previewAvailable(false).checkModified(false).noEdit().editable()
                        .requestPublication(false).requestDepublication(false).listVersions().requestDelete(true)
                        .hints()
        );

        draftVariant.setProperty(HippoStdNodeType.HIPPOSTD_HOLDER, "testuser");

        // status=true (holder == editor)
        assertMatchingHints(wf.hints(), HintsBuilder.build()
                        .status(true).isLive(false).previewAvailable(false).checkModified(false).editing()
                        .requestPublication(false).requestDepublication(false).listVersions().requestDelete(false)
                        .hints()
        );

        draftVariant.setProperty(HippoStdNodeType.HIPPOSTD_HOLDER, "otheruser");

        // status=false (holder != editor, inUseBy=otheruser)
        assertMatchingHints(wf.hints(), HintsBuilder.build()
                        .status(false).isLive(false).previewAvailable(false).checkModified(false).noEdit().inUseBy("otheruser")
                        .requestPublication(false).requestDepublication(false).listVersions().requestDelete(false)
                        .hints()
        );

        MockNode publishedVariant = addVariant(handleNode, HippoStdNodeType.PUBLISHED);
        // set (only) author permission
        session.setPermissions(publishedVariant.getPath(), "hippo:author", true);

        // no/empty availability published: live + preview
        assertMatchingHints(wf.hints(), HintsBuilder.build()
                        .status(false).isLive(true).previewAvailable(true).checkModified(false).noEdit().inUseBy("otheruser")
                        .requestPublication(false).requestDepublication(false).listVersions().requestDelete(false)
                        .hints()
        );

        publishedVariant.setProperty(HippoNodeType.HIPPO_AVAILABILITY, new String[]{"foo"});

        // no live|preview availability on published
        assertMatchingHints(wf.hints(), HintsBuilder.build()
                        .status(false).isLive(false).previewAvailable(false).checkModified(false).noEdit().inUseBy("otheruser")
                        .requestPublication(false).requestDepublication(false).listVersions().requestDelete(false)
                        .hints()
        );

        publishedVariant.setProperty(HippoNodeType.HIPPO_AVAILABILITY, new String[]{"live"});

        // live availability on published
        assertMatchingHints(wf.hints(), HintsBuilder.build()
                        .status(false).isLive(true).previewAvailable(false).checkModified(false).noEdit().inUseBy("otheruser")
                        .requestPublication(false).requestDepublication(false).listVersions().requestDelete(false)
                        .hints()
        );

        publishedVariant.setProperty(HippoNodeType.HIPPO_AVAILABILITY, new String[]{"preview", "live"});

        // live|preview availability on published
        assertMatchingHints(wf.hints(), HintsBuilder.build()
                        .status(false).isLive(true).previewAvailable(true).checkModified(false).noEdit().inUseBy("otheruser")
                        .requestPublication(false).requestDepublication(false).listVersions().requestDelete(false)
                        .hints()
        );

        MockNode unpublishedVariant = addVariant(handleNode, HippoStdNodeType.UNPUBLISHED);
        // set (only) author permission
        session.setPermissions(unpublishedVariant.getPath(), "hippo:author", true);
        unpublishedVariant.setProperty(HippoNodeType.HIPPO_AVAILABILITY, new String[]{"preview"});
        publishedVariant.setProperty(HippoNodeType.HIPPO_AVAILABILITY, new String[]{"live"});

        // live availability on published, preview availability on unpublished
        assertMatchingHints(wf.hints(), HintsBuilder.build()
                        .status(false).isLive(true).previewAvailable(true).checkModified(true).noEdit().inUseBy("otheruser")
                        .requestPublication(false).requestDepublication(false).listVersions().retrieveVersion().requestDelete(false)
                        .hints()
        );

        publishedVariant.remove();

        // only preview availability on unpublished
        assertMatchingHints(wf.hints(), HintsBuilder.build()
                        .status(false).isLive(false).previewAvailable(true).checkModified(true).noEdit().inUseBy("otheruser")
                        .requestPublication(false).requestDepublication(false).listVersions().retrieveVersion().requestDelete(false)
                        .hints()
        );
    }

    @Test
    public void testEditState() throws Exception {

        MockAccessManagedSession session = new MockAccessManagedSession(MockNode.root());
        MockWorkflowContext workflowContext = new MockWorkflowContext("testuser", session);
        MockNode handleNode = (MockNode)session.getRootNode().addNode("test", HippoNodeType.NT_HANDLE);
        DocumentWorkflowImpl wf = new DocumentWorkflowImpl();
        wf.setWorkflowContext(workflowContext);
        wf.setNode(handleNode);

        MockNode draftVariant = addVariant(handleNode, HippoStdNodeType.DRAFT);
        // set (only) author permission
        session.setPermissions(draftVariant.getPath(), "hippo:author", true);

        // not editing, no request pending: editable
        assertMatchingHints(wf.hints(), HintsBuilder.build()
                        .status(true).isLive(false).previewAvailable(false).checkModified(false).noEdit().editable()
                        .requestPublication(false).requestDepublication(false).listVersions().requestDelete(true)
                        .hints()
        );
        assertMatchingSCXMLStates(wf.getWorkflowExecutor(), StatesBuilder.build()
                        .status().logEvent().editable().noRequest().noPublish().noDepublish().noVersioning().terminateable().noCopy()
                        .states()
        );

        draftVariant.setProperty(HippoStdNodeType.HIPPOSTD_HOLDER, "otheruser");

        // editing, no request pending: editing
        assertMatchingHints(wf.hints(), HintsBuilder.build()
                        .status(false).isLive(false).previewAvailable(false).checkModified(false).noEdit().inUseBy("otheruser")
                        .requestPublication(false).requestDepublication(false).listVersions().requestDelete(false)
                        .hints()
        );
        assertMatchingSCXMLStates(wf.getWorkflowExecutor(), StatesBuilder.build()
                        .status().logEvent().editing().noRequest().noPublish().noDepublish().noVersioning().noTerminate().noCopy()
                        .states()
        );

        session.setPermissions(draftVariant.getPath(), "hippo:admin", true);

        // editing, no request pending, admin: editing, unlock
        assertMatchingHints(wf.hints(), HintsBuilder.build()
                        .status(false).isLive(false).previewAvailable(false).checkModified(false).noEdit().inUseBy("otheruser").unlock(true)
                        .requestPublication(false).requestDepublication(false).listVersions().requestDelete(false)
                        .hints()
        );
        assertMatchingSCXMLStates(wf.getWorkflowExecutor(), StatesBuilder.build()
                        .status().logEvent().editing().noRequest().noPublish().noDepublish().noVersioning().noTerminate().noCopy()
                        .states()
        );

        draftVariant.setProperty(HippoStdNodeType.HIPPOSTD_HOLDER, "testuser");

        // editor, no request pending (, admin): editing, edit
        assertMatchingHints(wf.hints(), HintsBuilder.build()
                        .status(true).isLive(false).previewAvailable(false).checkModified(false).editing()
                        .requestPublication(false).requestDepublication(false).listVersions().requestDelete(false)
                        .hints()
        );
        assertMatchingSCXMLStates(wf.getWorkflowExecutor(), StatesBuilder.build()
                        .status().logEvent().editing().noRequest().noPublish().noDepublish().noVersioning().noTerminate().noCopy()
                        .states()
        );

        draftVariant.getProperty(HippoStdNodeType.HIPPOSTD_HOLDER).remove();
        MockNode rejectedRequest = addRequest(handleNode, HippoStdPubWfNodeType.REJECTED, true);

        // not editing, request rejected, admin: editable, unlock=false
        assertMatchingHints(wf.hints(), HintsBuilder.build()
                        .status(true).isLive(false).previewAvailable(false).checkModified(false).noEdit().editable().unlock(false)
                        .cancelRequest(rejectedRequest.getIdentifier())
                        .requestPublication(false).requestDepublication(false).listVersions().requestDelete(true)
                        .hints()
        );
        assertMatchingSCXMLStates(wf.getWorkflowExecutor(), StatesBuilder.build()
                        .status().logEvent().editable().requested().noPublish().noDepublish().noVersioning().terminateable().noCopy()
                        .states()
        );

        session.setPermissions(draftVariant.getPath(), "hippo:author", true);

        MockNode publishRequest = addRequest(handleNode, HippoStdPubWfNodeType.PUBLISH, true);

        // not editing, publish request: no-edit
        assertMatchingHints(wf.hints(), HintsBuilder.build()
                        .status(true).isLive(false).previewAvailable(false).checkModified(false).noEdit()
                        .cancelRequest(rejectedRequest.getIdentifier())
                        .acceptRequest(publishRequest.getIdentifier(), false)
                        .cancelRequest(publishRequest.getIdentifier())
                        .rejectRequest(publishRequest.getIdentifier())
                        .requestPublication(false).requestDepublication(false).listVersions().requestDelete(false)
                        .hints()
        );
        assertMatchingSCXMLStates(wf.getWorkflowExecutor(), StatesBuilder.build()
                        .status().logEvent().noEdit().requested().noPublish().noDepublish().noVersioning().terminateable().noCopy()
                        .states()
        );
    }

    @Test
    public void testRequestState() throws Exception {
        MockAccessManagedSession session = new MockAccessManagedSession(MockNode.root());
        MockWorkflowContext workflowContext = new MockWorkflowContext("testuser", session);
        MockNode handleNode = (MockNode)session.getRootNode().addNode("test", HippoNodeType.NT_HANDLE);
        DocumentWorkflowImpl wf = new DocumentWorkflowImpl();
        wf.setWorkflowContext(workflowContext);
        wf.setNode(handleNode);

        // test state not-requested
        MockNode unpublishedVariant = addVariant(handleNode, HippoStdNodeType.UNPUBLISHED);
        session.setPermissions(unpublishedVariant.getPath(), "hippo:author", true);

        // no-request
        assertMatchingHints(wf.hints(), HintsBuilder.build()
                        .status(true).isLive(false).previewAvailable(true).checkModified(false).noEdit().editable()
                        .requestPublication(true).requestDepublication(false).listVersions().retrieveVersion().requestDelete(true)
                        .hints()
        );
        assertMatchingSCXMLStates(wf.getWorkflowExecutor(), StatesBuilder.build()
                        .status().logEvent().editable().noRequest().publishable().noDepublish().versionable().terminateable().noCopy()
                        .states()
        );

        MockNode publishRequest = addRequest(handleNode, HippoStdPubWfNodeType.PUBLISH, true);
        publishRequest.setProperty(HippoStdPubWfNodeType.HIPPOSTDPUBWF_USERNAME, "testuser");
        session.setPermissions(publishRequest.getPath(), "hippo:author", true);

        // author user publish request: cancelRequest=true
        assertMatchingHints(wf.hints(), HintsBuilder.build()
                        .status(true).isLive(false).previewAvailable(true).checkModified(false).noEdit()
                        .cancelRequest(publishRequest.getIdentifier())
                        .requestPublication(false).requestDepublication(false).listVersions().retrieveVersion().requestDelete(false)
                        .hints()
        );
        assertMatchingSCXMLStates(wf.getWorkflowExecutor(), StatesBuilder.build()
                        .status().logEvent().noEdit().requested().publishable().noDepublish().versionable().terminateable().noCopy()
                        .states()
        );

        publishRequest.setProperty(HippoStdPubWfNodeType.HIPPOSTDPUBWF_USERNAME, "otheruser");

        // author and other user publish request: no-op
        assertMatchingHints(wf.hints(), HintsBuilder.build()
                        .status(true).isLive(false).previewAvailable(true).checkModified(false).noEdit()
                        .requestPublication(false).requestDepublication(false).listVersions().retrieveVersion().requestDelete(false)
                        .hints()
        );
        assertMatchingSCXMLStates(wf.getWorkflowExecutor(), StatesBuilder.build()
                        .status().logEvent().noEdit().requested().publishable().noDepublish().versionable().terminateable().noCopy()
                        .states()
        );

        session.setPermissions(publishRequest.getPath(), "hippo:editor", true);

        // editor other user publish request: acceptRequest=true,rejectRequest=true
        assertMatchingHints(wf.hints(), HintsBuilder.build()
                        .status(true).isLive(false).previewAvailable(true).checkModified(false).noEdit()
                        .acceptRequest(publishRequest.getIdentifier(), true)
                        .rejectRequest(publishRequest.getIdentifier())
                        .requestPublication(false).requestDepublication(false).listVersions().retrieveVersion().requestDelete(false)
                        .hints()
        );
        assertMatchingSCXMLStates(wf.getWorkflowExecutor(), StatesBuilder.build()
                        .status().logEvent().noEdit().requested().publishable().noDepublish().versionable().terminateable().noCopy()
                        .states()
        );

        MockNode publishedVariant = addVariant(handleNode, HippoStdNodeType.PUBLISHED);
        session.setPermissions(publishedVariant.getPath(), "hippo:author", true);

        // editor other user publish request but unmodified: acceptRequest=false,rejectRequest=true
        assertMatchingHints(wf.hints(), HintsBuilder.build()
                        .status(true).isLive(true).previewAvailable(true).checkModified(false).noEdit()
                        .acceptRequest(publishRequest.getIdentifier(), false)
                        .rejectRequest(publishRequest.getIdentifier())
                        .requestPublication(false).requestDepublication(false).listVersions().retrieveVersion().requestDelete(false)
                        .hints()
        );
        assertMatchingSCXMLStates(wf.getWorkflowExecutor(), StatesBuilder.build()
                        .status().logEvent().noEdit().requested().noPublish().depublishable().versionable().noTerminate().noCopy()
                        .states()
        );

        publishRequest.setProperty(HippoStdPubWfNodeType.HIPPOSTDPUBWF_USERNAME, "testuser");

        // editor own publish request but unmodified: acceptRequest=false,rejectRequest=true,cancelRequest=true
        assertMatchingHints(wf.hints(), HintsBuilder.build()
                        .status(true).isLive(true).previewAvailable(true).checkModified(false).noEdit()
                        .acceptRequest(publishRequest.getIdentifier(), false)
                        .rejectRequest(publishRequest.getIdentifier())
                        .cancelRequest(publishRequest.getIdentifier())
                        .requestPublication(false).requestDepublication(false).listVersions().retrieveVersion().requestDelete(false)
                        .hints()
        );
        assertMatchingSCXMLStates(wf.getWorkflowExecutor(), StatesBuilder.build()
                        .status().logEvent().noEdit().requested().noPublish().depublishable().versionable().noTerminate().noCopy()
                        .states()
        );

        publishRequest.remove();

        MockNode depublishRequest = addRequest(handleNode, HippoStdPubWfNodeType.DEPUBLISH, true);
        depublishRequest.setProperty(HippoStdPubWfNodeType.HIPPOSTDPUBWF_USERNAME, "testuser");
        session.setPermissions(depublishRequest.getPath(), "hippo:editor", true);

        // editor own depublish request live: acceptRequest=true,rejectRequest=true,cancelRequest=true
        assertMatchingHints(wf.hints(), HintsBuilder.build()
                        .status(true).isLive(true).previewAvailable(true).checkModified(false).noEdit()
                        .acceptRequest(depublishRequest.getIdentifier(), true)
                        .rejectRequest(depublishRequest.getIdentifier())
                        .cancelRequest(depublishRequest.getIdentifier())
                        .requestPublication(false).requestDepublication(false).listVersions().retrieveVersion().requestDelete(false)
                        .hints()
        );
        assertMatchingSCXMLStates(wf.getWorkflowExecutor(), StatesBuilder.build()
                        .status().logEvent().noEdit().requested().noPublish().depublishable().versionable().noTerminate().noCopy()
                        .states()
        );

        publishedVariant.setProperty(HippoNodeType.HIPPO_AVAILABILITY, new String[]{"preview"});

        // editor own depublish request !live: acceptRequest=false,rejectRequest=true,cancelRequest=true
        assertMatchingHints(wf.hints(), HintsBuilder.build()
                        .status(true).isLive(false).previewAvailable(true).checkModified(false).noEdit()
                        .acceptRequest(depublishRequest.getIdentifier(), false)
                        .rejectRequest(depublishRequest.getIdentifier())
                        .cancelRequest(depublishRequest.getIdentifier())
                        .requestPublication(false).requestDepublication(false).listVersions().retrieveVersion().requestDelete(false)
                        .hints()
        );
        assertMatchingSCXMLStates(wf.getWorkflowExecutor(), StatesBuilder.build()
                        .status().logEvent().noEdit().requested().publishable().noDepublish().versionable().terminateable().noCopy()
                        .states()
        );

        depublishRequest.remove();

        MockNode deleteRequest = addRequest(handleNode, HippoStdPubWfNodeType.DELETE, true);
        deleteRequest.setProperty(HippoStdPubWfNodeType.HIPPOSTDPUBWF_USERNAME, "testuser");
        session.setPermissions(deleteRequest.getPath(), "hippo:editor", true);

        // editor own delete request !live: acceptRequest=true,rejectRequest=true,cancelRequest=true
        assertMatchingHints(wf.hints(), HintsBuilder.build()
                        .status(true).isLive(false).previewAvailable(true).checkModified(false).noEdit()
                        .acceptRequest(deleteRequest.getIdentifier(), true)
                        .rejectRequest(deleteRequest.getIdentifier())
                        .cancelRequest(deleteRequest.getIdentifier())
                        .requestPublication(false).requestDepublication(false).listVersions().retrieveVersion().requestDelete(false)
                        .hints()
        );
        assertMatchingSCXMLStates(wf.getWorkflowExecutor(), StatesBuilder.build()
                        .status().logEvent().noEdit().requested().publishable().noDepublish().versionable().terminateable().noCopy()
                        .states()
        );

        publishedVariant.setProperty(HippoNodeType.HIPPO_AVAILABILITY, new String[]{"preview","live"});

        // editor own delete request !live: acceptRequest=false,rejectRequest=true,cancelRequest=true
        assertMatchingHints(wf.hints(), HintsBuilder.build()
                        .status(true).isLive(true).previewAvailable(true).checkModified(false).noEdit()
                        .acceptRequest(deleteRequest.getIdentifier(), false)
                        .rejectRequest(deleteRequest.getIdentifier())
                        .cancelRequest(deleteRequest.getIdentifier())
                        .requestPublication(false).requestDepublication(false).listVersions().retrieveVersion().requestDelete(false)
                        .hints()
        );
        assertMatchingSCXMLStates(wf.getWorkflowExecutor(), StatesBuilder.build()
                        .status().logEvent().noEdit().requested().noPublish().depublishable().versionable().noTerminate().noCopy()
                        .states()
        );

        deleteRequest.remove();
        publishedVariant.remove();

        MockNode rejectedRequest = addRequest(handleNode, HippoStdPubWfNodeType.REJECTED, true);
        rejectedRequest.setProperty(HippoStdPubWfNodeType.HIPPOSTDPUBWF_USERNAME, "otheruser");
        session.setPermissions(rejectedRequest.getPath(), "hippo:editor", true);

        // editor other user rejected request: no-op
        assertMatchingHints(wf.hints(), HintsBuilder.build()
                        .status(true).isLive(false).previewAvailable(true).checkModified(false).noEdit().editable()
                        .requestPublication(true).requestDepublication(false).listVersions().retrieveVersion().requestDelete(true)
                        .hints()
        );
        assertMatchingSCXMLStates(wf.getWorkflowExecutor(), StatesBuilder.build()
                        .status().logEvent().editable().requested().publishable().noDepublish().versionable().terminateable().noCopy()
                        .states()
        );

        rejectedRequest.setProperty(HippoStdPubWfNodeType.HIPPOSTDPUBWF_USERNAME, "testuser");

        // editor own rejected request: cancelRequest=true
        assertMatchingHints(wf.hints(), HintsBuilder.build()
                        .status(true).isLive(false).previewAvailable(true).checkModified(false).noEdit().editable()
                        .cancelRequest(rejectedRequest.getIdentifier())
                        .requestPublication(true).requestDepublication(false).listVersions().retrieveVersion().requestDelete(true)
                        .hints()
        );
        assertMatchingSCXMLStates(wf.getWorkflowExecutor(), StatesBuilder.build()
                        .status().logEvent().editable().requested().publishable().noDepublish().versionable().terminateable().noCopy()
                        .states()
        );

        session.setPermissions(rejectedRequest.getPath(), "hippo:author", true);

        // author own rejected request: cancelRequest=true
        assertMatchingHints(wf.hints(), HintsBuilder.build()
                        .status(true).isLive(false).previewAvailable(true).checkModified(false).noEdit().editable()
                        .cancelRequest(rejectedRequest.getIdentifier())
                        .requestPublication(true).requestDepublication(false).listVersions().retrieveVersion().requestDelete(true)
                        .hints()
        );
        assertMatchingSCXMLStates(wf.getWorkflowExecutor(), StatesBuilder.build()
                        .status().logEvent().editable().requested().publishable().noDepublish().versionable().terminateable().noCopy()
                        .states()
        );

        rejectedRequest.setProperty(HippoStdPubWfNodeType.HIPPOSTDPUBWF_USERNAME, "otheruser");

        // author other user rejected request: no-op
        assertMatchingHints(wf.hints(), HintsBuilder.build()
                        .status(true).isLive(false).previewAvailable(true).checkModified(false).noEdit().editable()
                        .requestPublication(true).requestDepublication(false).listVersions().retrieveVersion().requestDelete(true)
                        .hints()
        );
        assertMatchingSCXMLStates(wf.getWorkflowExecutor(), StatesBuilder.build()
                        .status().logEvent().editable().requested().publishable().noDepublish().versionable().terminateable().noCopy()
                        .states()
        );

        rejectedRequest.remove();

        MockNode scheduledRequest = addRequest(handleNode, HippoStdPubWfNodeType.PUBLISH, false);
        session.setPermissions(scheduledRequest.getPath(), "hippo:editor", true);

        // editor scheduled request: cancelRequest=true
        assertMatchingHints(wf.hints(), HintsBuilder.build()
                        .status(true).isLive(false).previewAvailable(true).checkModified(false).noEdit()
                        .cancelRequest(scheduledRequest.getIdentifier())
                        .requestPublication(false).requestDepublication(false).listVersions().retrieveVersion().requestDelete(false)
                        .hints()
        );
        assertMatchingSCXMLStates(wf.getWorkflowExecutor(), StatesBuilder.build()
                        .status().logEvent().noEdit().requested().publishable().noDepublish().versionable().terminateable().noCopy()
                        .states()
        );

        session.setPermissions(scheduledRequest.getPath(), "hippo:author", true);

        // author scheduled request: no-op
        assertMatchingHints(wf.hints(), HintsBuilder.build()
                        .status(true).isLive(false).previewAvailable(true).checkModified(false).noEdit()
                        .requestPublication(false).requestDepublication(false).listVersions().retrieveVersion().requestDelete(false)
                        .hints()
        );
        assertMatchingSCXMLStates(wf.getWorkflowExecutor(), StatesBuilder.build()
                        .status().logEvent().noEdit().requested().publishable().noDepublish().versionable().terminateable().noCopy()
                        .states()
        );
    }

    @Test
    public void testPublishState() throws Exception {
        MockAccessManagedSession session = new MockAccessManagedSession(MockNode.root());
        MockNode handleNode = (MockNode)session.getRootNode().addNode("test", HippoNodeType.NT_HANDLE);
        MockWorkflowContext workflowContext = new MockWorkflowContext("testuser", session);
        DocumentWorkflowImpl wf = new DocumentWorkflowImpl();
        wf.setWorkflowContext(workflowContext);
        wf.setNode(handleNode);

        MockNode draftVariant = addVariant(handleNode, HippoStdNodeType.DRAFT);
        session.setPermissions(draftVariant.getPath(), "hippo:author", true);
        draftVariant.setProperty(HippoStdNodeType.HIPPOSTD_HOLDER, "testuser");

        MockNode unpublishedVariant = addVariant(handleNode, HippoStdNodeType.UNPUBLISHED);
        session.setPermissions(unpublishedVariant.getPath(), "hippo:author", true);

        // author, no request, editing, modified (unpublished): requestPublication=false
        assertMatchingHints(wf.hints(), HintsBuilder.build()
                        .status(true).isLive(false).previewAvailable(true).checkModified(true).editing()
                        .requestPublication(false).requestDepublication(false).listVersions().retrieveVersion().requestDelete(false)
                        .hints()
        );
        assertMatchingSCXMLStates(wf.getWorkflowExecutor(), StatesBuilder.build()
                        .status().logEvent().editing().noRequest().noPublish().noDepublish().versionable().noTerminate().noCopy()
                        .states()
        );

        session.setPermissions(unpublishedVariant.getPath(), "hippo:editor", true);

        // editor, no request, editing, modified (unpublished): requestPublication=false,publish=false
        assertMatchingHints(wf.hints(), HintsBuilder.build()
                        .status(true).isLive(false).previewAvailable(true).checkModified(true).editing()
                        .requestPublication(false).publish(false).requestDepublication(false).depublish(false)
                        .listVersions().retrieveVersion().versionable().requestDelete(false).terminateable(false).copy()
                        .hints()
        );
        assertMatchingSCXMLStates(wf.getWorkflowExecutor(), StatesBuilder.build()
                        .status().logEvent().editing().noRequest().noPublish().noDepublish().versionable().noTerminate().copyable()
                        .states()
        );

        MockNode publishedVariant = addVariant(handleNode, HippoStdNodeType.PUBLISHED);
        session.setPermissions(publishedVariant.getPath(), "hippo:author", true);
        draftVariant.getProperty(HippoStdNodeType.HIPPOSTD_HOLDER).remove();

        // editor, no request, !editing, !modified (unpublished==published): requestPublication=false,publish=false
        assertMatchingHints(wf.hints(), HintsBuilder.build()
                        .status(true).isLive(true).previewAvailable(true).checkModified(true).noEdit().editable()
                        .requestPublication(false).publish(false).requestDepublication(true)
                        .listVersions().retrieveVersion().versionable().requestDelete(false).terminateable(false)
                        .hints()
        );
        assertMatchingSCXMLStates(wf.getWorkflowExecutor(), StatesBuilder.build()
                        .status().logEvent().editable().noRequest().noPublish().depublishable().versionable().noTerminate().noCopy()
                        .states()
        );

        // make unpublished modified
        Calendar publishedModified = Calendar.getInstance();
        Calendar unpublishedModified = Calendar.getInstance();
        publishedModified.setTimeInMillis(unpublishedModified.getTimeInMillis() - 1000);
        unpublishedVariant.setProperty(HippoStdPubWfNodeType.HIPPOSTDPUBWF_LAST_MODIFIED_DATE, new MockValue(PropertyType.DATE, ISO8601.format(unpublishedModified)));
        publishedVariant.setProperty(HippoStdPubWfNodeType.HIPPOSTDPUBWF_LAST_MODIFIED_DATE, new MockValue(PropertyType.DATE, ISO8601.format(publishedModified)));

        // editor, no request, !editing, modified (unpublished!=published): requestPublication=true,publish=true
        assertMatchingHints(wf.hints(), HintsBuilder.build()
                        .status(true).isLive(true).previewAvailable(true).checkModified(true).noEdit().editable()
                        .requestPublication(true).publish(true).requestDepublication(true)
                        .listVersions().retrieveVersion().versionable().requestDelete(false).terminateable(false)
                        .hints()
        );
        assertMatchingSCXMLStates(wf.getWorkflowExecutor(), StatesBuilder.build()
                        .status().logEvent().editable().noRequest().publishable().depublishable().versionable().noTerminate().noCopy()
                        .states()
        );

        session.setPermissions(unpublishedVariant.getPath(), "hippo:author", true);

        // author, no request, !editing, modified (unpublished!=published): requestPublication=true
        assertMatchingHints(wf.hints(), HintsBuilder.build()
                        .status(true).isLive(true).previewAvailable(true).checkModified(true).noEdit().editable()
                        .requestPublication(true).requestDepublication(true)
                        .listVersions().retrieveVersion().requestDelete(false)
                        .hints()
        );
        assertMatchingSCXMLStates(wf.getWorkflowExecutor(), StatesBuilder.build()
                        .status().logEvent().editable().noRequest().publishable().depublishable().versionable().noTerminate().noCopy()
                        .states()
        );

        MockNode scheduledRequest = addRequest(handleNode, HippoStdPubWfNodeType.PUBLISH, false);
        session.setPermissions(scheduledRequest.getPath(), "hippo:author", true);

        // author, request, !editing, modified (unpublished!=published): requestPublication=false
        assertMatchingHints(wf.hints(), HintsBuilder.build()
                        .status(true).isLive(true).previewAvailable(true).checkModified(true).noEdit()
                        .requestPublication(false).requestDepublication(false)
                        .listVersions().retrieveVersion().requestDelete(false)
                        .hints()
        );
        assertMatchingSCXMLStates(wf.getWorkflowExecutor(), StatesBuilder.build()
                        .status().logEvent().noEdit().requested().publishable().depublishable().versionable().noTerminate().noCopy()
                        .states()
        );

        workflowContext.setUserIdentity("system");
        session.setPermissions(unpublishedVariant.getPath(), "hippo:editor", true);

        // system, request, !editing, modified (unpublished!=published): requestPublication=true,publish=true
        assertMatchingHints(wf.hints(), HintsBuilder.build()
                        .status(true).isLive(true).previewAvailable(true).checkModified(true).noEdit()
                        .requestPublication(true).publish(true).requestDepublication(true)
                        .listVersions().retrieveVersion().versionable().requestDelete(false).terminateable(false)
                        .hints()
        );
        assertMatchingSCXMLStates(wf.getWorkflowExecutor(), StatesBuilder.build()
                        .status().logEvent().noEdit().requested().publishable().depublishable().versionable().noTerminate().noCopy()
                        .states()
        );
    }

    @Test
    public void testDePublishState() throws Exception {
        MockAccessManagedSession session = new MockAccessManagedSession(MockNode.root());
        MockNode handleNode = (MockNode)session.getRootNode().addNode("test", HippoNodeType.NT_HANDLE);
        MockWorkflowContext workflowContext = new MockWorkflowContext("testuser", session);
        DocumentWorkflowImpl wf = new DocumentWorkflowImpl();
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
        assertMatchingHints(wf.hints(), HintsBuilder.build()
                        .status(true).isLive(true).previewAvailable(true).checkModified(true).editing()
                        .requestPublication(false).requestDepublication(false).listVersions().retrieveVersion().requestDelete(false)
                        .hints()
        );
        assertMatchingSCXMLStates(wf.getWorkflowExecutor(), StatesBuilder.build()
                        .status().logEvent().editing().noRequest().noPublish().noDepublish().versionable().noTerminate().noCopy()
                        .states()
        );

        session.setPermissions(publishedVariant.getPath(), "hippo:editor", true);

        // editor, no request, editing, live: requestDepublication=false,depublish=false
        assertMatchingHints(wf.hints(), HintsBuilder.build()
                        .status(true).isLive(true).previewAvailable(true).checkModified(true).editing()
                        .requestPublication(false).requestDepublication(false).depublish(false)
                        .listVersions().retrieveVersion().requestDelete(false).copy()
                        .hints()
        );
        assertMatchingSCXMLStates(wf.getWorkflowExecutor(), StatesBuilder.build()
                        .status().logEvent().editing().noRequest().noPublish().noDepublish().versionable().noTerminate().copyable()
                        .states()
        );

        draftVariant.getProperty(HippoStdNodeType.HIPPOSTD_HOLDER).remove();
        publishedVariant.setProperty(HippoNodeType.HIPPO_AVAILABILITY, new String[]{});

        // editor, no request, !editing, !live: requestDepublication=false,depublish=false
        assertMatchingHints(wf.hints(), HintsBuilder.build()
                        .status(true).isLive(false).previewAvailable(true).checkModified(true).noEdit().editable()
                        .requestPublication(true).requestDepublication(false).depublish(false)
                        .listVersions().retrieveVersion().requestDelete(true).copy()
                        .hints()
        );
        assertMatchingSCXMLStates(wf.getWorkflowExecutor(), StatesBuilder.build()
                        .status().logEvent().editable().noRequest().publishable().noDepublish().versionable().terminateable().copyable()
                        .states()
        );

        publishedVariant.setProperty(HippoNodeType.HIPPO_AVAILABILITY, new String[]{"live"});

        // editor, no request, !editing, live: requestDepublication=true,depublish=true
        assertMatchingHints(wf.hints(), HintsBuilder.build()
                        .status(true).isLive(true).previewAvailable(true).checkModified(true).noEdit().editable()
                        .requestPublication(false).requestDepublication(true).depublish(true)
                        .listVersions().retrieveVersion().requestDelete(false).copy()
                        .hints()
        );
        assertMatchingSCXMLStates(wf.getWorkflowExecutor(), StatesBuilder.build()
                        .status().logEvent().editable().noRequest().noPublish().depublishable().versionable().noTerminate().copyable()
                        .states()
        );

        session.setPermissions(publishedVariant.getPath(), "hippo:author", true);

        // author, no request, !editing, live: requestDepublication=true
        assertMatchingHints(wf.hints(), HintsBuilder.build()
                        .status(true).isLive(true).previewAvailable(true).checkModified(true).noEdit().editable()
                        .requestPublication(false).requestDepublication(true)
                        .listVersions().retrieveVersion().requestDelete(false)
                        .hints()
        );
        assertMatchingSCXMLStates(wf.getWorkflowExecutor(), StatesBuilder.build()
                        .status().logEvent().editable().noRequest().noPublish().depublishable().versionable().noTerminate().noCopy()
                        .states()
        );

        MockNode scheduledRequest = addRequest(handleNode, HippoStdPubWfNodeType.PUBLISH, false);
        session.setPermissions(scheduledRequest.getPath(), "hippo:author", true);

        // author, request, !editing, live: requestDepublication=false
        assertMatchingHints(wf.hints(), HintsBuilder.build()
                        .status(true).isLive(true).previewAvailable(true).checkModified(true).noEdit()
                        .requestPublication(false).requestDepublication(false)
                        .listVersions().retrieveVersion().requestDelete(false)
                        .hints()
        );
        assertMatchingSCXMLStates(wf.getWorkflowExecutor(), StatesBuilder.build()
                        .status().logEvent().noEdit().requested().noPublish().depublishable().versionable().noTerminate().noCopy()
                        .states()
        );

        workflowContext.setUserIdentity("system");
        session.setPermissions(publishedVariant.getPath(), "hippo:editor", true);

        // system, request, !editing, live: requestDepublication=true,depublish=true
        assertMatchingHints(wf.hints(), HintsBuilder.build()
                        .status(true).isLive(true).previewAvailable(true).checkModified(true).noEdit()
                        .requestPublication(false).requestDepublication(true).depublish(true)
                        .listVersions().retrieveVersion().requestDelete(false).copy()
                        .hints()
        );
        assertMatchingSCXMLStates(wf.getWorkflowExecutor(), StatesBuilder.build()
                        .status().logEvent().noEdit().requested().noPublish().depublishable().versionable().noTerminate().copyable()
                        .states()
        );
    }

    @Test
    public void testVersioningState() throws Exception {
        MockAccessManagedSession session = new MockAccessManagedSession(MockNode.root());
        MockNode handleNode = (MockNode)session.getRootNode().addNode("test", HippoNodeType.NT_HANDLE);
        MockWorkflowContext workflowContext = new MockWorkflowContext("testuser", session);
        DocumentWorkflowImpl wf = new DocumentWorkflowImpl();
        wf.setWorkflowContext(workflowContext);
        wf.setNode(handleNode);

        MockNode draftVariant = addVariant(handleNode, HippoStdNodeType.DRAFT);
        session.setPermissions(draftVariant.getPath(), "hippo:author", true);

        // no unpublished: (only) listVersions=true
        assertMatchingHints(wf.hints(), HintsBuilder.build()
                        .status(true).isLive(false).previewAvailable(false).checkModified(false).noEdit().editable()
                        .requestPublication(false).requestDepublication(false).listVersions().requestDelete(true)
                        .hints()
        );
        assertMatchingSCXMLStates(wf.getWorkflowExecutor(), StatesBuilder.build()
                        .status().logEvent().editable().noRequest().noPublish().noDepublish().noVersioning().terminateable().noCopy()
                        .states()
        );

        draftVariant.remove();
        MockNode unpublishedVariant = addVariant(handleNode, HippoStdNodeType.UNPUBLISHED);
        session.setPermissions(unpublishedVariant.getPath(), "hippo:author", true);

        // author, unpublished: listVersions=true, retrieveVersion=true
        assertMatchingHints(wf.hints(), HintsBuilder.build()
                        .status(true).isLive(false).previewAvailable(true).checkModified(false).noEdit().editable()
                        .requestPublication(true).requestDepublication(false).listVersions().retrieveVersion().requestDelete(true)
                        .hints()
        );
        assertMatchingSCXMLStates(wf.getWorkflowExecutor(), StatesBuilder.build()
                        .status().logEvent().editable().noRequest().publishable().noDepublish().versionable().terminateable().noCopy()
                        .states()
        );

        session.setPermissions(unpublishedVariant.getPath(), "hippo:editor", true);

        // author, unpublished: listVersions=true, retrieveVersion=true, versionable
        assertMatchingHints(wf.hints(), HintsBuilder.build()
                        .status(true).isLive(false).previewAvailable(true).checkModified(false).noEdit().editable()
                        .requestPublication(true).publish(true).requestDepublication(false).depublish(false)
                        .listVersions().retrieveVersion().versionable().requestDelete(true).terminateable(true).copy()
                        .hints()
        );
        assertMatchingSCXMLStates(wf.getWorkflowExecutor(), StatesBuilder.build()
                        .status().logEvent().editable().noRequest().publishable().noDepublish().versionable().terminateable().copyable()
                        .states()
        );
    }

    @Test
    public void testTerminateState() throws Exception {
        MockAccessManagedSession session = new MockAccessManagedSession(MockNode.root());
        MockNode folderNode = (MockNode)session.getRootNode().addNode("folder", HippoNodeType.NT_DOCUMENT);
        session.setPermissions(folderNode.getPath(), "jcr:write", false);
        MockNode handleNode = (MockNode)folderNode.addNode("test", HippoNodeType.NT_HANDLE);
        MockWorkflowContext workflowContext = new MockWorkflowContext("testuser", session);
        DocumentWorkflowImpl wf = new DocumentWorkflowImpl();
        wf.setWorkflowContext(workflowContext);
        wf.setNode(handleNode);

        MockNode draftVariant = addVariant(handleNode, HippoStdNodeType.DRAFT);
        session.setPermissions(draftVariant.getPath(), "hippo:author", true);
        draftVariant.setProperty(HippoStdNodeType.HIPPOSTD_HOLDER, "testuser");
        MockNode publishedVariant = addVariant(handleNode, HippoStdNodeType.PUBLISHED);
        session.setPermissions(publishedVariant.getPath(), "hippo:author", true);

        // author, editing, live: requestDelete=false
        assertMatchingHints(wf.hints(), HintsBuilder.build()
                        .status(true).isLive(true).previewAvailable(true).checkModified(false).editing()
                        .requestPublication(false).requestDepublication(false)
                        .listVersions().requestDelete(false)
                        .hints()
        );
        assertMatchingSCXMLStates(wf.getWorkflowExecutor(), StatesBuilder.build()
                        .status().logEvent().editing().noRequest().noPublish().noDepublish().noVersioning().noTerminate().noCopy()
                        .states()
        );

        session.setPermissions(publishedVariant.getPath(), "hippo:editor", true);

        // editor, editing, live: requestDelete=false
        assertMatchingHints(wf.hints(), HintsBuilder.build()
                        .status(true).isLive(true).previewAvailable(true).checkModified(false).editing()
                        .requestPublication(false).publish(false).requestDepublication(false).depublish(false)
                        .listVersions().requestDelete(false).copy()
                        .hints()
        );
        assertMatchingSCXMLStates(wf.getWorkflowExecutor(), StatesBuilder.build()
                        .status().logEvent().editing().noRequest().noPublish().noDepublish().noVersioning().noTerminate().copyable()
                        .states()
        );

        session.setPermissions(folderNode.getPath(), "jcr:write", true);

        // editor + writable containing folder, editing, live: requestDelete=false, terminateable=false
        assertMatchingHints(wf.hints(), HintsBuilder.build()
                        .status(true).isLive(true).previewAvailable(true).checkModified(false).editing()
                        .requestPublication(false).publish(false).requestDepublication(false).depublish(false)
                        .listVersions().requestDelete(false).terminateable(false).copy()
                        .hints()
        );
        assertMatchingSCXMLStates(wf.getWorkflowExecutor(), StatesBuilder.build()
                        .status().logEvent().editing().noRequest().noPublish().noDepublish().noVersioning().noTerminate().copyable()
                        .states()
        );

        session.setPermissions(publishedVariant.getPath(), "hippo:author", true);

        // author + writable containing folder, editing, live: requestDelete=false
        assertMatchingHints(wf.hints(), HintsBuilder.build()
                        .status(true).isLive(true).previewAvailable(true).checkModified(false).editing()
                        .requestPublication(false).requestDepublication(false)
                        .listVersions().requestDelete(false)
                        .hints()
        );
        assertMatchingSCXMLStates(wf.getWorkflowExecutor(), StatesBuilder.build()
                        .status().logEvent().editing().noRequest().noPublish().noDepublish().noVersioning().noTerminate().noCopy()
                        .states()
        );

        draftVariant.getProperty(HippoStdNodeType.HIPPOSTD_HOLDER).remove();
        publishedVariant.setProperty(HippoNodeType.HIPPO_AVAILABILITY, new String[0]);

        MockNode scheduledRequest = addRequest(handleNode, HippoStdPubWfNodeType.PUBLISH, false);

        // author + writable containing folder, !editing, !live, request: requestDelete=false
        assertMatchingHints(wf.hints(), HintsBuilder.build()
                        .status(true).isLive(false).previewAvailable(false).checkModified(false).noEdit()
                        .cancelRequest(scheduledRequest.getIdentifier())
                        .requestPublication(false).requestDepublication(false)
                        .listVersions().requestDelete(false)
                        .hints()
        );
        assertMatchingSCXMLStates(wf.getWorkflowExecutor(), StatesBuilder.build()
                        .status().logEvent().noEdit().requested().noPublish().noDepublish().noVersioning().terminateable().noCopy()
                        .states()
        );

        scheduledRequest.remove();

        // author + writable containing folder, !editing, !live: requestDelete=true
        assertMatchingHints(wf.hints(), HintsBuilder.build()
                        .status(true).isLive(false).previewAvailable(false).checkModified(false).noEdit().editable()
                        .requestPublication(false).requestDepublication(false)
                        .listVersions().requestDelete(true)
                        .hints()
        );
        assertMatchingSCXMLStates(wf.getWorkflowExecutor(), StatesBuilder.build()
                        .status().logEvent().editable().noRequest().noPublish().noDepublish().noVersioning().terminateable().noCopy()
                        .states()
        );

        session.setPermissions(publishedVariant.getPath(), "hippo:editor", true);

        // editor + writable containing folder, !editing, !live: requestDelete=true, terminateable
        assertMatchingHints(wf.hints(), HintsBuilder.build()
                        .status(true).isLive(false).previewAvailable(false).checkModified(false).noEdit().editable()
                        .requestPublication(false).publish(false).requestDepublication(false).depublish(false)
                        .listVersions().requestDelete(true).terminateable(true).copy()
                        .hints()
        );
        assertMatchingSCXMLStates(wf.getWorkflowExecutor(), StatesBuilder.build()
                        .status().logEvent().editable().noRequest().noPublish().noDepublish().noVersioning().terminateable().copyable()
                        .states()
        );
    }

    @Test
    public void testCopyState() throws Exception {
        MockAccessManagedSession session = new MockAccessManagedSession(MockNode.root());
        MockNode handleNode = (MockNode)session.getRootNode().addNode("test", HippoNodeType.NT_HANDLE);
        MockWorkflowContext workflowContext = new MockWorkflowContext("testuser", session);
        DocumentWorkflowImpl wf = new DocumentWorkflowImpl();
        wf.setWorkflowContext(workflowContext);
        wf.setNode(handleNode);

        MockNode publishedVariant = addVariant(handleNode, HippoStdNodeType.PUBLISHED);
        session.setPermissions(publishedVariant.getPath(), "hippo:author", true);

        // author, published: no-copy
        assertMatchingHints(wf.hints(), HintsBuilder.build()
                        .status(true).isLive(true).previewAvailable(true).checkModified(false).noEdit().editable()
                        .requestPublication(false).requestDepublication(true)
                        .listVersions().requestDelete(false)
                        .hints()
        );
        assertMatchingSCXMLStates(wf.getWorkflowExecutor(), StatesBuilder.build()
                        .status().logEvent().editable().noRequest().noPublish().depublishable().noVersioning().noTerminate().noCopy()
                        .states()
        );

        session.setPermissions(publishedVariant.getPath(), "hippo:editor", true);

        // editor, published: copy
        assertMatchingHints(wf.hints(), HintsBuilder.build()
                        .status(true).isLive(true).previewAvailable(true).checkModified(false).noEdit().editable()
                        .requestPublication(false).publish(false).requestDepublication(true).depublish(true)
                        .listVersions().requestDelete(false).terminateable(false).copy()
                        .hints()
        );
        assertMatchingSCXMLStates(wf.getWorkflowExecutor(), StatesBuilder.build()
                        .status().logEvent().editable().noRequest().noPublish().depublishable().noVersioning().noTerminate().copyable()
                        .states()
        );
    }
}
