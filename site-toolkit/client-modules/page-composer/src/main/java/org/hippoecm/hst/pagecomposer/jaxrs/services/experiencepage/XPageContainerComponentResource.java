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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
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

import org.apache.jackrabbit.JcrConstants;
import org.hippoecm.hst.configuration.HstNodeTypes;
import org.hippoecm.hst.configuration.components.HstComponentConfiguration;
import org.hippoecm.hst.container.RequestContextProvider;
import org.hippoecm.hst.pagecomposer.jaxrs.api.annotation.PrivilegesAllowed;
import org.hippoecm.hst.pagecomposer.jaxrs.model.ContainerItemImpl;
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
import org.onehippo.repository.documentworkflow.DocumentWorkflow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.lang.String.format;
import static org.hippoecm.hst.configuration.HstNodeTypes.COMPONENT_PROPERTY_COMPONENTDEFINITION;
import static org.hippoecm.hst.configuration.HstNodeTypes.NODETYPE_HST_CONTAINERITEMCOMPONENT;
import static org.hippoecm.hst.pagecomposer.jaxrs.cxf.CXFJaxrsHstConfigService.REQUEST_EXPERIENCE_PAGE_UNPUBLISHED_UUID_VARIANT_ATRRIBUTE;
import static org.hippoecm.hst.pagecomposer.jaxrs.services.experiencepage.XPageUtils.checkoutCorrectBranch;
import static org.hippoecm.hst.pagecomposer.jaxrs.services.experiencepage.XPageUtils.getObtainEditableInstanceWorkflow;
import static org.hippoecm.hst.pagecomposer.jaxrs.services.experiencepage.XPageUtils.getInternalWorkflowSession;
import static org.hippoecm.hst.pagecomposer.jaxrs.services.experiencepage.XPageUtils.getWorkspaceNode;
import static org.hippoecm.hst.pagecomposer.jaxrs.services.experiencepage.XPageUtils.validateTimestamp;
import static org.hippoecm.hst.pagecomposer.jaxrs.services.util.ContainerUtils.createComponentItem;
import static org.hippoecm.hst.pagecomposer.jaxrs.services.util.ContainerUtils.findNewName;
import static org.hippoecm.hst.pagecomposer.jaxrs.services.util.ContainerUtils.getCatalogItem;
import static org.hippoecm.hst.pagecomposer.jaxrs.util.UUIDUtils.isValidUUID;
import static org.hippoecm.hst.platform.services.channel.ChannelManagerPrivileges.XPAGE_REQUIRED_PRIVILEGE_NAME;

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
            final String message = format("Value of path parameter itemUUID: '%s' is not a valid UUID", itemUUID);
            return clientError(message, new ErrorStatus(ClientError.INVALID_UUID));
        }
        if (siblingItemUUID != null && !isValidUUID(siblingItemUUID)) {
            final String message = format("Value of path parameter siblingItemUUID: '%s' is not a valid UUID", siblingItemUUID);
            return clientError(message, new ErrorStatus(ClientError.INVALID_UUID));
        }
        final ContainerAction<Response> createContainerItem = () -> {

            final PageComposerContextService pageComposerContextService = getPageComposerContextService();

            final HippoSession userSession = getSession();
            final DocumentWorkflow documentWorkflow = getObtainEditableInstanceWorkflow(userSession, pageComposerContextService);

            final boolean isCheckedOut = checkoutCorrectBranch(documentWorkflow, pageComposerContextService);

            final Session workflowSession = getInternalWorkflowSession(documentWorkflow);

            // with workflowSession, we write directly to the unpublished variant : the container from the request
            // is of the 'preview' variant (mind you, this can be the preview of a branch loaded from version history!)
            final Node catalogItem = ContainerUtils.getContainerItem(workflowSession, itemUUID);

            final Node containerNode = getWorkspaceNode(workflowSession, getPageComposerContextService().getRequestConfigIdentifier());

            // we can always validate the timestamp against the workspace version, since the 'checkoutCorrectBranch'
            // does already fail when a too old version is attempted to be restored: of course are a restore, the
            // timestamp check always passes since a check is done against an older restored version
            // TODO validate in integration test, including version history checked out branches
            validateTimestamp(versionStamp, containerNode, userSession.getUserID());

            final HstComponentConfiguration componentDefinition = getCatalogItem(pageComposerContextService, catalogItem);


            final Node newItem = createComponentItem(containerNode, catalogItem, componentDefinition);

            if (siblingItemUUID != null) {
                try {
                    final Node siblingItem = getWorkspaceNode(workflowSession, siblingItemUUID);
                    if (!siblingItem.isNodeType(NODETYPE_HST_CONTAINERITEMCOMPONENT)) {
                        throw new ClientException(format("The container item '%s' does not have the correct type",
                                siblingItemUUID), ClientError.INVALID_NODE_TYPE);
                    }
                    if (!siblingItem.getPath().startsWith(containerNode.getPath() + "/")) {
                        throw new ClientException(format("Order before container item '%s' is of other experience page",
                                siblingItemUUID), ClientError.INVALID_UUID);
                    }
                    containerNode.orderBefore(newItem.getName(), siblingItem.getName());
                } catch (ItemNotFoundException e) {
                    log.info("Cannot find container item '{}'.", itemUUID);
                    throw new ClientException(format("Cannot find container item '%s'",
                            siblingItemUUID), ClientError.INVALID_UUID);
                }

            }
            final Calendar updatedTimestamp = updateTimestamp(containerNode);

            documentWorkflow.saveUnpublished();

            return respondContainerItem(new ContainerItemImpl(newItem, componentDefinition, updatedTimestamp.getTimeInMillis()), isCheckedOut,
                    Response.Status.CREATED, "Successfully created item");

        };

        return handleAction(createContainerItem);
    }

    private Calendar updateTimestamp(final Node containerNode) throws RepositoryException {
        // update last modified for optimistic locking
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

            final PageComposerContextService pageComposerContextService = getPageComposerContextService();

            final HippoSession userSession = getSession();
            final DocumentWorkflow documentWorkflow = getObtainEditableInstanceWorkflow(userSession, pageComposerContextService);
            final Session workflowSession = getInternalWorkflowSession(documentWorkflow);

            final boolean isCheckedOut = checkoutCorrectBranch(documentWorkflow, pageComposerContextService);

            final Node containerNode = getWorkspaceNode(workflowSession, getPageComposerContextService().getRequestConfigIdentifier());

            // we can always validate the timestamp against the workspace version, since the 'checkoutCorrectBranch'
            // does already fail when a too old version is attempted to be restored: of course are a restore, the
            // timestamp check always passes since a check is done against an older restored version
            // TODO validate in integration test, including version history checked out branches
            validateTimestamp(container.getLastModifiedTimestamp(), containerNode, userSession.getUserID());

            final List<String> children = getWorkspaceChildren(workflowSession, container.getChildren());

            validateContainerItems(workflowSession, children);

            ContainerUtils.updateContainerOrder(workflowSession, children, containerNode, node -> {
                // no locking needed for XPages;
            });

            // update last modified for optimistic locking
            final Calendar calendar = updateTimestamp(containerNode);
            container.setLastModifiedTimestamp(calendar.getTimeInMillis());

            documentWorkflow.saveUnpublished();

            return ok(container.getId() + " updated", container, isCheckedOut);
        };

        return handleAction(updateContainer);
    }

    // returns the UUIDs of the workspace version for children in case children belong to versioned nodes
    private List<String> getWorkspaceChildren(final Session session, final List<String> childIds) throws RepositoryException {
        final List<String> workspaceChildren = new ArrayList<>(childIds.size());
        for (String childId : childIds) {
            if (!isValidUUID(childId)) {
                throw new ClientException(format("Invalid child id '%s'", childId), ClientError.INVALID_UUID);
            }
            try {
                workspaceChildren.add(getWorkspaceNode(session, childId).getIdentifier());
            } catch (ItemNotFoundException e) {
                throw new ClientException(format("Could not find workspace node for child id '%s'", childId), ClientError.INVALID_UUID);
            }
        }
        return workspaceChildren;
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

        ContainerUtils.validateChildren(workflowSession, childIds, pathPrefix);

    }

    @DELETE
    @Path("/{itemUUID}")
    @Produces(MediaType.APPLICATION_JSON)
    @PrivilegesAllowed(XPAGE_REQUIRED_PRIVILEGE_NAME)
    public Response deleteContainerItem(final @PathParam("itemUUID") String itemUUID,
                                        final @QueryParam("lastModifiedTimestamp") long versionStamp) {
        final ContainerAction<Response> deleteContainerItem = () -> {

            try {

                final PageComposerContextService pageComposerContextService = getPageComposerContextService();

                final HippoSession userSession = getSession();
                final DocumentWorkflow documentWorkflow = getObtainEditableInstanceWorkflow(userSession, pageComposerContextService);

                final Session internalWorkflowSession = getInternalWorkflowSession(documentWorkflow);

                final boolean isCheckedOut = checkoutCorrectBranch(documentWorkflow, pageComposerContextService);

                final Node container = getWorkspaceNode(internalWorkflowSession, getPageComposerContextService().getRequestConfigIdentifier());

                // we can always validate the timestamp against the workspace version, since the 'checkoutCorrectBranch'
                // does already fail when a too old version is attempted to be restored: of course are a restore, the
                // timestamp check always passes since a check is done against an older restored version
                // TODO validate in integration test, including version history checked out branches
                validateTimestamp(versionStamp, container, userSession.getUserID());


                // itemUUID can belong to versioned component item, in that case, find the checked out workspace equivalent
                final Node item = internalWorkflowSession.getNodeByIdentifier(itemUUID);
                final Node workspaceContainerItem;
                if (item.isNodeType(JcrConstants.NT_FROZENNODE)) {
                    // get hold of the workspace container item! Since 'checkoutCorrectBranch' did already do the checkout,
                    // and a restore from version history restores the frozen uuid, we can safely get hold of that one
                    final String workspaceUUID = item.getProperty(JcrConstants.JCR_FROZENUUID).getString();
                    workspaceContainerItem = internalWorkflowSession.getNodeByIdentifier(workspaceUUID);
                } else {
                    workspaceContainerItem = item;
                }

                if (!workspaceContainerItem.getPath().startsWith(container.getPath() + "/")) {
                    throw new ClientException("Cannot delete container item of other document", ClientError.INVALID_UUID);
                }

                workspaceContainerItem.remove();

                // update last modified for optimistic locking
                updateTimestamp(container);

                documentWorkflow.saveUnpublished();

                // TODO the updated timestamp should be returned and used by the FE for the next call from this
                //      container
                return ok(itemUUID + " deleted", isCheckedOut);
            } catch (ItemNotFoundException e) {
                throw new ClientException("Item to delete not found", ClientError.INVALID_UUID);
            }
        };

        return handleAction(deleteContainerItem);
    }

    private HippoSession getSession() throws RepositoryException {
        return (HippoSession) RequestContextProvider.get().getSession();
    }

    private Response handleAction(ContainerAction<Response> action) {
        try {
            return action.apply();
        } catch (ClientException e) {
            log.info("Client Exception {} : {} ", e.getErrorStatus().getError(), e.toString());
            return clientError(e.getMessage(), e.getErrorStatus());
        } catch (RepositoryException | WorkflowException | IllegalArgumentException e) {
            log.info("Server error : {} ", e.toString());
            return error(e.getMessage(), ErrorStatus.unknown(e.getMessage()));
        }
    }


}
