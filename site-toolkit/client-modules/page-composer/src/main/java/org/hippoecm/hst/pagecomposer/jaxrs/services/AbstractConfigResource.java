/*
 *  Copyright 2010-2020 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.pagecomposer.jaxrs.services;

import java.util.Map;
import java.util.concurrent.Callable;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.ws.rs.core.Response;

import org.apache.commons.lang3.ArrayUtils;
import org.hippoecm.hst.configuration.HstNodeTypes;
import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.configuration.internal.CanonicalInfo;
import org.hippoecm.hst.content.beans.manager.ObjectConverter;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.pagecomposer.jaxrs.api.BaseChannelEvent;
import org.hippoecm.hst.pagecomposer.jaxrs.model.ContainerItem;
import org.hippoecm.hst.pagecomposer.jaxrs.model.ContainerItemRepresentation;
import org.hippoecm.hst.pagecomposer.jaxrs.model.ResponseRepresentation;
import org.hippoecm.hst.pagecomposer.jaxrs.services.exceptions.ClientException;
import org.hippoecm.hst.pagecomposer.jaxrs.services.validators.Validator;
import org.hippoecm.hst.pagecomposer.jaxrs.util.HstConfigurationUtils;
import org.hippoecm.hst.platform.api.ChannelEventBus;
import org.hippoecm.hst.platform.api.PlatformServices;
import org.hippoecm.repository.api.HippoSession;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AbstractConfigResource {

    private static Logger log = LoggerFactory.getLogger(AbstractConfigResource.class);

    private PageComposerContextService pageComposerContextService;

    public void setPageComposerContextService(PageComposerContextService pageComposerContextService) {
        this.pageComposerContextService = pageComposerContextService;
    }

    public PageComposerContextService getPageComposerContextService() {
        return pageComposerContextService;
    }

    public PlatformServices getPlatformServices() {
        return HippoServiceRegistry.getService(PlatformServices.class);
    }

    protected Response ok(String msg) {
        return ok(msg, ArrayUtils.EMPTY_STRING_ARRAY, false);
    }

    protected Response ok(String msg, boolean reloadRequired) {
        return ok(msg, ArrayUtils.EMPTY_STRING_ARRAY, reloadRequired);
    }

    protected Response ok(String msg, Object data) {
        return ok(msg, data, false);
    }

    protected Response ok(String msg, Object data, boolean reloadRequired) {
        ResponseRepresentation entity = new ResponseRepresentation(data);
        entity.setMessage(msg);
        entity.setSuccess(true);
        entity.setReloadRequired(reloadRequired);
        return Response.ok().entity(entity).build();
    }

    protected Response error(String msg) {
        return error(msg, ArrayUtils.EMPTY_STRING_ARRAY);
    }

    protected Response error(String msg, boolean reloadRequired) {
        return error(msg, ArrayUtils.EMPTY_STRING_ARRAY, reloadRequired);
    }

    protected Response error(String msg, Object data) {
        return error(msg, data, false);
    }

    protected Response error(String msg, Object data, boolean reloadRequired) {
        ResponseRepresentation entity = new ResponseRepresentation(data);
        entity.setMessage(msg);
        entity.setSuccess(false);
        entity.setReloadRequired(reloadRequired);
        return Response.serverError().entity(entity).build();
    }

    protected Response clientError(String msg, Object data) {
        return clientError(msg, data, false);
    }


    protected Response clientError(String msg, Object data, boolean reloadRequired) {
        ResponseRepresentation entity = new ResponseRepresentation(data);
        entity.setMessage(msg);
        entity.setSuccess(false);
        entity.setReloadRequired(reloadRequired);
        return Response.status(Response.Status.BAD_REQUEST).entity(entity).build();
    }

    protected Response created(String msg) {
        return created(msg, false);
    }

    protected Response created(String msg, boolean reloadRequired) {
        ResponseRepresentation entity = new ResponseRepresentation();
        entity.setMessage(msg);
        entity.setSuccess(true);
        entity.setReloadRequired(reloadRequired);
        return Response.status(Response.Status.CREATED).entity(entity).build();
    }

    protected Response conflict(String msg) {
        return conflict(msg, false);
    }

    protected Response conflict(String msg, boolean reloadRequired) {
        ResponseRepresentation entity = new ResponseRepresentation();
        entity.setMessage(msg);
        entity.setSuccess(false);
        entity.setReloadRequired(reloadRequired);
        return Response.status(Response.Status.CONFLICT).entity(entity).build();
    }

    protected ObjectConverter getObjectConverter(HstRequestContext requestContext) {
        return requestContext.getContentBeansTool().getObjectConverter();
    }


    protected Response tryGet(final Callable<Response> callable) {
        try {
            return callable.call();
        } catch (ClientException e) {
            resetSession();
            return logAndReturnClientError(e);
        } catch (Exception e) {
            resetSession();
            return logAndReturnServerError(e);
        }
    }

    protected Response tryExecute(final Callable<Response> callable,
                                  final Validator preValidator) {
        try {

            createMandatoryWorkspaceNodesIfMissing();

            final HstRequestContext requestContext = getPageComposerContextService().getRequestContext();
            preValidator.validate(requestContext);

            final Response response = callable.call();

            final Session session = requestContext.getSession();
            if (session.hasPendingChanges()) {
                HstConfigurationUtils.persistChanges(session);
            }
            return response;
        } catch (ClientException e) {
            resetSession();
            return logAndReturnClientError(e);
        } catch (Exception e) {
            resetSession();
            return logAndReturnServerError(e);
        }
    }

    protected void publishSynchronousEvent(final BaseChannelEvent event) throws ClientException {
        final ChannelEventBus cmEventBus = HippoServiceRegistry.getService(ChannelEventBus.class);
        cmEventBus.post(event, event.getChannel().getContextPath());

        if (event.getException() != null) {
            throw event.getException();
        }
    }


    private void createMandatoryWorkspaceNodesIfMissing() throws RepositoryException {
        final String liveConfigPath = getPageComposerContextService().getEditingLiveConfigurationPath();
        final String previewConfigPath = getPageComposerContextService().getEditingPreviewConfigurationPath();
        HstConfigurationUtils.createMandatoryWorkspaceNodesIfMissing(liveConfigPath, getPageComposerContextService().getRequestContext().getSession());
        HstConfigurationUtils.createMandatoryWorkspaceNodesIfMissing(previewConfigPath, getPageComposerContextService().getRequestContext().getSession());
    }

    protected void resetSession() {
        final HstRequestContext requestContext = getPageComposerContextService().getRequestContext();
        if (requestContext != null) {
            try {
                final Session session = requestContext.getSession();
                if (session.hasPendingChanges()) {
                    if (session instanceof HippoSession) {
                        ((HippoSession) session).localRefresh();
                    } else {
                        session.refresh(false);
                    }
                }
            } catch (RepositoryException e) {
                log.error("RepositoryException while resetting session", e);
            }
        }
    }

    protected Response logAndReturnServerError(Exception e) {
        if (log.isDebugEnabled()) {
            log.warn(e.toString(), e);
        } else {
            log.warn(e.toString(), e);
        }
        return error(e.getMessage());
    }

    protected Response logAndReturnClientError(ClientException e) {
        final String formattedMessage = e.getMessage();
        if (log.isDebugEnabled()) {
            log.info(formattedMessage, e);
        } else {
            log.info(formattedMessage);
        }
        final ResponseRepresentation entity = new ResponseRepresentation();
        entity.setSuccess(false);
        entity.setMessage(e.toString());
        entity.setErrorCode(e.getError().name());
        entity.setData(e.getParameterMap());
        return Response.status(Response.Status.BAD_REQUEST).entity(entity).build();
    }

    protected CanonicalInfo getCanonicalInfo(final Object o) throws IllegalStateException {
        if (o instanceof CanonicalInfo) {
            return (CanonicalInfo) o;
        }
        throw new IllegalStateException("HstSiteMenuItemConfiguration not instanceof CanonicalInfo");
    }

    protected String getPreviewConfigurationPath() {
        return getPageComposerContextService().getEditingPreviewConfigurationPath();
    }

    protected String getPreviewConfigurationWorkspacePath() {
        return getPreviewConfigurationPath() + "/" + HstNodeTypes.NODENAME_HST_WORKSPACE;
    }

    /**
     * @param mountId
     * @return the {@link Mount} for {@code mountId} which can be a mount of a different webapp compared to the currently
     * being edited mount. If no mount for {@code mountId} found, return null
     */
    protected Mount getMount(final String mountId) {
        final Map<String, Mount> previewMounts = getPlatformServices().getMountService()
                .getPreviewMounts(pageComposerContextService.getRequestContext().getVirtualHost().getHostGroupName());

        return previewMounts.get(mountId);
    }

    protected String getPreviewConfigurationWorkspacePath(final String mountId) {
        final Mount mount = getMount(mountId);
        if (mount == null || !mount.getHstSite().hasPreviewConfiguration()) {
            final String msg = String.format("Cannot find for id '%s' or the mount exists but does not have a preview configuration.", mountId);
            throw new IllegalArgumentException(msg);
        }
        final String configurationPath = mount.getHstSite().getConfigurationPath();
        return configurationPath + "/" + HstNodeTypes.NODENAME_HST_WORKSPACE;
    }

    public Response respondContainerItem(final ContainerItem containerItem,
                                         final boolean requiresReload,
                                         final Response.StatusType statusType,
                                         final String msg) throws RepositoryException {
        final Node containerItemNode = containerItem.getContainerItem();

        log.info("Returning success response for container item '{}':  StatusType : {},  ReloadRequired : {}," +
                "Message = {}", containerItemNode.getPath(), statusType, requiresReload, msg);

        final ContainerItemRepresentation containerItemRepresentation = new ContainerItemRepresentation()
                .represent(containerItemNode, containerItem.getComponentDefinition(), containerItem.getTimeStamp());

        final ResponseRepresentation entity = new ResponseRepresentation(containerItemRepresentation);
        entity.setReloadRequired(requiresReload);
        entity.setSuccess(true);
        entity.setMessage(msg);
        return Response.status(statusType)
                .entity(entity)
                .build();
    }
}
