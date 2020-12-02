/*
 * Copyright 2020 Hippo B.V. (http://www.onehippo.com)
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
 *
 */

package org.hippoecm.hst.pagecomposer.jaxrs.services.experiencepage;

import java.util.Calendar;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.hippoecm.hst.configuration.HstNodeTypes;
import org.hippoecm.hst.configuration.components.HstComponentConfiguration;
import org.hippoecm.hst.container.RequestContextProvider;
import org.hippoecm.hst.pagecomposer.jaxrs.api.annotation.PrivilegesAllowed;
import org.hippoecm.hst.pagecomposer.jaxrs.model.ContainerItemImpl;
import org.hippoecm.hst.pagecomposer.jaxrs.model.ContainerRepresentation;
import org.hippoecm.hst.pagecomposer.jaxrs.model.ErrorStatus;
import org.hippoecm.hst.pagecomposer.jaxrs.services.ComponentResource;
import org.hippoecm.hst.pagecomposer.jaxrs.services.PageComposerContextService;
import org.hippoecm.hst.pagecomposer.jaxrs.services.exceptions.ClientError;
import org.hippoecm.hst.pagecomposer.jaxrs.services.exceptions.ClientException;
import org.hippoecm.hst.pagecomposer.jaxrs.services.util.ContainerUtils;
import org.hippoecm.repository.api.HippoSession;
import org.hippoecm.repository.api.WorkflowException;
import org.onehippo.repository.documentworkflow.DocumentWorkflow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.lang.String.format;
import static org.hippoecm.hst.configuration.HstNodeTypes.NODETYPE_HST_CONTAINERCOMPONENT;
import static org.hippoecm.hst.pagecomposer.jaxrs.services.experiencepage.XPageContainerComponentResource.validateContainerItems;
import static org.hippoecm.hst.pagecomposer.jaxrs.services.experiencepage.XPageUtils.checkoutCorrectBranch;
import static org.hippoecm.hst.pagecomposer.jaxrs.services.experiencepage.XPageUtils.getInternalWorkflowSession;
import static org.hippoecm.hst.pagecomposer.jaxrs.services.experiencepage.XPageUtils.getObtainEditableInstanceWorkflow;
import static org.hippoecm.hst.pagecomposer.jaxrs.services.experiencepage.XPageUtils.getWorkspaceChildrenIds;
import static org.hippoecm.hst.pagecomposer.jaxrs.services.experiencepage.XPageUtils.getWorkspaceNode;
import static org.hippoecm.hst.pagecomposer.jaxrs.services.experiencepage.XPageUtils.updateTimestamp;
import static org.hippoecm.hst.pagecomposer.jaxrs.services.experiencepage.XPageUtils.validateTimestamp;
import static org.hippoecm.hst.pagecomposer.jaxrs.services.util.ContainerUtils.createComponentItem;
import static org.hippoecm.hst.pagecomposer.jaxrs.services.util.ContainerUtils.getCatalogItem;
import static org.hippoecm.hst.platform.services.channel.ChannelManagerPrivileges.XPAGE_REQUIRED_PRIVILEGE_NAME;
import static org.hippoecm.hst.platform.utils.UUIDUtils.isValidUUID;

@Path("/experiencepage/hst:xpage/")
public class XPageResource extends ComponentResource {

    private static Logger log = LoggerFactory.getLogger(XPageResource.class);

    @PUT
    @Path("/")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @PrivilegesAllowed(XPAGE_REQUIRED_PRIVILEGE_NAME)
    public Response updateContainer(final ContainerRepresentation container) {
        // note container.getId() is NOT the UUID of the XPage Layout container but of the hippo:identifier of
        // the container : we need to create a container component with the nodename equal to 'xpageLayoutHippoIdentifier'
        if (!isValidUUID(container.getId())) {
            final String message = format("Value of path parameter catalogItemUUID: '%s' is not a valid UUID", container.getId());
            return clientError(message, new ErrorStatus(ClientError.INVALID_UUID));
        }

        final XPageContainerComponentResource.ContainerAction<Response> updateContainer = () -> {

            final PageComposerContextService pageComposerContextService = getPageComposerContextService();

            final HippoSession userSession = getSession();
            final DocumentWorkflow documentWorkflow = getObtainEditableInstanceWorkflow(userSession, pageComposerContextService);
            final Session workflowSession = getInternalWorkflowSession(documentWorkflow);

            checkoutCorrectBranch(documentWorkflow, pageComposerContextService);

            final Node xPageNode = getWorkspaceNode(workflowSession, getPageComposerContextService().getRequestConfigIdentifier());

            final Node containerNode = xPageNode.addNode(container.getId(), NODETYPE_HST_CONTAINERCOMPONENT);

            final List<String> children = getWorkspaceChildrenIds(workflowSession, container.getChildren());

            validateContainerItems(getPageComposerContextService(), workflowSession, children);

            ContainerUtils.updateContainerOrder(workflowSession, children, containerNode, node -> {
                // no locking needed for XPages;
            });

            // update last modified for optimistic locking
            final Calendar calendar = updateTimestamp(containerNode);
            container.setLastModifiedTimestamp(calendar.getTimeInMillis());

            documentWorkflow.saveUnpublished();

            // since a new container is created, the page MUST be reloaded
            final boolean requiresReload = true;
            return ok(xPageNode.getIdentifier() + " updated", container, requiresReload);
        };

        return handleAction(updateContainer);
    }


    @POST
    @Path("/{xpageLayoutHippoIdentifier}/{catalogItemUUID}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @PrivilegesAllowed(XPAGE_REQUIRED_PRIVILEGE_NAME)
    public Response createContainerAndContainerItem(final @PathParam("xpageLayoutHippoIdentifier") String xpageLayoutHippoIdentifier,
                                        final @PathParam("catalogItemUUID") String catalogItemUUID,
                                        final @QueryParam("lastModifiedTimestamp") long versionStamp) {
        // note xpageLayoutHippoIdentifier is NOT the UUID of the XPage Layout container but of the hippo:identifier of
        // the container : we need to create a container component with the nodename equal to 'xpageLayoutHippoIdentifier'
        if (!isValidUUID(xpageLayoutHippoIdentifier)) {
            final String message = format("Value of path parameter catalogItemUUID: '%s' is not a valid UUID", xpageLayoutHippoIdentifier);
            return clientError(message, new ErrorStatus(ClientError.INVALID_UUID));
        }
        if (!isValidUUID(catalogItemUUID)) {
            final String message = format("Value of path parameter catalogItemUUID: '%s' is not a valid UUID", catalogItemUUID);
            return clientError(message, new ErrorStatus(ClientError.INVALID_UUID));
        }


        final XPageContainerComponentResource.ContainerAction<Response> createContainerItem = () -> {

            final PageComposerContextService pageComposerContextService = getPageComposerContextService();

            final HippoSession userSession = getSession();
            final DocumentWorkflow documentWorkflow = getObtainEditableInstanceWorkflow(userSession, pageComposerContextService);

            checkoutCorrectBranch(documentWorkflow, pageComposerContextService);

            final Session workflowSession = getInternalWorkflowSession(documentWorkflow);

            // with workflowSession, we write directly to the unpublished variant : the container from the request
            // is of the 'preview' variant (mind you, this can be the preview of a branch loaded from version history!)
            final Node catalogItem = ContainerUtils.getContainerItem(workflowSession, catalogItemUUID);

            final Node xPageNode = getWorkspaceNode(workflowSession, getPageComposerContextService().getRequestConfigIdentifier());

            final Node containerNode = xPageNode.addNode(xpageLayoutHippoIdentifier, NODETYPE_HST_CONTAINERCOMPONENT);

            final HstComponentConfiguration componentDefinition = getCatalogItem(pageComposerContextService, catalogItem);

            final Node newItem = createComponentItem(containerNode, catalogItem, componentDefinition);

            final Calendar updatedTimestamp = updateTimestamp(containerNode);

            documentWorkflow.saveUnpublished();

            // since a new container is created, the page MUST be reloaded
            final boolean requiresReload = true;
            return respondContainerItem(new ContainerItemImpl(newItem, componentDefinition, updatedTimestamp.getTimeInMillis()), requiresReload,
                    Response.Status.CREATED, "Successfully created item");

        };

        return handleAction(createContainerItem);
    }

    private HippoSession getSession() throws RepositoryException {
        return (HippoSession) RequestContextProvider.get().getSession();
    }

    private Response handleAction(XPageContainerComponentResource.ContainerAction<Response> action) {
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
