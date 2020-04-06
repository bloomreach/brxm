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
import java.util.Optional;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
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
import org.hippoecm.hst.pagecomposer.jaxrs.services.ContainerComponentService;
import org.hippoecm.hst.pagecomposer.jaxrs.services.exceptions.ClientError;
import org.hippoecm.hst.pagecomposer.jaxrs.services.exceptions.ClientException;
import org.hippoecm.hst.pagecomposer.jaxrs.services.exceptions.InvalidNodeTypeException;
import org.hippoecm.repository.HippoStdNodeType;
import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.api.HippoSession;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.api.WorkflowManager;
import org.hippoecm.repository.util.JcrUtils;
import org.hippoecm.repository.util.WorkflowUtils;
import org.onehippo.cms7.services.cmscontext.CmsSessionContext;
import org.onehippo.repository.branch.BranchConstants;
import org.onehippo.repository.documentworkflow.DocumentWorkflow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hippoecm.hst.configuration.HstNodeTypes.GENERAL_PROPERTY_LAST_MODIFIED;
import static org.hippoecm.hst.configuration.HstNodeTypes.NODETYPE_HST_CONTAINERITEMCOMPONENT;
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

    private ContainerComponentService xPageContainerComponentService;

    public void setXPageContainerComponentService(ContainerComponentService xPageContainerComponentService) {
        this.xPageContainerComponentService = xPageContainerComponentService;
    }

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
        if (!isValidUUID(catalogItemUUID)) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(String.format("Value of path parameter itemUUID: '%s' is not a valid UUID", catalogItemUUID))
                    .build();
        }

        final ContainerAction<Response> createContainerItem = () -> {

            final HippoSession userSession = getSession();

            // userSession is allowed to read the node since has XPAGE_REQUIRED_PRIVILEGE_NAME on the node
            final Node handle = userSession.getNodeByIdentifier(getPageComposerContextService().getExperiencePageHandleUUID());

            // TODO is 'default' the right document workflow??
            // I think it is ...
            final DocumentWorkflow documentWorkflow = (DocumentWorkflow) userSession.getWorkspace().getWorkflowManager().getWorkflow("default", handle);

            final Node draftNode = WorkflowUtils.getDocumentVariantNode(handle, DRAFT).orElse(null);
            if ((draftNode != null)) {
                final String draftHolder = getStringProperty(draftNode, HIPPOSTD_HOLDER, null);
                final String userId = userSession.getUserID();
                if (!userId.equals(draftHolder)) {
                    return Response.status(Response.Status.BAD_REQUEST)
                            .entity(String.format("Holder of draft '%s' is not equal to user from session '%s'", draftHolder, userId))
                            .build();
                }
            }

            checkoutCorrectBranch(documentWorkflow);

            // we need to write with the workflowSession. Make sure to use this workflowSession and not
            // impersonate to a workflowSession : This way we can make sure that the workflow manager also
            // persists the changes since the workflow manager will handle the  workflow session save (when we invoke
            // the document workflow

            final Session workflowSession = documentWorkflow.getWorkflowContext().getInternalWorkflowSession();

            // with workflowSession, we write directly to the unpublished variant

            final Node catalogItem = getContainerItem(workflowSession, catalogItemUUID);

            final Node container = getPageComposerContextService().getRequestConfigNode("hst:abstractcomponent");

            if (versionStamp != 0 && container.hasProperty(GENERAL_PROPERTY_LAST_MODIFIED)) {
                long existingStamp = container.getProperty(GENERAL_PROPERTY_LAST_MODIFIED).getDate().getTimeInMillis();
                if (existingStamp != versionStamp) {
                    String msg = String.format("Node '%s' has been modified wrt versionStamp. Someone else might have " +
                            "made concurrent changes, page must be reloaded. This can happen due to optimistic locking",
                            getNodePathQuietly(container));
                    log.info(msg);
                    throw new ClientException(msg, ClientError.ITEM_CHANGED);
                }
            }
            // update last modified
            final Calendar updatedTimestamp = Calendar.getInstance();
            container.setProperty(HstNodeTypes.GENERAL_PROPERTY_LAST_MODIFIED, updatedTimestamp);

            // now we have the catalogItem that contains 'how' to create the new containerItem and we have the
            // containerNode. Find a correct newName and create a new node.
            final String newItemNodeName = findNewName(catalogItem.getName(), container);
            final Node newItem = JcrUtils.copy(workflowSession, catalogItem.getPath(), container.getPath() + "/" + newItemNodeName);


//            TODO
            try {
                documentWorkflow.saveUnpublished();
            } catch (WorkflowException e) {
                e.printStackTrace();
            }

            return respondNewContainerItemCreated(new ContainerItemImpl(newItem, updatedTimestamp.getTimeInMillis()));

        };
        return handleAction(createContainerItem);
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
        if (!isValidUUID(siblingItemUUID)) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(String.format("Value of path parameter siblingItemUUID: '%s' is not a valid UUID", siblingItemUUID))
                    .build();
        }
        return handleAction(() -> {
            final ContainerItem newContainerItem = xPageContainerComponentService.createContainerItem(getSession(), itemUUID, siblingItemUUID, versionStamp);
            return respondNewContainerItemCreated(newContainerItem);
        });
    }


    @PUT
    @Path("/")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @PrivilegesAllowed(XPAGE_REQUIRED_PRIVILEGE_NAME)
    public Response updateContainer(final ContainerRepresentation container) {
        final ContainerAction<Response> updateContainer = () -> {
            xPageContainerComponentService.updateContainer(getSession(), container);
            return Response.status(Response.Status.OK).entity(container).build();
        };

        return handleAction(updateContainer);
    }

    @DELETE
    @Path("/{itemUUID}")
    @Produces(MediaType.APPLICATION_JSON)
    @PrivilegesAllowed(XPAGE_REQUIRED_PRIVILEGE_NAME)
    public Response deleteContainerItem(final @PathParam("itemUUID") String itemUUID,
                                        final @QueryParam("lastModifiedTimestamp") long versionStamp) {
        final ContainerAction<Response> deleteContainerItem = () -> {
            xPageContainerComponentService.deleteContainerItem(getSession(), itemUUID, versionStamp);
            return Response.status(Response.Status.OK).build();
        };

        return handleAction(deleteContainerItem);
    }

    private HippoSession getSession() throws RepositoryException {
        return (HippoSession) RequestContextProvider.get().getSession();
    }

    private Response handleAction(final ContainerAction<Response> action) {
        final Response.Status httpStatusCode;
        final ErrorStatus errorStatus;

        try {
            return action.apply();
        } catch (ClientException e) {
            errorStatus = e.getErrorStatus();
            httpStatusCode = Response.Status.BAD_REQUEST;
        } catch (RepositoryException | WorkflowException | IllegalArgumentException e) {
            errorStatus = ErrorStatus.unknown(e.getMessage());
            httpStatusCode = Response.Status.INTERNAL_SERVER_ERROR;
        }
        return createErrorResponse(httpStatusCode, errorStatus);
    }

    private Response createErrorResponse(final Response.Status httpStatusCode, final ErrorStatus errorStatus) {
        return Response.status(httpStatusCode).entity(errorStatus).build();
    }

    private Response respondNewContainerItemCreated(ContainerItem newContainerItem) throws RepositoryException {
        final Node newNode = newContainerItem.getContainerItem();
        final ContainerItemRepresentation containerItemRepresentation = new ContainerItemRepresentation().represent(newNode, newContainerItem.getTimeStamp());

        log.info("Successfully created containerItemRepresentation '{}' with path '{}'", newNode.getName(), newNode.getPath());
        return Response.status(Response.Status.CREATED)
                .entity(containerItemRepresentation)
                .build();
    }


    private Document checkoutCorrectBranch(final DocumentWorkflow documentWorkflow) throws WorkflowException {
        // TODO checkout the write branch which currently being edited in CM, this might be another one than currently
        // TODO the unpublished is......find current branch via CmsSessionContext

        try {
            if (!Boolean.TRUE.equals(documentWorkflow.hints().get("checkoutBranch"))) {
                // only master branch
                return documentWorkflow.getBranch(MASTER_BRANCH_ID, DRAFT);
            }
        } catch (RemoteException | RepositoryException e) {
            throw new WorkflowException(e.getMessage());
        }

        final HttpSession httpSession = getPageComposerContextService().getRequestContext().getServletRequest().getSession();
        final CmsSessionContext cmsSessionContext = CmsSessionContext.getContext(httpSession);

        return documentWorkflow.checkoutBranch(getBranchId(cmsSessionContext));
    }

    private String getBranchId(CmsSessionContext cmsSessionContext) {
        return Optional.ofNullable(cmsSessionContext.getContextPayload())
                .map(contextPayload -> contextPayload.get(ContainerConstants.RENDER_BRANCH_ID).toString())
                .orElse(MASTER_BRANCH_ID);
    }

    private Node getContainerItem(final Session session, final String itemUUID) throws RepositoryException {
        final Node containerItem = session.getNodeByIdentifier(itemUUID);

        if (!containerItem.isNodeType(NODETYPE_HST_CONTAINERITEMCOMPONENT)) {
            log.warn("The container component '{}' does not have the correct type. ", itemUUID);
            throw new InvalidNodeTypeException("The container component does not have the correct type.", itemUUID);
        }
        return containerItem;
    }

    private static String findNewName(String base, Node parent) throws RepositoryException {
        String newName = base;
        int counter = 0;
        while (parent.hasNode(newName)) {
            newName = base + ++counter;
        }
        log.debug("New child name '{}' for parent '{}'", newName, parent.getPath());
        return newName;
    }

}
