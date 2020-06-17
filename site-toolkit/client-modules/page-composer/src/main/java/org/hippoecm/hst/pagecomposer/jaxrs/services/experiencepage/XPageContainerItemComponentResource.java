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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.Response;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.hippoecm.hst.pagecomposer.jaxrs.api.annotation.PrivilegesAllowed;
import org.hippoecm.hst.pagecomposer.jaxrs.model.ContainerItemComponentRepresentation;
import org.hippoecm.hst.pagecomposer.jaxrs.model.ContainerItemImpl;
import org.hippoecm.hst.pagecomposer.jaxrs.model.ErrorStatus;
import org.hippoecm.hst.pagecomposer.jaxrs.services.AbstractConfigResource;
import org.hippoecm.hst.pagecomposer.jaxrs.services.ContainerItemComponentResourceInterface;
import org.hippoecm.hst.pagecomposer.jaxrs.services.ContainerItemComponentService;
import org.hippoecm.hst.pagecomposer.jaxrs.services.exceptions.ClientException;
import org.hippoecm.hst.pagecomposer.jaxrs.services.exceptions.ServerErrorException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.lang.String.format;
import static org.hippoecm.hst.platform.services.channel.ChannelManagerPrivileges.XPAGE_REQUIRED_PRIVILEGE_NAME;

/**
 * The REST resource handler for the nodes that are of the type "hst:containeritemcomponent". This is specified using
 * the @Path annotation.
 * <p>
 * The resources handler operates on variants and their parameters.
 */
@Path("/experiencepage/hst:containeritemcomponent/")
public class XPageContainerItemComponentResource extends AbstractConfigResource implements ContainerItemComponentResourceInterface {
    private static Logger log = LoggerFactory.getLogger(XPageContainerItemComponentResource.class);

    private ContainerItemComponentService xPageContainerItemComponentService;

    public void setXpageContainerItemComponentService(final ContainerItemComponentService containerItemComponentService) {
        this.xPageContainerItemComponentService = containerItemComponentService;
    }

    @GET
    @Path("/")
    @Produces(MediaType.APPLICATION_JSON)
    @PrivilegesAllowed(XPAGE_REQUIRED_PRIVILEGE_NAME)
    @Override
    public Response getVariants() {

        try {
            Set<String> variants = this.xPageContainerItemComponentService.getVariants();
            log.info("Available variants: {}", variants);
            return ok("Available variants: ", variants);
        } catch (ClientException e){
            log.warn("Unable to get the parameters of the component", e);
            return clientError("Unable to get the parameters of the component", e.getErrorStatus());
        } catch (RepositoryException e) {
            log.error("Unable to get the parameters of the component", e);
            return error("Unable to get the parameters of the component", ErrorStatus.unknown(e.getMessage()));
        }
    }

    @POST
    @Path("/")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @PrivilegesAllowed(XPAGE_REQUIRED_PRIVILEGE_NAME)
    @Override
    public Response retainVariants(final String[] variants,
                                   final @HeaderParam("lastModifiedTimestamp") long versionStamp) {
        try {

            final HashSet<String> retainedVariants = new HashSet<>(Arrays.asList(variants));
            final Pair<Set<String>, Boolean> result = this.xPageContainerItemComponentService.retainVariants(retainedVariants, versionStamp);

            return ok("Removed variants:", result.getLeft(), result.getRight());
        } catch (ClientException e) {
            log.error("Unable to cleanup the variants of the component", e);
            return clientError(e.getMessage(), e.getErrorStatus());
        } catch (RepositoryException e) {
            log.error("Unable to cleanup the variants of the component", e);
            return error("Unable to cleanup the variants of the component", ErrorStatus.unknown(e.getMessage()));
        } catch (IllegalStateException e) {
            log.error("Unable to cleanup the variants of the component", e);
            throw new WebApplicationException(e);
        }
    }

    @GET
    @Path("/{variant}/{locale}")
    @Produces(MediaType.APPLICATION_JSON)
    @PrivilegesAllowed(XPAGE_REQUIRED_PRIVILEGE_NAME)
    @Override
    public ContainerItemComponentRepresentation getVariant(final @PathParam("variant") String variant,
                               final @PathParam("locale") String localeString) {
        try {
            return this.xPageContainerItemComponentService.getVariant(variant, localeString);
        } catch (Exception e) {
            log.warn("Failed to retrieve parameters.", e);
        }
        return null;
    }

    @PUT
    @Path("/{variantId}")
    @Produces(MediaType.APPLICATION_JSON)
    @PrivilegesAllowed(XPAGE_REQUIRED_PRIVILEGE_NAME)
    @Override
    public Response moveAndUpdateVariant(final @PathParam("variantId") String variantId,
                                         final @HeaderParam("Move-To") String  newVariantId,
                                         final @HeaderParam("lastModifiedTimestamp") long versionStamp,
                                         final MultivaluedHashMap<String, String> params) {
        try {
            if (StringUtils.isEmpty(newVariantId)) {
                final Pair<Node, Boolean> result = this.xPageContainerItemComponentService.updateVariant(variantId, versionStamp, params);
                return respondContainerItem(new ContainerItemImpl(result.getLeft(), 0L), result.getRight(),
                        Response.Status.OK, format("Parameters for '%s' saved successfully.",  variantId));
            } else {
                final Pair<Node, Boolean> result = this.xPageContainerItemComponentService.moveAndUpdateVariant(variantId, newVariantId, versionStamp, params);
                return respondContainerItem(new ContainerItemImpl(result.getLeft(), 0L), result.getRight(),
                        Response.Status.OK, format("Parameters renamed from '%s' to '%s' and saved successfully.", variantId, newVariantId));
            }
        } catch (ClientException e) {
            return clientError("Unable to set the parameters of component", e.getErrorStatus());
        } catch (RepositoryException e) {
            return error("Unable to set the parameters of component", ErrorStatus.unknown(e.getMessage()));
        }
    }


    @POST
    @Path("/{variantId}")
    @Produces(MediaType.APPLICATION_JSON)
    @PrivilegesAllowed(XPAGE_REQUIRED_PRIVILEGE_NAME)
    @Override
    public Response createVariant(final @PathParam("variantId") String variantId,
                                  final @HeaderParam("lastModifiedTimestamp") long versionStamp) {

        try {
            final Pair<Node, Boolean> result = this.xPageContainerItemComponentService.createVariant(variantId, versionStamp);
            return respondContainerItem(new ContainerItemImpl(result.getLeft(), 0L), result.getRight(),
                    Response.Status.CREATED, format("Variant '%s' created successfully", variantId));
        } catch (ClientException e) {
            return clientError("Could not create variant '" + variantId + "'", e.getErrorStatus());
        } catch (RepositoryException | ServerErrorException e) {
            log.error("Could not create variant '{}'", variantId, e);
            return error("Could not create variant '" + variantId + "'", ErrorStatus.unknown(e.getMessage()));
        }
    }

    @DELETE
    @Path("/{variantId}")
    @Produces(MediaType.APPLICATION_JSON)
    @PrivilegesAllowed(XPAGE_REQUIRED_PRIVILEGE_NAME)
    @Override
    public Response deleteVariant(final @PathParam("variantId") String variantId,
                                  final @HeaderParam("lastModifiedTimestamp") long versionStamp) {
        try {
            final Pair<Node, Boolean> result = this.xPageContainerItemComponentService.deleteVariant(variantId, versionStamp);
            return respondContainerItem(new ContainerItemImpl(result.getLeft(), 0L), result.getRight(),
                    Response.Status.OK, format("Variant '%s' deleted successfully", variantId));
        } catch (ClientException e) {
            log.warn("Could not delete variant '{}' : {}", variantId, e.getMessage());
            return clientError("Could not delete variant '" + variantId + "'", e.getErrorStatus());
        } catch (RepositoryException e) {
            log.error("Could not delete variant '{}': {}", variantId, e.getMessage());
            final ErrorStatus errorStatus = ErrorStatus.unknown(e.getMessage());
            return error("Could not delete variant '" + variantId + "'", errorStatus);
        }
    }
}
