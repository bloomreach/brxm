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
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.jcr.Value;
import javax.servlet.ServletException;
import javax.ws.rs.core.Response;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.assertj.core.api.Assertions;
import org.hippoecm.hst.configuration.HstNodeTypes;
import org.hippoecm.hst.pagecomposer.jaxrs.AbstractPageComposerTest;
import org.hippoecm.hst.pagecomposer.jaxrs.model.ContainerRepresentation;
import org.hippoecm.hst.pagecomposer.jaxrs.model.ExtResponseRepresentation;
import org.hippoecm.hst.pagecomposer.jaxrs.services.experiencepage.XPageContainerComponentResource;
import org.hippoecm.repository.HippoStdNodeType;
import org.hippoecm.repository.HippoStdPubWfNodeType;
import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.HippoSession;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.api.WorkflowManager;
import org.hippoecm.repository.util.JcrUtils;
import org.junit.Test;
import org.onehippo.repository.documentworkflow.DocumentWorkflow;
import org.onehippo.repository.testutils.RepositoryTestCase;
import org.onehippo.testutils.log4j.Log4jInterceptor;
import org.springframework.mock.web.MockHttpServletResponse;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.CREATED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hippoecm.hst.configuration.HstNodeTypes.COMPONENT_PROPERTY_COMPONENTDEFINITION;
import static org.hippoecm.hst.configuration.HstNodeTypes.COMPONENT_PROPERTY_COMPONENT_CLASSNAME;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_PROPERTY_BRANCH_ID;
import static org.hippoecm.repository.util.JcrUtils.getStringProperty;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class XPageContainerComponentResourceTest extends AbstractXPageComponentResourceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    public void modifying_live_or_draft_variants_not_allowed() throws Exception {
        final String mountId = getNodeId(admin, "/hst:hst/hst:hosts/dev-localhost/localhost/hst:root");

        final String catalogId = getNodeId(admin, "/hst:hst/hst:configurations/hst:default/hst:catalog/testpackage/oldstyle-testitem");

        final String containerId = getNodeId(admin,publishedExpPageVariant.getPath() + "/hst:xpage/430df2da-3dc8-40b5-bed5-bdc44b8445c6");

        failCreateAssertions(mountId, catalogId, containerId);

    }

    private void failCreateAssertions(final String mountId, final String catalogId, final String containerId) throws IOException, ServletException {
        final RequestResponseMock createRequestResponse = mockGetRequestResponse(
                "http", "localhost", "/_rp/" + containerId + "./" + catalogId, null,
                "POST");

        final MockHttpServletResponse createResponse = render(mountId, createRequestResponse, ADMIN_CREDENTIALS);
        final Map<String, String> createResponseMap = mapper.readerFor(Map.class).readValue(createResponse.getContentAsString());

        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), createResponse.getStatus());
        assertEquals(false, createResponseMap.get("success"));
        assertTrue(createResponseMap.get("message").contains("Does not belong to unpublished variant of Experience Page."));
    }

    @Test
    public void create_and_delete_container_item_as_admin() throws Exception {
        createAndDeleteItemAs(ADMIN_CREDENTIALS, true, false, "oldstyle-testitem");
        createAndDeleteItemAs(ADMIN_CREDENTIALS, true, false, "newstyle-testitem");
    }


    @Test
    public void create_and_delete_container_item_as_admin_for_VERSIONED_XPage() throws Exception {
        createAndDeleteItemAs(ADMIN_CREDENTIALS, true, true, "oldstyle-testitem");
    }

    @Test
    public void create_and_delete_container_item_as_editor() throws Exception {
        createAndDeleteItemAs(EDITOR_CREDENTIALS, true, false, "oldstyle-testitem");
    }

    @Test
    public void create_and_delete_container_item_as_editor_for_VERSIONED_XPage() throws Exception {
        createAndDeleteItemAs(EDITOR_CREDENTIALS, true, true, "oldstyle-testitem");
    }

    /**
     * Note an author cannot modify hst config pages but *CAN* modify experience pages if the cms user as role author
     */
    @Test
    public void create_and_delete_container_item_as_author() throws Exception {
        // author is not allowed to do a GET on ContainerItemComponentResource.getVariant()
        createAndDeleteItemAs(AUTHOR_CREDENTIALS, true, false, "oldstyle-testitem");
    }


    @Test
    public void create_and_delete_container_item_as_author_for_VERSIONED_XPage() throws Exception {
        // author is not allowed to do a GET on ContainerItemComponentResource.getVariant()
        createAndDeleteItemAs(AUTHOR_CREDENTIALS, true, true, "oldstyle-testitem");
    }

    /**
     * Note an author who does not have role hippo:author on the experience page is not allowed to modify the hst:page
     * in the experience page document
     */
    @Test
    public void create_container_item_NOT_allowed_it_not_role_author() throws Exception {
        // for author user, temporarily remove the role 'hippo:author' : Without this role, (s)he should not be allowed
        // to invoked the XPageContainerComponentResource

        final Session admin = createSession(ADMIN_CREDENTIALS);
        Property privilegesProp = admin.getNode("/hippo:configuration/hippo:roles/author").getProperty("hipposys:privileges");
        Value[] before = privilegesProp.getValues();
        privilegesProp.remove();
        admin.save();

        try {
            // since author does not have privilege hippo:author anymore, expect a FORBIDDEN
            createAndDeleteItemAs(AUTHOR_CREDENTIALS, false, false, "oldstyle-testitem");
        } finally {
            // restore privileges
            admin.getNode("/hippo:configuration/hippo:roles/author").setProperty("hipposys:privileges", before);
            admin.save();
        }
    }

    /**
     * Note an author who does not have role hippo:author on the experience page is not allowed to modify the hst:page
     * in the experience page document
     */
    @Test
    public void create_container_item_NOT_allowed_it_not_role_author_for_VERSIONED_XPage() throws Exception {
        // for author user, temporarily remove the role 'hippo:author' : Without this role, (s)he should not be allowed
        // to invoked the XPageContainerComponentResource

        final Session admin = createSession(ADMIN_CREDENTIALS);
        Property privilegesProp = admin.getNode("/hippo:configuration/hippo:roles/author").getProperty("hipposys:privileges");
        Value[] before = privilegesProp.getValues();
        privilegesProp.remove();
        admin.save();

        try {
            // since author does not have privilege hippo:author anymore, expect a FORBIDDEN
            createAndDeleteItemAs(AUTHOR_CREDENTIALS, false, true, "oldstyle-testitem");
        } finally {
            // restore privileges
            admin.getNode("/hippo:configuration/hippo:roles/author").setProperty("hipposys:privileges", before);
            admin.save();
        }
    }

    /**
     * The 'creds' are used to invoke the HST pipeline, the adminSession is used to invoke workflow from here (like
     * publish)
     */
    private void createAndDeleteItemAs(final SimpleCredentials creds, final boolean allowed,
                                       final boolean versionedXPageTest, final String catalogItemNodeName) throws Exception {

        final String mountId = getNodeId(admin, "/hst:hst/hst:hosts/dev-localhost/localhost/hst:root");

        final String containerId;
        if (versionedXPageTest) {// assert current unpublished variant is for branch
            containerId = doVersionAndgetFrozenContainer().getIdentifier();
            // assert current unpublished variant is for a branch and not for MASTER due to doVersionAndgetFrozenContainer
            assertThat(unpublishedExpPageVariant.hasProperty(HIPPO_PROPERTY_BRANCH_ID)).isTrue();
        } else {
            containerId = getNodeId(admin, unpublishedExpPageVariant.getPath() + "/hst:xpage/430df2da-3dc8-40b5-bed5-bdc44b8445c6");
        }

        final String catalogId = getNodeId(admin, "/hst:hst/hst:configurations/hst:default/hst:catalog/testpackage/" + catalogItemNodeName);

        final DocumentWorkflow documentWorkflow = getDocumentWorkflow(admin);
        // since document got published and nothing yet changed, should not be published

        assertEquals("No changes yet in unpublished, hence not expected publication option",
                FALSE, documentWorkflow.hints().get("publish"));

        final RequestResponseMock createRequestResponse = mockGetRequestResponse(
                "http", "localhost", "/_rp/" + containerId + "./" + catalogId, null,
                "POST");


        final MockHttpServletResponse createResponse = render(mountId, createRequestResponse, creds);
        final ExtResponseRepresentation extResponseRepresentation = mapper.readerFor(ExtResponseRepresentation.class).readValue(createResponse.getContentAsString());

        if (!allowed) {
            assertEquals("FORBIDDEN", extResponseRepresentation.getErrorCode());
            return;
        }

        if (versionedXPageTest) {
            // assert current unpublished variant is now for MASTER since master should have been checked out
            assertThat(unpublishedExpPageVariant.hasProperty(HIPPO_PROPERTY_BRANCH_ID)).isFalse();
        }
        assertRequiresReload(createResponse, versionedXPageTest);

        assertEquals(CREATED.getStatusCode(), createResponse.getStatus());

        // assert modifying the preview did not create a draft variant!!! changes are directly on unpublished
        assertNull(getVariant(handle, "draft"));

        Map<String, ?> map = (Map) extResponseRepresentation.getData();
        final String createdUUID = map.get("id").toString();

        // assertion on newly created item
        assertTrue(admin.nodeExists(unpublishedExpPageVariant.getPath() + "/hst:xpage/430df2da-3dc8-40b5-bed5-bdc44b8445c6/" + catalogItemNodeName));

        final Node newContainerItem = admin.getNodeByIdentifier(createdUUID);

        // assert newly created container item does not have the catalog item copied (old style) but keeps a
        // hst:componentdefinition reference
        assertThat(admin.getNode("/hst:hst/hst:configurations/hst:default/hst:catalog/testpackage/" + catalogItemNodeName)
                .hasProperty(COMPONENT_PROPERTY_COMPONENT_CLASSNAME))
                .as("Catalog item expected to have classname")
                .isTrue();

        if (catalogItemNodeName.equals("oldstyle-testitem")) {
            // old-style is expected to copy the hst:componentclassname
            assertThat(newContainerItem.hasProperty(COMPONENT_PROPERTY_COMPONENT_CLASSNAME))
                    .as("Component item referencing catalog item expected not to have classname")
                    .isTrue();

            // old-style is not expected to contain hst:componentdefinition backreference
            assertThat(newContainerItem.hasProperty(COMPONENT_PROPERTY_COMPONENTDEFINITION))
                    .as("Component backreference not expected for oldstyle item")
                    .isFalse();

        } else if (catalogItemNodeName.equals("newstyle-testitem")) {
            // new-style is expected to NOT copy the hst:componentclassname
            assertThat(newContainerItem.hasProperty(COMPONENT_PROPERTY_COMPONENT_CLASSNAME))
                    .as("Component item referencing catalog item expected not to have classname")
                    .isFalse();

            // new-style is expected to contain hst:componentdefinition backreference
            assertThat(JcrUtils.getStringProperty(newContainerItem, COMPONENT_PROPERTY_COMPONENTDEFINITION, null))
                    .as("Expected newly created container item to have hst:componentdefinition property pointing " +
                            "to catalog item")
                    .isEqualTo("hst:components/testpackage/" + catalogItemNodeName);
        }


        // assert document can now be published
        assertEquals("Unpublished has changes, publication should be enabled",
                TRUE, documentWorkflow.hints().get("publish"));

        assertEquals("Expected unpublished to have been last modified by current user", creds.getUserID(),
                unpublishedExpPageVariant.getProperty(HippoStdPubWfNodeType.HIPPOSTDPUBWF_LAST_MODIFIED_BY).getString());

        // now publish the document,
        documentWorkflow.publish();
        // assert that published variant now has extra container item 'testitem'
        assertTrue(admin.nodeExists(publishedExpPageVariant.getPath() + "/hst:xpage/430df2da-3dc8-40b5-bed5-bdc44b8445c6/" + catalogItemNodeName));


        // now delete : for versioned XPage tests, the containerId from above can be one from version history but since
        // the XPage already has been modified by the create, it has been checked out to the workspace : If you then
        // use the OLD containerId again, the request SHOULD fail because that means the user is interacting with a
        // STALE page
        if (versionedXPageTest) {
            final RequestResponseMock deleteRequestResponse = mockGetRequestResponse(
                    "http", "localhost", "/_rp/" + containerId + "./" + createdUUID, null,
                    "DELETE");
            try (final Log4jInterceptor interceptor = Log4jInterceptor.onInfo().trap(XPageContainerComponentResource.class).build()) {
                final MockHttpServletResponse fail = render(mountId, deleteRequestResponse, creds);
                assertThat(fail.getStatus())
                        .as("Because the delete request is done with the containerId from version history, it is " +
                                "expected that the request fail since the version has been checked out already to the workspace " +
                                "and thus is 'containerId' part of a stale page")
                        .isEqualTo(400);

                final List<String> messages = interceptor.messages().collect(Collectors.toList());
                assertThat(messages.size())
                        .isEqualTo(1);

                assertThat(messages.get(0)) .as("Expected message about not being most recent version for containerId")
                        .contains("s not the most recent version for 'master' anymore");

            }
        }
        final RequestResponseMock deleteRequestResponse = mockGetRequestResponse(
                "http", "localhost", "/_rp/" + newContainerItem.getParent().getIdentifier() + "./" + createdUUID, null,
                "DELETE");
        final MockHttpServletResponse deleteResponse = render(mountId, deleteRequestResponse, creds);
        assertEquals(Response.Status.OK.getStatusCode(), deleteResponse.getStatus());

        try {
            admin.getNodeByIdentifier(createdUUID);
            fail("Item expected to have been deleted again");
        } catch (ItemNotFoundException e) {
            // expected
        }

        // published variant still has the container item
        assertTrue(admin.nodeExists(publishedExpPageVariant.getPath() + "/hst:xpage/430df2da-3dc8-40b5-bed5-bdc44b8445c6/" + catalogItemNodeName));

        // after delete, the unpublished has become publishable again
        assertEquals("Unpublished has changes, publication should be enabled",
                TRUE, documentWorkflow.hints().get("publish"));

        documentWorkflow.publish();

        // published variant should not have the container item any more
        assertFalse(admin.nodeExists(publishedExpPageVariant.getPath() + "/hst:xpage/430df2da-3dc8-40b5-bed5-bdc44b8445c6/" + catalogItemNodeName));

        // now assert an existing component item (or catalog) not from the current XPAGE cannot be deleted via
        // the container id

        final RequestResponseMock deleteRequestResponseInvalid = mockGetRequestResponse(
                "http", "localhost", "/_rp/" + containerId + "./" + catalogId, null,
                "DELETE");
        final MockHttpServletResponse invalidResponse = render(mountId, deleteRequestResponseInvalid, creds);
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), invalidResponse.getStatus());


    }

    @Test
    public void not_allowed_to_modify_xpage_if_draft_edited_by_other_user() throws Exception {

        // first make 'author' the holder of draft and then admin should not be allowed to make changes

        final HippoSession author = (HippoSession) createSession(AUTHOR_CREDENTIALS);

        // make sure document is being edited
        final DocumentWorkflow authorWorkflow = (DocumentWorkflow) author.getWorkspace().getWorkflowManager().getWorkflow("default", author.getNode(EXPERIENCE_PAGE_HANDLE_PATH));
        authorWorkflow.obtainEditableInstance();

        final String mountId = getNodeId(admin, "/hst:hst/hst:hosts/dev-localhost/localhost/hst:root");

        final String containerId = getNodeId(admin, unpublishedExpPageVariant.getPath() + "/hst:xpage/430df2da-3dc8-40b5-bed5-bdc44b8445c6");
        final String catalogId = getNodeId(admin, "/hst:hst/hst:configurations/hst:default/hst:catalog/testpackage/oldstyle-testitem");

        final RequestResponseMock createRequestResponse = mockGetRequestResponse(
                "http", "localhost", "/_rp/" + containerId + "./" + catalogId, null,
                "POST");


        final MockHttpServletResponse createResponse = render(mountId, createRequestResponse, ADMIN_CREDENTIALS);
        final Map<String, String> createResponseMap = mapper.readerFor(Map.class).readValue(createResponse.getContentAsString());

        assertEquals(BAD_REQUEST.getStatusCode(), createResponse.getStatus());

        assertEquals("Document not editable", createResponseMap.get("message"));

    }

    @Test
    public void create_item_before() throws Exception {

        final String mountId = getNodeId(admin, "/hst:hst/hst:hosts/dev-localhost/localhost/hst:root");

        final String containerId = getNodeId(admin, unpublishedExpPageVariant.getPath() + "/hst:xpage/430df2da-3dc8-40b5-bed5-bdc44b8445c6");
        final String beforeItemId = getNodeId(admin, unpublishedExpPageVariant.getPath() + "/hst:xpage/430df2da-3dc8-40b5-bed5-bdc44b8445c6/banner");
        final String catalogId = getNodeId(admin, "/hst:hst/hst:configurations/hst:default/hst:catalog/testpackage/oldstyle-testitem");

        final RequestResponseMock createRequestResponse = mockGetRequestResponse(
                "http", "localhost", "/_rp/" + containerId + "./" + catalogId + "/" + beforeItemId, null,
                "POST");

        final MockHttpServletResponse createResponse = render(mountId, createRequestResponse, ADMIN_CREDENTIALS);

        assertEquals(CREATED.getStatusCode(), createResponse.getStatus());

        final Node container = admin.getNodeByIdentifier(containerId);

        final NodeIterator nodes = container.getNodes();
        assertEquals("Expected oldstyle-testitem to be created before banner", "oldstyle-testitem", nodes.nextNode().getName());
        assertEquals("Expected oldstyle-testitem to be created before banner", "banner", nodes.nextNode().getName());


        assertRequiresReload(createResponse, false);
    }

    @Test
    public void create_item_before_for_VERSIONED_XPage() throws Exception {

        final String mountId = getNodeId(admin, "/hst:hst/hst:hosts/dev-localhost/localhost/hst:root");

        final String containerId = doVersionAndgetFrozenContainer().getIdentifier();
        // expect unpublished variant to be for a branch
        assertThat(unpublishedExpPageVariant.hasProperty(HIPPO_PROPERTY_BRANCH_ID)).isTrue();

        final String beforeItemId = getNodeId(admin, unpublishedExpPageVariant.getPath() + "/hst:xpage/430df2da-3dc8-40b5-bed5-bdc44b8445c6/banner");
        final String catalogId = getNodeId(admin, "/hst:hst/hst:configurations/hst:default/hst:catalog/testpackage/oldstyle-testitem");

        final RequestResponseMock createRequestResponse = mockGetRequestResponse(
                "http", "localhost", "/_rp/" + containerId + "./" + catalogId + "/" + beforeItemId, null,
                "POST");

        final MockHttpServletResponse createResponse = render(mountId, createRequestResponse, ADMIN_CREDENTIALS);

        assertEquals(CREATED.getStatusCode(), createResponse.getStatus());


        assertThat(unpublishedExpPageVariant.hasProperty(HIPPO_PROPERTY_BRANCH_ID)).isFalse();

        // do not reuse containerId from above since that one is from version history
        final Node container = admin.getNodeByIdentifier(getNodeId(admin,
                unpublishedExpPageVariant.getPath() + "/hst:xpage/430df2da-3dc8-40b5-bed5-bdc44b8445c6"));

        final NodeIterator nodes = container.getNodes();
        assertEquals("Expected oldstyle-testitem to be created before banner", "oldstyle-testitem", nodes.nextNode().getName());
        assertEquals("Expected oldstyle-testitem to be created before banner", "banner", nodes.nextNode().getName());

        // versioned XPage was modified
        assertRequiresReload(createResponse, true);
    }

    @Test
    public void create_item_before_non_existing_item_results_in_error() throws Exception {

        final String beforeItemId = UUID.randomUUID().toString();
        final String expectedMessage = String.format("Cannot find container item '%s'", beforeItemId);
        notAllowedcreateBeforeItem(beforeItemId, expectedMessage, false);
    }

    @Test
    public void create_item_before_non_existing_item_results_in_error_for_VERSIONED_XPage() throws Exception {

        final String beforeItemId = UUID.randomUUID().toString();
        final String expectedMessage = String.format("Cannot find container item '%s'", beforeItemId);
        notAllowedcreateBeforeItem(beforeItemId, expectedMessage, true);
    }

    /**
     * the 'before item' is an item of another experience page
     *
     * @throws Exception
     */
    @Test
    public void create_item_before_item_of_other_container_results_in_error() throws Exception {

        final String beforeItemId = getNodeId(admin,EXPERIENCE_PAGE_2_HANDLE_PATH + "/expPage2/hst:xpage/430df2da-3dc8-40b5-bed5-bdc44b8445c6/banner");
        final String expectedMessage = String.format(String.format("Order before container item '%s' is of other experience page", beforeItemId));
        notAllowedcreateBeforeItem(beforeItemId, expectedMessage, false);
    }

    @Test
    public void create_item_before_item_of_other_container_results_in_error_for_VERSIONED_XPage() throws Exception {

        final String beforeItemId = getNodeId(admin,EXPERIENCE_PAGE_2_HANDLE_PATH + "/expPage2/hst:xpage/430df2da-3dc8-40b5-bed5-bdc44b8445c6/banner");
        final String expectedMessage = String.format(String.format("Order before container item '%s' is of other experience page", beforeItemId));
        notAllowedcreateBeforeItem(beforeItemId, expectedMessage, true);
    }

    @Test
    public void create_item_before_node_not_of_type_container_item_results_in_error() throws Exception {
        final String beforeItemId = unpublishedExpPageVariant.getIdentifier();
        final String expectedMessage = String.format(String.format("The container item '%s' does not have the correct type", beforeItemId));
        notAllowedcreateBeforeItem(beforeItemId, expectedMessage, false);
    }

    @Test
    public void create_item_before_node_not_of_type_container_item_results_in_error_for_VERSIONED_XPage() throws Exception {
        final String beforeItemId = unpublishedExpPageVariant.getIdentifier();
        final String expectedMessage = String.format(String.format("The container item '%s' does not have the correct type", beforeItemId));
        notAllowedcreateBeforeItem(beforeItemId, expectedMessage, true);
    }


    private void notAllowedcreateBeforeItem(final String beforeItemId, final String expectedMessage,
                                            final boolean versionedXPageTest) throws Exception {
        final String mountId = getNodeId(admin, "/hst:hst/hst:hosts/dev-localhost/localhost/hst:root");

        final String containerId;
        if (versionedXPageTest) {
            containerId = doVersionAndgetFrozenContainer().getIdentifier();
            // assert current unpublished variant is for a branch and not for MASTER due to getFrozenBannerComponent
            assertThat(unpublishedExpPageVariant.hasProperty(HIPPO_PROPERTY_BRANCH_ID)).isTrue();
        } else {
            containerId = getNodeId(unpublishedExpPageVariant.getPath() + "/hst:xpage/430df2da-3dc8-40b5-bed5-bdc44b8445c6");
        }


        final String catalogId = getNodeId(admin, "/hst:hst/hst:configurations/hst:default/hst:catalog/testpackage/oldstyle-testitem");

        final RequestResponseMock createRequestResponse = mockGetRequestResponse(
                "http", "localhost", "/_rp/" + containerId + "./" + catalogId + "/" + beforeItemId, null,
                "POST");

        final MockHttpServletResponse createResponse = render(mountId, createRequestResponse, ADMIN_CREDENTIALS);

        final Map<String, String> createResponseMap = mapper.readerFor(Map.class).readValue(createResponse.getContentAsString());

        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), createResponse.getStatus());
        assertEquals(false, createResponseMap.get("success"));

        assertEquals(expectedMessage, createResponseMap.get("message"));

        // although the invocation failed, the master branch was expected to be already checked out and thus branch
        // property expected to be removed
        assertThat(unpublishedExpPageVariant.hasProperty(HIPPO_PROPERTY_BRANCH_ID)).isFalse();

        // even for failed responses, for now we do not send 'reload = true'
        assertRequiresReload(createResponse, false);
    }


    @Test
    public void move_container_item_within_container() throws Exception {

        JcrUtils.copy(admin, unpublishedExpPageVariant.getPath() + "/hst:xpage/430df2da-3dc8-40b5-bed5-bdc44b8445c6/banner",
                unpublishedExpPageVariant.getPath() + "/hst:xpage/430df2da-3dc8-40b5-bed5-bdc44b8445c6/banner2");
        admin.save();

        final String mountId = getNodeId(admin, "/hst:hst/hst:hosts/dev-localhost/localhost/hst:root");

        final String containerId = getNodeId(admin, unpublishedExpPageVariant.getPath() + "/hst:xpage/430df2da-3dc8-40b5-bed5-bdc44b8445c6");
        final String itemId1 = getNodeId(admin, unpublishedExpPageVariant.getPath() + "/hst:xpage/430df2da-3dc8-40b5-bed5-bdc44b8445c6/banner");
        final String itemId2 = getNodeId(admin, unpublishedExpPageVariant.getPath() + "/hst:xpage/430df2da-3dc8-40b5-bed5-bdc44b8445c6/banner-new-style");
        final String itemId3 = getNodeId(admin, unpublishedExpPageVariant.getPath() + "/hst:xpage/430df2da-3dc8-40b5-bed5-bdc44b8445c6/banner2");

        final RequestResponseMock updateRequestResponse = mockGetRequestResponse(
                "http", "localhost", "/_rp/" + containerId, null,
                "PUT");

        final ContainerRepresentation containerRepresentation = new ContainerRepresentation();

        containerRepresentation.setId(containerId);
        // move item2 before item1
        containerRepresentation.setChildren(Stream.of(itemId2, itemId3, itemId1).collect(Collectors.toList()));

        updateRequestResponse.getRequest().setContent(objectMapper.writeValueAsBytes(containerRepresentation));
        updateRequestResponse.getRequest().setContentType("application/json;charset=UTF-8");

        final MockHttpServletResponse updateResponse = render(mountId, updateRequestResponse, ADMIN_CREDENTIALS);

        assertEquals(Response.Status.OK.getStatusCode(), updateResponse.getStatus());

        // assert container items have been flipped
        final Node container = admin.getNodeByIdentifier(containerId);

        final NodeIterator children = container.getNodes();

        assertEquals("banner-new-style", children.nextNode().getName());
        assertEquals("banner2", children.nextNode().getName());
        assertEquals("banner", children.nextNode().getName());

        // assert 'container node' is not locked
        assertNull("Container nodes for experience pages should never get locked",
                getStringProperty(container, HstNodeTypes.GENERAL_PROPERTY_LOCKED_BY, null));

        final DocumentWorkflow documentWorkflow = getDocumentWorkflow(admin);

        assertEquals("Unpublished has changes, publication should be enabled",
                TRUE, documentWorkflow.hints().get("publish"));

    }

    @Test
    public void move_container_item_within_container_for_VERSIONED_XPage() throws Exception {

        final DocumentWorkflow documentWorkflow = getDocumentWorkflow(admin);

        // trigger that the unpublished has been marked changed so on 'branch' a new version will be created
        documentWorkflow.obtainEditableInstance();
        documentWorkflow.commitEditableInstance();

        final String mountId = getNodeId(admin, "/hst:hst/hst:hosts/dev-localhost/localhost/hst:root");

        final Node frozenContainer = doVersionAndgetFrozenContainer();

        final String containerId = frozenContainer.getIdentifier();
        final String itemId1 = frozenContainer.getNode("banner").getIdentifier();
        final String itemId2 = frozenContainer.getNode("banner-new-style").getIdentifier();

        final RequestResponseMock updateRequestResponse = mockGetRequestResponse(
                "http", "localhost", "/_rp/" + containerId, null,
                "PUT");

        final ContainerRepresentation containerRepresentation = new ContainerRepresentation();

        containerRepresentation.setId(containerId);
        // move item2 before item1
        containerRepresentation.setChildren(Stream.of(itemId2, itemId1).collect(Collectors.toList()));

        updateRequestResponse.getRequest().setContent(objectMapper.writeValueAsBytes(containerRepresentation));
        updateRequestResponse.getRequest().setContentType("application/json;charset=UTF-8");

        final MockHttpServletResponse updateResponse = render(mountId, updateRequestResponse, ADMIN_CREDENTIALS);

        assertEquals(Response.Status.OK.getStatusCode(), updateResponse.getStatus());

        // master should have been checked out
        assertThat(unpublishedExpPageVariant.hasProperty(HIPPO_PROPERTY_BRANCH_ID)).isFalse();



        // do not reuse containerId from above since that one is from version history
        final Node container = admin.getNodeByIdentifier(getNodeId(admin,
                unpublishedExpPageVariant.getPath() + "/hst:xpage/430df2da-3dc8-40b5-bed5-bdc44b8445c6"));
        // assert container items have been flipped

        final NodeIterator children = container.getNodes();

        assertEquals("banner-new-style", children.nextNode().getName());
        assertEquals("banner", children.nextNode().getName());


        assertEquals("Unpublished has changes, publication should be enabled",
                TRUE, documentWorkflow.hints().get("publish"));

        assertRequiresReload(updateResponse, true);
    }


    @Test
    public void move_container_item_between_container_of_same_XPage() throws Exception {

        final String mountId = getNodeId(admin, "/hst:hst/hst:hosts/dev-localhost/localhost/hst:root");

        final String targetContainerId = getNodeId(admin, unpublishedExpPageVariant.getPath() + "/hst:xpage/430df2da-3dc8-40b5-bed5-bdc44b8445c7");
        final String itemId = getNodeId(admin, unpublishedExpPageVariant.getPath() + "/hst:xpage/430df2da-3dc8-40b5-bed5-bdc44b8445c6/banner");

        final RequestResponseMock updateRequestResponse = mockGetRequestResponse(
                "http", "localhost", "/_rp/" + targetContainerId, null,
                "PUT");

        final ContainerRepresentation containerRepresentation = new ContainerRepresentation();

        containerRepresentation.setId(targetContainerId);
        // move item to other container
        containerRepresentation.setChildren(Stream.of(itemId).collect(Collectors.toList()));

        updateRequestResponse.getRequest().setContent(objectMapper.writeValueAsBytes(containerRepresentation));
        updateRequestResponse.getRequest().setContentType("application/json;charset=UTF-8");

        final Node sourceContainer = admin.getNode(unpublishedExpPageVariant.getPath() + "/hst:xpage/430df2da-3dc8-40b5-bed5-bdc44b8445c6");
        // banner and banner-new-style container items expected
        assertEquals(2l, sourceContainer.getNodes().getSize());

        final MockHttpServletResponse updateResponse = render(mountId, updateRequestResponse, ADMIN_CREDENTIALS);

        assertEquals(Response.Status.OK.getStatusCode(), updateResponse.getStatus());

        // assert  second container now has the item
        final Node targetContainer = admin.getNodeByIdentifier(targetContainerId);

        final NodeIterator children = targetContainer.getNodes();
        // the container already got an item 'banner'
        assertEquals("banner", children.nextNode().getName());
        // the extra item moved into it should have a postfix to its name
        assertEquals("Expected postfix '1' to banner moved to container to avoid same name",
                "banner1", children.nextNode().getName());


        // only the banner-new-style expected to be left
        assertEquals(1l, sourceContainer.getNodes().getSize());

        final DocumentWorkflow documentWorkflow = getDocumentWorkflow(admin);

        assertEquals("Unpublished has changes, publication should be enabled",
                TRUE, documentWorkflow.hints().get("publish"));


    }

    /**
     * The unit test document expPage-with-static-components contains a container which is not backed by the XPage
     * Layout at hst:xpage/extra/extra-container-xpage-doc-only
     * This test is to assert that we can modify those containers as well without problem
     * @throws Exception
     */
    @Test
    public void move_to_and_create_container_item_in_NON_XPAGE_LAYOUT_container() throws Exception {
        try {
            // ******* SETUP FIXTURE ************ //
            JcrUtils.copy(admin, EXPERIENCE_PAGE_WITH_STATIC_COMPONENTS_HANDLE_PATH, "/backupXPage");
            admin.save();

            // make sure the unpublished variant exists (just by depublishing for now....)
            final WorkflowManager workflowManager = ((HippoSession) admin).getWorkspace().getWorkflowManager();

            handle = admin.getNode(EXPERIENCE_PAGE_WITH_STATIC_COMPONENTS_HANDLE_PATH);
            final DocumentWorkflow documentWorkflow = (DocumentWorkflow) workflowManager.getWorkflow("default", handle);
            documentWorkflow.depublish();
            // and publish again such that there is a live variant
            documentWorkflow.publish();

            // ******* END SETUP FIXTURE ************ //

            Node unpublished = getVariant(handle, "unpublished");

            // ********** MOVE ***********

            final String mountId = getNodeId(admin, "/hst:hst/hst:hosts/dev-localhost/localhost/hst:root");

            final String targetContainerId = getNodeId(admin, unpublished.getPath() + "/hst:xpage/extra/extra-container-xpage-doc-only");
            final String itemId = getNodeId(admin, unpublished.getPath() + "/hst:xpage/430df2da-3dc8-40b5-bed5-bdc44b8445c6/banner");

            final RequestResponseMock updateRequestResponse = mockGetRequestResponse(
                    "http", "localhost", "/_rp/" + targetContainerId, null,
                    "PUT");

            final ContainerRepresentation containerRepresentation = new ContainerRepresentation();

            containerRepresentation.setId(targetContainerId);
            // move item to other container
            containerRepresentation.setChildren(Stream.of(itemId).collect(Collectors.toList()));

            updateRequestResponse.getRequest().setContent(objectMapper.writeValueAsBytes(containerRepresentation));
            updateRequestResponse.getRequest().setContentType("application/json;charset=UTF-8");

            final Node sourceContainer = admin.getNode(unpublished.getPath() + "/hst:xpage/430df2da-3dc8-40b5-bed5-bdc44b8445c6");
            // banner container items expected
            assertEquals(1l, sourceContainer.getNodes().getSize());

            final MockHttpServletResponse updateResponse = render(mountId, updateRequestResponse, ADMIN_CREDENTIALS);

            assertEquals(Response.Status.OK.getStatusCode(), updateResponse.getStatus());

            // assert  second container now has the item
            final Node targetContainer = admin.getNodeByIdentifier(targetContainerId);

            final NodeIterator children = targetContainer.getNodes();
            assertEquals(1l, children.getSize());
            // the container already got an item 'banner'
            assertEquals("banner", children.nextNode().getName());

            // banner should have been moved
            assertEquals(0l, sourceContainer.getNodes().getSize());

            // ********** END MOVE **********

            // ********** CREATE AN ITEM (as author) ******

            final String catalogId = getNodeId(admin, "/hst:hst/hst:configurations/hst:default/hst:catalog/testpackage/newstyle-testitem");

            final RequestResponseMock createRequestResponse = mockGetRequestResponse(
                    "http", "localhost", "/_rp/" + targetContainerId + "./" + catalogId, null,
                    "POST");


            final MockHttpServletResponse createResponse = render(mountId, createRequestResponse, AUTHOR_CREDENTIALS);
            final ExtResponseRepresentation extResponseRepresentation = mapper.readerFor(ExtResponseRepresentation.class).readValue(createResponse.getContentAsString());

            assertEquals(CREATED.getStatusCode(), createResponse.getStatus());
            // assertion on newly created item
            assertTrue(admin.nodeExists(unpublished.getPath() + "/hst:xpage/extra/extra-container-xpage-doc-only/newstyle-testitem"));
            // ********** END CREATE AN ITEM  ******
        } finally {
            if (admin.nodeExists("/backupXPage")) {
                admin.getNode(EXPERIENCE_PAGE_WITH_STATIC_COMPONENTS_HANDLE_PATH).remove();
                admin.move("/backupXPage", EXPERIENCE_PAGE_WITH_STATIC_COMPONENTS_HANDLE_PATH);
                admin.save();
            }
        }
    }


    @Test
    public void move_container_item_between_container_of_different_XPages_is_not_allowed() throws Exception {

        final String mountId = getNodeId(admin, "/hst:hst/hst:hosts/dev-localhost/localhost/hst:root");

        // Container of a different XPage than itemId
        final String targetContainerId = getNodeId(admin, "/unittestcontent/documents/unittestproject/experiences/expPage2/expPage2/hst:xpage/430df2da-3dc8-40b5-bed5-bdc44b8445c6");
        final String itemId = getNodeId(admin, unpublishedExpPageVariant.getPath() + "/hst:xpage/430df2da-3dc8-40b5-bed5-bdc44b8445c6/banner");

        final RequestResponseMock updateRequestResponse = mockGetRequestResponse(
                "http", "localhost", "/_rp/" + targetContainerId, null,
                "PUT");

        final ContainerRepresentation containerRepresentation = new ContainerRepresentation();

        containerRepresentation.setId(targetContainerId);
        // move item to other container
        containerRepresentation.setChildren(Stream.of(itemId).collect(Collectors.toList()));

        updateRequestResponse.getRequest().setContent(objectMapper.writeValueAsBytes(containerRepresentation));
        updateRequestResponse.getRequest().setContentType("application/json;charset=UTF-8");

        final MockHttpServletResponse updateResponse = render(mountId, updateRequestResponse, ADMIN_CREDENTIALS);

        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), updateResponse.getStatus());

        final DocumentWorkflow documentWorkflow = getDocumentWorkflow(admin);

        assertEquals("Unpublished should not have changes",
                FALSE, documentWorkflow.hints().get("publish"));

    }


    /**
     * <p> It is not allowed to move a container item from HST Configuration to an XPage: A document has a different
     * life cycle than HST configuration, thus if we would support such a move, we'd get problems if either the XPage or
     * HST Config gets published </p> <p> This tests covers the move of an an HST Config container item to XPage
     * container. The reverse test is covered in {@link ContainerComponentResourceTest} </p>
     */
    @Test
    public void move_container_item_from_hst_config_to_XPage_is_not_allowed() throws Exception {

        // FIRST add a container item to HST CONFIG
        Session session = backupHstAndCreateWorkspace();

        try {
            // create a container and container item  in workspace
            String[] content = new String[]{
                    "/hst:hst/hst:configurations/unittestproject/hst:workspace/hst:pages/containertestpage", "hst:component",
                    "/hst:hst/hst:configurations/unittestproject/hst:workspace/hst:pages/containertestpage/main", "hst:component",
                    "/hst:hst/hst:configurations/unittestproject/hst:workspace/hst:pages/containertestpage/main/container", "hst:containercomponent",
                    "hst:xtype", "hst.vbox",
                    "/hst:hst/hst:configurations/unittestproject/hst:workspace/hst:pages/containertestpage/main/container/banner", "hst:containeritemcomponent",
                    "hst:componentclassname", "org.hippoecm.hst.test.BannerComponent",
            };

            RepositoryTestCase.build(content, session);

            session.save();

            final String mountId = getNodeId(session, "/hst:hst/hst:hosts/dev-localhost/localhost/hst:root");

            final String targetContainerId = getNodeId(session, unpublishedExpPageVariant.getPath() + "/hst:xpage/430df2da-3dc8-40b5-bed5-bdc44b8445c6");

            final String itemFromHstConfig = getNodeId(session, "/hst:hst/hst:configurations/unittestproject/hst:workspace/hst:pages/containertestpage/main/container/banner");

            final RequestResponseMock updateContainerReqRes = mockGetRequestResponse(
                    "http", "localhost", "/_rp/" + targetContainerId, null, "PUT");

            final ContainerRepresentation containerRepresentation = new ContainerRepresentation();

            containerRepresentation.setId(targetContainerId);
            // try to move itemFromXPage to other container
            containerRepresentation.setChildren(Stream.of(itemFromHstConfig).collect(Collectors.toList()));

            updateContainerReqRes.getRequest().setContent(objectMapper.writeValueAsBytes(containerRepresentation));
            updateContainerReqRes.getRequest().setContentType("application/json;charset=UTF-8");

            final MockHttpServletResponse updateResponse = render(mountId, updateContainerReqRes, ADMIN_CREDENTIALS);

            assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), updateResponse.getStatus());

        } finally {
            restoreHstConfigBackup(session);
            session.logout();
        }
    }

    @Test
    public void move_container_item_which_does_not_exist_is_bad_request() throws Exception {

        final String mountId = getNodeId(admin, "/hst:hst/hst:hosts/dev-localhost/localhost/hst:root");

        final String containerId = getNodeId(admin,unpublishedExpPageVariant.getPath() + "/hst:xpage/430df2da-3dc8-40b5-bed5-bdc44b8445c6");

        final RequestResponseMock updateRequestResponse = mockGetRequestResponse(
                "http", "localhost", "/_rp/" + containerId, null,
                "PUT");

        final ContainerRepresentation containerRepresentation = new ContainerRepresentation();

        containerRepresentation.setId(containerId);
        // move non existing item
        containerRepresentation.setChildren(Stream.of(UUID.randomUUID().toString()).collect(Collectors.toList()));

        updateRequestResponse.getRequest().setContent(objectMapper.writeValueAsBytes(containerRepresentation));
        updateRequestResponse.getRequest().setContentType("application/json;charset=UTF-8");

        final MockHttpServletResponse updateResponse = render(mountId, updateRequestResponse, ADMIN_CREDENTIALS);

        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), updateResponse.getStatus());
    }

    @Test
    public void move_invalid_container_item_id_is_bad_request() throws Exception {

        final String mountId = getNodeId(admin, "/hst:hst/hst:hosts/dev-localhost/localhost/hst:root");

        final String containerId = getNodeId(unpublishedExpPageVariant.getPath() + "/hst:xpage/430df2da-3dc8-40b5-bed5-bdc44b8445c6");

        final RequestResponseMock updateRequestResponse = mockGetRequestResponse(
                "http", "localhost", "/_rp/" + containerId, null,
                "PUT");

        final ContainerRepresentation containerRepresentation = new ContainerRepresentation();

        containerRepresentation.setId(containerId);
        // move non existing item
        containerRepresentation.setChildren(Stream.of("invalid-UUID").collect(Collectors.toList()));

        updateRequestResponse.getRequest().setContent(objectMapper.writeValueAsBytes(containerRepresentation));
        updateRequestResponse.getRequest().setContentType("application/json;charset=UTF-8");

        final MockHttpServletResponse updateResponse = render(mountId, updateRequestResponse, ADMIN_CREDENTIALS);

        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), updateResponse.getStatus());
    }

    @Test
    public void creating_container_item_not_allowed_if_request_for_publication_present() throws RepositoryException, IOException, WorkflowException, ServletException {

        final Session authorSession = createSession(AUTHOR_CREDENTIALS);
        try {
            final DocumentWorkflow documentWorkflow = getDocumentWorkflow(authorSession);
            final Document document = documentWorkflow.obtainEditableInstance();
            final Node handleNode = document.getNode(authorSession).getParent();
            final Node draftNode = getVariant(handleNode, HippoStdNodeType.DRAFT);
            draftNode.setProperty(HippoNodeType.HIPPO_NAME, "TEST");
            documentWorkflow.commitEditableInstance();

            documentWorkflow.requestPublication();

            final String mountId = getNodeId(admin, "/hst:hst/hst:hosts/dev-localhost/localhost/hst:root");
            final String containerId = getNodeId(admin, unpublishedExpPageVariant.getPath() + "/hst:xpage/430df2da-3dc8-40b5-bed5-bdc44b8445c6");
            final String catalogId = getNodeId(admin, "/hst:hst/hst:configurations/hst:default/hst:catalog/testpackage/oldstyle-testitem");

            final RequestResponseMock createRequestResponse = mockGetRequestResponse(
                    "http", "localhost", "/_rp/" + containerId + "./" + catalogId, null,
                    "POST");

            final MockHttpServletResponse createResponse = render(mountId, createRequestResponse, ADMIN_CREDENTIALS);
            Assertions.assertThat(createResponse.getStatus())
                    .isEqualTo(BAD_REQUEST.getStatusCode());

            final Map<String, ?> createResponseMap = mapper.readerFor(Map.class).readValue(createResponse.getContentAsString());
            Assertions.assertThat(createResponseMap.get("success"))
                    .isEqualTo(FALSE);
            Assertions.assertThat(createResponseMap.get("message"))
                    .isEqualTo("Document not editable");
        } finally {
            authorSession.logout();
        }
    }
}
