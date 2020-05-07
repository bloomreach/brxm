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
package org.hippoecm.hst.pagecomposer.jaxrs.services.experiencepage;

import java.rmi.RemoteException;
import java.util.Calendar;
import java.util.List;
import java.util.Optional;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.http.HttpSession;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.hippoecm.hst.configuration.HstNodeTypes;
import org.hippoecm.hst.container.RequestContextProvider;
import org.hippoecm.hst.core.container.ContainerConstants;
import org.hippoecm.hst.pagecomposer.jaxrs.api.annotation.PrivilegesAllowed;
import org.hippoecm.hst.pagecomposer.jaxrs.model.ContainerItem;
import org.hippoecm.hst.pagecomposer.jaxrs.model.ContainerItemImpl;
import org.hippoecm.hst.pagecomposer.jaxrs.model.ContainerItemRepresentation;
import org.hippoecm.hst.pagecomposer.jaxrs.model.ContainerRepresentation;
import org.hippoecm.hst.pagecomposer.jaxrs.model.ErrorStatus;
import org.hippoecm.hst.pagecomposer.jaxrs.services.AbstractConfigResource;
import org.hippoecm.hst.pagecomposer.jaxrs.services.ContainerComponentResourceInterface;
import org.hippoecm.hst.pagecomposer.jaxrs.services.PageComposerContextService;
import org.hippoecm.hst.pagecomposer.jaxrs.services.exceptions.ClientError;
import org.hippoecm.hst.pagecomposer.jaxrs.services.exceptions.ClientException;
import org.hippoecm.hst.pagecomposer.jaxrs.services.util.ContainerUtils;
import org.hippoecm.repository.api.HippoSession;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.util.JcrUtils;
import org.hippoecm.repository.util.WorkflowUtils;
import org.onehippo.cms7.services.cmscontext.CmsSessionContext;
import org.onehippo.repository.documentworkflow.DocumentWorkflow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hippoecm.hst.configuration.HstNodeTypes.GENERAL_PROPERTY_LAST_MODIFIED;
import static org.hippoecm.hst.pagecomposer.jaxrs.cxf.CXFJaxrsHstConfigService.REQUEST_EXPERIENCE_PAGE_UNPUBLISHED_UUID_VARIANT_ATRRIBUTE;
import static org.hippoecm.hst.pagecomposer.jaxrs.services.experiencepage.XPageUtils.checkoutCorrectBranch;
import static org.hippoecm.hst.pagecomposer.jaxrs.services.experiencepage.XPageUtils.getContainer;
import static org.hippoecm.hst.pagecomposer.jaxrs.services.experiencepage.XPageUtils.getDocumentWorkflow;
import static org.hippoecm.hst.pagecomposer.jaxrs.services.experiencepage.XPageUtils.getInternalWorkflowSession;
import static org.hippoecm.hst.pagecomposer.jaxrs.util.UUIDUtils.isValidUUID;
import static org.hippoecm.hst.platform.services.channel.ChannelManagerPrivileges.XPAGE_REQUIRED_PRIVILEGE_NAME;
import static org.hippoecm.repository.HippoStdNodeType.HIPPOSTD_HOLDER;
import static org.hippoecm.repository.util.JcrUtils.getNodePathQuietly;
import static org.hippoecm.repository.util.JcrUtils.getStringProperty;
import static org.hippoecm.repository.util.WorkflowUtils.Variant.DRAFT;
import static org.onehippo.repository.branch.BranchConstants.MASTER_BRANCH_ID;

@Path("/experiencepage/hst:containercomponent/")
public class XPageContainerComponentResource extends AbstractConfigResource implements ContainerComponentResourceInterface {
    private static Logger log = LoggerFactory.getLogger(XPageContainerComponentResource.class);

    @FunctionalInterface
    interface ContainerAction<R> {
        R apply() throws RepositoryException, WorkflowException;
    }

    @POST
    @Path("/{itemUUID}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @PrivilegesAllowed(XPAGE_REQUIRED_PRIVILEGE_NAME)
    public Response createContainerItem(final @PathParam("itemUUID") String catalogItemUUID,
                                        final @QueryParam("lastModifiedTimestamp") long versionStamp) {

        return createContainerItemAndAddBefore(catalogItemUUID, null, versionStamp);

    }


    @POST
    @Path("/{itemUUID}/{siblingItemUUID}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @PrivilegesAllowed(XPAGE_REQUIRED_PRIVILEGE_NAME)
    public Response createContainerItemAndAddBefore(final @PathParam("itemUUID") String itemUUID,
                                                    final @PathParam("siblingItemUUID") String siblingItemUUID,
                                                    final @QueryParam("lastModifiedTimestamp") long versionStamp) {
        if (!isValidUUID(itemUUID)) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(String.format("Value of path parameter itemUUID: '%s' is not a valid UUID", itemUUID))
                    .build();
        }
        if (siblingItemUUID != null && !isValidUUID(siblingItemUUID)) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(String.format("Value of path parameter siblingItemUUID: '%s' is not a valid UUID", siblingItemUUID))
                    .build();
        }

        final ContainerAction<Response> createContainerItem = () -> {

            final DocumentWorkflow documentWorkflow = getDocumentWorkflow(getSession(), getPageComposerContextService());

            final Session workflowSession = getInternalWorkflowSession(documentWorkflow);

            // with workflowSession, we write directly to the unpublished variant : the container from the request
            // is of the 'preview' variant (mind you, this can be the preview of a branch loaded from version history!)
            final Node catalogItem = ContainerUtils.getContainerItem(workflowSession, itemUUID);

            final Node containerNode = getContainer(versionStamp, workflowSession, getPageComposerContextService());

            // now we have the catalogItem that contains 'how' to create the new containerItem and we have the
            // containerNode. Find a correct newName and create a new node.
            final String newItemNodeName = ContainerUtils.findNewName(catalogItem.getName(), containerNode);
            final Node newItem = JcrUtils.copy(workflowSession, catalogItem.getPath(), containerNode.getPath() + "/" + newItemNodeName);

            if (siblingItemUUID != null) {
                final Node siblingItem = ContainerUtils.getContainerItem(workflowSession, siblingItemUUID);
                if (!siblingItem.getPath().startsWith(containerNode.getPath() + "/")) {
                    throw new ClientException(String.format("Order before container item '%s' is of other experience page",
                            siblingItemUUID), ClientError.INVALID_UUID);
                }
                containerNode.orderBefore(newItem.getName(), siblingItem.getName());
            }
            final Calendar updatedTimestamp = updateTimestamp(containerNode);

            documentWorkflow.saveUnpublished();

            return respondNewContainerItemCreated(new ContainerItemImpl(newItem, updatedTimestamp.getTimeInMillis()));

        };

        return handleAction(createContainerItem);
    }

    private Calendar updateTimestamp(final Node containerNode) throws RepositoryException {
        // update last modified for optimistic locking
        // TODO Do we want this optimistic locking in this way?
        final Calendar updatedTimestamp = Calendar.getInstance();
        containerNode.setProperty(HstNodeTypes.GENERAL_PROPERTY_LAST_MODIFIED, updatedTimestamp);
        return updatedTimestamp;
    }


    @PUT
    @Path("/")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @PrivilegesAllowed(XPAGE_REQUIRED_PRIVILEGE_NAME)
    public Response updateContainer(final ContainerRepresentation container) {
        final ContainerAction<Response> updateContainer = () -> {

            final DocumentWorkflow documentWorkflow = getDocumentWorkflow(getSession(), getPageComposerContextService());

            final Session workflowSession = getInternalWorkflowSession(documentWorkflow);
            final Node containerNode = getContainer(container.getLastModifiedTimestamp(), workflowSession, getPageComposerContextService());

            validateContainerItems(workflowSession, container.getChildren());

            ContainerUtils.updateContainerOrder(workflowSession, container.getChildren(), containerNode, node -> {
                // no locking needed for XPages;
            });

            // update last modified for optimistic locking
            // TODO Do we want this optimistic locking in this way?
            updateTimestamp(containerNode);

            documentWorkflow.saveUnpublished();

            return Response.status(Response.Status.OK).entity(container).build();
        };

        return handleAction(updateContainer);
    }

    /**
     * assert all children are of type 'hst:containeritemcomponent' and that they are ALL descendants of the currently
     * edited XPage : it is not allowed to move children from outside the current XPage into this XPage
     *
     * @throws ClientException in case the {@code childsIds} are not valid
     */
    private void validateContainerItems(final Session workflowSession, final List<String> childIds) throws RepositoryException, ClientException {

        final String unpublishedId = (String) getPageComposerContextService().getRequestContext().getAttribute(REQUEST_EXPERIENCE_PAGE_UNPUBLISHED_UUID_VARIANT_ATRRIBUTE);

        final Node unpublished = workflowSession.getNodeByIdentifier(unpublishedId);

        final String pathPrefix = unpublished.getPath() + "/";

        for (String childId : childIds) {
            if (!isValidUUID(childId)) {
                throw new ClientException(String.format("Invalid child id '%s'", childId), ClientError.INVALID_UUID);
            }
            try {
                final Node componentItem = workflowSession.getNodeByIdentifier(childId);
                if (!componentItem.getPath().startsWith(pathPrefix)) {
                    throw new ClientException(String.format("Child '%s' is not allowed to be moved within the XPage '%s' " +
                                    "because it is not a descendant of the XPage",
                            componentItem.getPath(), unpublished.getPath()), ClientError.ITEM_NOT_CORRECT_LOCATION);
                }
                if (!componentItem.isNodeType(HstNodeTypes.NODETYPE_HST_CONTAINERITEMCOMPONENT)) {
                    throw new ClientException(String.format("Child '%s' has not a valid nodetype for a container",
                            componentItem.getPath()), ClientError.INVALID_NODE_TYPE);
                }
            } catch (ItemNotFoundException e) {
                throw new ClientException("Could not find one of the children in the container", ClientError.INVALID_UUID);
            }
        }

    }

    @DELETE
    @Path("/{itemUUID}")
    @Produces(MediaType.APPLICATION_JSON)
    @PrivilegesAllowed(XPAGE_REQUIRED_PRIVILEGE_NAME)
    public Response deleteContainerItem(final @PathParam("itemUUID") String itemUUID,
                                        final @QueryParam("lastModifiedTimestamp") long versionStamp) {
        final ContainerAction<Response> deleteContainerItem = () -> {


            HippoSession session = getSession();
            try {
                final Node containerItem = session.getNodeByIdentifier(itemUUID);

                final Node container = getPageComposerContextService().getRequestConfigNode("hst:abstractcomponent");

                if (!containerItem.getPath().startsWith(container.getPath() + "/")) {
                    throw new ClientException("Cannot delete container item of other document", ClientError.INVALID_UUID);
                }

                DocumentWorkflow documentWorkflow = getDocumentWorkflow(session, getPageComposerContextService());

                final Session internalWorkflowSession = getInternalWorkflowSession(documentWorkflow);
                internalWorkflowSession.getNodeByIdentifier(itemUUID).remove();
                documentWorkflow.saveUnpublished();

                return Response.status(Response.Status.OK).build();
            } catch (ItemNotFoundException e) {
                throw new ClientException("Item to delete not found", ClientError.INVALID_UUID);
            }
        };

        return handleAction(deleteContainerItem);
    }

    private HippoSession getSession() throws RepositoryException {
        return (HippoSession) RequestContextProvider.get().getSession();
    }

    private Response handleAction(final ContainerAction<Response> action) {
        try {
            return action.apply();
        } catch (ClientException e) {
            return clientError(e.getMessage(), e.getErrorStatus());
        } catch (RepositoryException | WorkflowException | IllegalArgumentException e) {
            return error(e.getMessage(), ErrorStatus.unknown(e.getMessage()));
        }
    }

    private Response respondNewContainerItemCreated(ContainerItem newContainerItem) throws RepositoryException {
        final Node newNode = newContainerItem.getContainerItem();
        final ContainerItemRepresentation containerItemRepresentation = new ContainerItemRepresentation().represent(newNode, newContainerItem.getTimeStamp());

        log.info("Successfully created containerItemRepresentation '{}' with path '{}'", newNode.getName(), newNode.getPath());
        return Response.status(Response.Status.CREATED)
                .entity(containerItemRepresentation)
                .build();
    }


}
