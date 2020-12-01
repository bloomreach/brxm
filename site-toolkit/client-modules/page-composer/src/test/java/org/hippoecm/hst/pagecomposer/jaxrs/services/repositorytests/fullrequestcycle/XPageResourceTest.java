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
package org.hippoecm.hst.pagecomposer.jaxrs.services.repositorytests.fullrequestcycle;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.ws.rs.core.Response;

import org.apache.commons.io.IOUtils;
import org.hippoecm.hst.configuration.HstNodeTypes;
import org.hippoecm.hst.pagecomposer.jaxrs.model.ContainerRepresentation;
import org.hippoecm.hst.pagecomposer.jaxrs.model.ExtResponseRepresentation;
import org.hippoecm.repository.HippoStdNodeType;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.util.JcrUtils;
import org.hippoecm.repository.util.NodeIterable;
import org.hippoecm.repository.util.Utilities;
import org.json.JSONException;
import org.junit.Before;
import org.junit.Test;
import org.onehippo.repository.branch.BranchConstants;
import org.onehippo.repository.documentworkflow.BranchHandleImpl;
import org.onehippo.repository.documentworkflow.DocumentWorkflow;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.mock.web.MockHttpServletResponse;

import com.fasterxml.jackson.databind.ObjectMapper;

import static java.lang.Boolean.FALSE;
import static javax.ws.rs.core.Response.Status.CREATED;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_BRANCHES_PROPERTY;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_MIXIN_BRANCH_INFO;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_PROPERTY_BRANCH_ID;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_PROPERTY_BRANCH_NAME;
import static org.hippoecm.repository.api.HippoNodeType.NT_HIPPO_VERSION_INFO;
import static org.hippoecm.repository.HippoStdNodeType.NT_CM_XPAGE_FOLDER;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class XPageResourceTest extends AbstractXPageComponentResourceTest {

    private String containerNodeName;
    private Node hstXpageDocNode;
    private String mountId;
    private String experienceSiteMapItemId;

    @Before
    public void setup() throws Exception {
        // the containerNodeName is the equivalent of the 'hippo:identifier' on the XPage Layout container
        containerNodeName = "430df2da-3dc8-40b5-bed5-bdc44b8445c6";

        // first delete the container from the XPage : this is for the fixture: this mimics the situation that to an
        // existing XPage Layout a new container gets added which does not have its equivalent container on existing
        // XPage Docs

        hstXpageDocNode = admin.getNode(unpublishedExpPageVariant.getPath() + "/hst:xpage");

        admin.getNode(hstXpageDocNode.getPath() + "/" + containerNodeName).remove();
        admin.save();

        mountId = getNodeId(admin, "/hst:hst/hst:hosts/dev-localhost/localhost/hst:root");
        experienceSiteMapItemId = getNodeId(admin, "/hst:hst/hst:configurations/unittestproject/hst:sitemap/experiences/_any_.html");

    }

    @Test
    public void move_container_item_to_container_missing_in_xpage() throws Exception {

        //  try to move the container item from /430df2da-3dc8-40b5-bed5-bdc44b8445c7/banner to 'containerNodeName'
        // container which does not exist any more: as a result, it should be created

        // the 'endpoint' is against the hst:xpage node
        final RequestResponseMock updateRequestResponse = mockGetRequestResponse(
                "http", "localhost", "/_rp/" + hstXpageDocNode.getIdentifier(), null,
                "PUT");

        final ContainerRepresentation containerRepresentation = new ContainerRepresentation();

        // this container does not yet exist for the hst:xpage and should gete created as a result
        containerRepresentation.setId(containerNodeName);

        final String itemToMove = hstXpageDocNode.getNode("430df2da-3dc8-40b5-bed5-bdc44b8445c7/banner").getIdentifier();

        containerRepresentation.setChildren(Collections.singletonList(itemToMove));

        updateRequestResponse.getRequest().setContent(new ObjectMapper().writeValueAsBytes(containerRepresentation));
        updateRequestResponse.getRequest().setContentType("application/json;charset=UTF-8");

        final MockHttpServletResponse updateResponse = render(mountId, updateRequestResponse, ADMIN_CREDENTIALS);

        assertEquals(Response.Status.OK.getStatusCode(), updateResponse.getStatus());

        // assert container has been created and item has been moved
        assertTrue("Expected new container with name equal to 'containerNodeName' to have been created", hstXpageDocNode.hasNode(containerNodeName));

        final Node container = hstXpageDocNode.getNode(containerNodeName);

        assertTrue("Expected the banner item was moved", container.hasNode("banner"));

    }

    @Test
    public void create_container_item_when_container_missing_in_xpage() throws Exception {

        final String catalogId = getNodeId(admin, "/hst:hst/hst:configurations/hst:default/hst:catalog/testpackage/newstyle-testitem");

        final DocumentWorkflow documentWorkflow = getDocumentWorkflow(admin);
        // since document got published and nothing yet changed, should not be published

        assertEquals("No changes yet in unpublished, hence not expected publication option",
                FALSE, documentWorkflow.hints().get("publish"));

        /*
         * the invocation that is done from the CM is:
         *
         * _rp/{uuid}./{xpage_container_hippo_identifier}/{catalog_uuid}
         *
         * where {uuid} is the uuid of the hst:xpage node below the XPage document : the reason for this form is as
         * follows:
         *
         * - An author should be able to add items to the container
         * - An author cannot read the hst XPage Layout configuration
         *
         * So the only thing we can do to provide enough info is as follows:
         *
         * - provide the hst:xpage uuid below the unpublished variant
         * - provide the node name of the container to be created: this is the value of the hippo:identifier on the
         *   XPage Layout in hst config
         * - provide the catalog item id
         *
         * With the above info, the backend can know which container node to created and which item to inject into it
         */

        final RequestResponseMock createRequestResponse = mockGetRequestResponse(
                "http", "localhost", "/_rp/" + hstXpageDocNode.getIdentifier() + "./" + containerNodeName + "/" + catalogId, null,
                "POST");


        // use author credentials: if author succeeds, then surely for ADMIN it will also work
        final MockHttpServletResponse createResponse = render(mountId, createRequestResponse, AUTHOR_CREDENTIALS);
        final ExtResponseRepresentation extResponseRepresentation = mapper.readerFor(ExtResponseRepresentation.class).readValue(createResponse.getContentAsString());

        // for a newly added container, ALWAYS a page reload is needed
        assertRequiresReload(createResponse, true);

        assertEquals(CREATED.getStatusCode(), createResponse.getStatus());

        // assert modifying the preview did not create a draft variant!!! changes are directly on unpublished
        assertNull(getVariant(handle, "draft"));

        Map<String, ?> map = (Map) extResponseRepresentation.getData();
        final String createdUUID = map.get("id").toString();


        assertTrue(admin.nodeExists(unpublishedExpPageVariant.getPath() + "/hst:xpage/" + containerNodeName));

        final Node newlyCreatedContainer = admin.getNode(unpublishedExpPageVariant.getPath() + "/hst:xpage/" + containerNodeName);

        assertTrue(newlyCreatedContainer.hasNode("newstyle-testitem"));
        // assert the createdUUID is for the created container ITEM, not the container!
        assertEquals(createdUUID, newlyCreatedContainer.getNode("newstyle-testitem").getIdentifier());

    }

    @Test
    public void action_and_state_for_master_channel_and_master_xpage() throws Exception {

        assertions(ADMIN_CREDENTIALS, "action_and_state_for_master_channel_and_master_xpage_admin.json", "master");
        assertions(AUTHOR_CREDENTIALS, "action_and_state_for_master_channel_and_master_xpage_author.json", "master");

    }

    @Test
    public void action_and_state_for_master_channel_and_master_xpage_with_changes() throws Exception {

        final DocumentWorkflow documentWorkflow = getDocumentWorkflow(admin);
        documentWorkflow.obtainEditableInstance();
        final Node draftNode = getVariant(handle, HippoStdNodeType.DRAFT);
        draftNode.setProperty(HippoNodeType.HIPPO_NAME, "TEST");
        admin.save();
        documentWorkflow.commitEditableInstance();

        assertions(ADMIN_CREDENTIALS, "action_and_state_for_master_channel_and_master_xpage_admin_modified.json", "master");
        assertions(AUTHOR_CREDENTIALS, "action_and_state_for_master_channel_and_master_xpage_author_modified.json", "master");

        // make sure that the unpublished version in jcr workspace is for a branch, and then validate the 'master' again
        // now master comes from version history and should have the same result

        documentWorkflow.branch("foo", "Foo");

        assertTrue("expect master unpublished to be in version history",
                new BranchHandleImpl("master", handle).getUnpublished().getPath().startsWith("/jcr:system/jcr:versionStorage"));

        // even though master in version history, we expect the exact same states and actions for master since not
        // changed
        assertions(ADMIN_CREDENTIALS, "action_and_state_for_master_channel_and_master_xpage_admin_modified.json", "master");
        assertions(AUTHOR_CREDENTIALS, "action_and_state_for_master_channel_and_master_xpage_author_modified.json", "master");

    }

    @Test
    public void action_and_state_for_master_channel_and_master_xpage_in_version_history() throws Exception {

        // as a result of branching, unpublished will be for 'foo' and 'master' will be loaded from version history,
        // the states and actions for master however should still be the same as before
        final DocumentWorkflow documentWorkflow = getDocumentWorkflow(admin);

        documentWorkflow.branch("foo", "Foo");

        // trigger a change in the 'foo' variant, this should not result the master to have 'request-publish'
        documentWorkflow.obtainEditableInstance("foo");
        final Node draftNode = getVariant(handle, HippoStdNodeType.DRAFT);
        draftNode.setProperty(HippoNodeType.HIPPO_NAME, "TEST");
        admin.save();
        documentWorkflow.commitEditableInstance();

        assertTrue("Expected that 'foo' branch has changes", (Boolean) documentWorkflow.hints("foo").get("publishBranch"));

        // expected same fixture as for action_and_state_for_master_channel_and_master_xpage
        assertions(ADMIN_CREDENTIALS, "action_and_state_for_master_channel_and_master_xpage_admin.json", "master");
        assertions(AUTHOR_CREDENTIALS, "action_and_state_for_master_channel_and_master_xpage_author.json", "master");

    }


    @Test
    public void action_and_state_for_channel_foo_and_NO_EXISTING_xpage_for_branch_foo() throws Exception {

        // both admin and author are expected to get the very same result if branch does not exist
        assertions(ADMIN_CREDENTIALS, "action_and_state_for_channel_foo_and_NO_EXISTING_xpage_for_branch_foo.json", "foo");
        assertions(AUTHOR_CREDENTIALS, "action_and_state_for_channel_foo_and_NO_EXISTING_xpage_for_branch_foo.json", "foo");

    }

    @Test
    public void action_and_state_for_channel_foo_and_EXISTING_xpage_for_branch_foo() throws Exception {

        final DocumentWorkflow documentWorkflow = getDocumentWorkflow(admin);

        documentWorkflow.branch("foo", "Foo");

        assertions(ADMIN_CREDENTIALS, "action_and_state_for_channel_foo_and_EXISTING_xpage_for_branch_foo.json", "foo");
        assertions(AUTHOR_CREDENTIALS, "action_and_state_for_channel_foo_and_EXISTING_xpage_for_branch_foo.json", "foo");

        // make sure branch 'foo' moves to version history, this should not impact the actions on it

        documentWorkflow.checkoutBranch(BranchConstants.MASTER_BRANCH_ID);

        assertTrue("expect foo unpublished to be in version history",
                new BranchHandleImpl("foo", handle).getUnpublished().getPath().startsWith("/jcr:system/jcr:versionStorage"));

        assertions(ADMIN_CREDENTIALS, "action_and_state_for_channel_foo_and_EXISTING_xpage_for_branch_foo.json", "foo");
        assertions(AUTHOR_CREDENTIALS, "action_and_state_for_channel_foo_and_EXISTING_xpage_for_branch_foo.json", "foo");

        // trigger a change in the 'foo' variant to
        documentWorkflow.obtainEditableInstance("foo");
        final Node draftNode = getVariant(handle, HippoStdNodeType.DRAFT);
        draftNode.setProperty(HippoNodeType.HIPPO_NAME, "TEST");
        admin.save();
        documentWorkflow.commitEditableInstance();

        assertions(ADMIN_CREDENTIALS, "action_and_state_for_channel_foo_and_EXISTING_xpage_for_branch_foo_modified.json", "foo");
        assertions(AUTHOR_CREDENTIALS, "action_and_state_for_channel_foo_and_EXISTING_xpage_for_branch_foo_modified.json", "foo");


        documentWorkflow.checkoutBranch(BranchConstants.MASTER_BRANCH_ID);

        // also when in version history, modified is picked up correctly
        assertions(ADMIN_CREDENTIALS, "action_and_state_for_channel_foo_and_EXISTING_xpage_for_branch_foo_modified.json", "foo");
        assertions(AUTHOR_CREDENTIALS, "action_and_state_for_channel_foo_and_EXISTING_xpage_for_branch_foo_modified.json", "foo");

    }

    @Test
    public void action_and_state_for_channel_foo_and_EXISTING_unpublished_xpage_for_branch_foo() throws Exception {

        final DocumentWorkflow documentWorkflow = getDocumentWorkflow(admin);
        documentWorkflow.depublish();

        documentWorkflow.branch("foo", "Foo");

        assertions(ADMIN_CREDENTIALS, "action_and_state_for_channel_foo_and_EXISTING_unpublished_xpage_for_branch_foo.json", "foo");
        assertions(AUTHOR_CREDENTIALS, "action_and_state_for_channel_foo_and_EXISTING_unpublished_xpage_for_branch_foo.json", "foo");
    }

    @Test
    public void action_and_state_for_channel_master_cmxpagefolder() throws Exception {

        admin.getNode("/unittestcontent/documents/unittestproject/experiences/experiences-subfolder").addMixin(NT_CM_XPAGE_FOLDER);
        admin.save();

        try {
            assertions(ADMIN_CREDENTIALS, "action_and_state_for_master_channel_and_master_xpage_admin_cmxpagefolder.json", "master");
            assertions(AUTHOR_CREDENTIALS, "action_and_state_for_master_channel_and_master_xpage_author_cmxpagefolder.json", "master");
        } finally {
            admin.getNode("/unittestcontent/documents/unittestproject/experiences/experiences-subfolder").removeMixin(NT_CM_XPAGE_FOLDER);
            admin.save();
        }
    }



    @Test
    public void action_and_state_for_channel_foo_and_xpage_for_branch_foo_and_NO_master() throws Exception {

        // change the document to be only for 'foo' and not existing for master : for this to work, we first need
        // to copy the handle to a fresh new document since this one already has a version for 'master'

        Node xpageWithoutCoreHandle = null;

        try {
            xpageWithoutCoreHandle = JcrUtils.copy(handle, "xpageDocWithoutCore", handle.getParent());

            final Session session = handle.getSession();
            xpageWithoutCoreHandle.addMixin(NT_HIPPO_VERSION_INFO);
            xpageWithoutCoreHandle.setProperty(HIPPO_BRANCHES_PROPERTY, new String[]{"foo"});
            for (Node variant : new NodeIterable(xpageWithoutCoreHandle.getNodes())) {
                variant.addMixin(HIPPO_MIXIN_BRANCH_INFO);
                variant.setProperty(HIPPO_PROPERTY_BRANCH_ID, "foo");
                variant.setProperty(HIPPO_PROPERTY_BRANCH_NAME, "Foo");
                session.move(variant.getPath(), xpageWithoutCoreHandle.getPath() + "/xpageDocWithoutCore");
            }
            session.save();

            // change the hstXpageDocNode to point to the new node to test
            hstXpageDocNode = getVariant(xpageWithoutCoreHandle, "unpublished").getNode("hst:xpage");

            assertions(ADMIN_CREDENTIALS, "action_and_state_for_channel_foo_and_xpage_for_branch_foo_and_NO_master.json", "foo");
            assertions(AUTHOR_CREDENTIALS, "action_and_state_for_channel_foo_and_xpage_for_branch_foo_and_NO_master.json", "foo");
        } finally {
            if (xpageWithoutCoreHandle != null) {
                xpageWithoutCoreHandle.remove();
                handle.getSession().save();
            }
        }
    }

    @Test
    public void action_and_state_for_channel_foo_and_NO_EXISTING_xpage_for_branch_foo_AND_NO_master() throws Exception {

        // change the document to be only for 'bar' and not existing for master : for this to work, we first need
        // to copy the handle to a fresh new document since this one already has a version for 'master'

        Node xpageWithoutCoreHandle = null;
        try {
            xpageWithoutCoreHandle = JcrUtils.copy(handle, "xpageDocWithoutCore", handle.getParent());
            final Session session = handle.getSession();
            xpageWithoutCoreHandle.addMixin(NT_HIPPO_VERSION_INFO);
            xpageWithoutCoreHandle.setProperty(HIPPO_BRANCHES_PROPERTY, new String[]{"bar"});
            for (Node variant : new NodeIterable(xpageWithoutCoreHandle.getNodes())) {
                variant.addMixin(HIPPO_MIXIN_BRANCH_INFO);
                variant.setProperty(HIPPO_PROPERTY_BRANCH_ID, "bar");
                variant.setProperty(HIPPO_PROPERTY_BRANCH_NAME, "Bar");
                session.move(variant.getPath(), xpageWithoutCoreHandle.getPath() + "/xpageDocWithoutCore");
            }
            session.save();

            // change the hstXpageDocNode to point to the new node to test
            hstXpageDocNode = getVariant(xpageWithoutCoreHandle, "unpublished").getNode("hst:xpage");


            // now we have a use case where we 'ask' for branch  'foo' which does not exist. Normally you then get 'core',
            // however, since core does not exist, 'just' any other branch which exists (bar in this case) should be returned
            assertions(ADMIN_CREDENTIALS, "action_and_state_for_channel_foo_and_NO_EXISTING_xpage_for_branch_foo_and_NO_master.json", "foo");
            assertions(AUTHOR_CREDENTIALS, "action_and_state_for_channel_foo_and_NO_EXISTING_xpage_for_branch_foo_and_NO_master.json", "foo");
        } finally {
            if (xpageWithoutCoreHandle != null) {
                xpageWithoutCoreHandle.remove();
                handle.getSession().save();
            }
        }
    }

    private void assertions(final SimpleCredentials creds, String fixtureFileName, final String branchId) throws Exception {
        final RequestResponseMock createRequestResponse = mockGetRequestResponse(
                "http", "localhost", "/_rp/" + hstXpageDocNode.getIdentifier() + "./item/" + experienceSiteMapItemId, null,
                "GET");

        final MockHttpServletResponse get = render(mountId, createRequestResponse, creds, branchId);
        InputStream expected = XPageResourceTest.class.getResourceAsStream(fixtureFileName);
        assertions(get.getContentAsString(), expected);
    }


    private void assertions(final String actual, final InputStream expectedStream) throws IOException, JSONException {
        String expected = IOUtils.toString(expectedStream, StandardCharsets.UTF_8);
        JSONAssert.assertEquals(expected, actual, JSONCompareMode.STRICT_ORDER);
    }
}
