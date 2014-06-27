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

package org.hippoecm.hst.demo.jaxrs.services;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.jcr.ItemNotFoundException;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.hippoecm.hst.demo.jaxrs.services.util.ResponseUtils;
import org.hippoecm.hst.jaxrs.services.AbstractResource;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.cms7.services.contenttype.ContentType;
import org.onehippo.cms7.services.contenttype.ContentTypeItem;
import org.onehippo.cms7.services.contenttype.ContentTypeService;
import org.onehippo.cms7.services.contenttype.EffectiveNodeType;
import org.onehippo.cms7.services.contenttype.EffectiveNodeTypeChild;
import org.onehippo.cms7.services.contenttype.EffectiveNodeTypeItem;
import org.onehippo.cms7.services.contenttype.EffectiveNodeTypeProperty;

@Path("/contenttypes/")
@Produces({MediaType.APPLICATION_JSON})
public class ContentTypesResource extends AbstractResource {

    @GET
    @Path("/ent")
    public Set<String> listENTPrefixes() {

        ContentTypeService service = HippoServiceRegistry.getService(ContentTypeService.class);
        try {
            return service.getEffectiveNodeTypes().getTypesByPrefix().keySet();
        } catch (RepositoryException e) {
            throw new WebApplicationException(e, ResponseUtils.buildServerErrorResponse(e));
        }
    }

    @GET
    @Path("/ent/{prefix}")
    public Set<EffectiveNodeType> listENTypes(@PathParam("prefix") String prefix) {

        ContentTypeService service = HippoServiceRegistry.getService(ContentTypeService.class);
        try {
            Set<EffectiveNodeType> result = service.getEffectiveNodeTypes().getTypesByPrefix().get(prefix);
            if (result != null) {
                return result;
            }
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        } catch (RepositoryException e) {
            throw new WebApplicationException(e, ResponseUtils.buildServerErrorResponse(e));
        }
    }

    @GET
    @Path("/ent/{prefix}:{name}")
    public EffectiveNodeType listENType(@PathParam("prefix") String prefix, @PathParam("name") String name) {

        ContentTypeService service = HippoServiceRegistry.getService(ContentTypeService.class);
        try {
            EffectiveNodeType ent = service.getEffectiveNodeTypes().getType(prefix + ":" + name);
            if (ent != null) {
                return ent;
            }
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        } catch (RepositoryException e) {
            throw new WebApplicationException(e, ResponseUtils.buildServerErrorResponse(e));
        }
    }

    @GET
    @Path("/ent/{type}/{item}")
    public List<EffectiveNodeTypeItem> listENTItem(@PathParam("type") String type, @PathParam("item") String item) {

        ContentTypeService service = HippoServiceRegistry.getService(ContentTypeService.class);
        try {
            List<EffectiveNodeTypeItem> result = null;
            EffectiveNodeType ent = service.getEffectiveNodeTypes().getType(type);
            if (ent != null) {
                List<EffectiveNodeTypeProperty> prop = ent.getProperties().get(item);
                if (prop != null) {
                    result = new ArrayList<EffectiveNodeTypeItem>(prop);
                }
                List<EffectiveNodeTypeChild> child = ent.getChildren().get(item);
                if (child != null) {
                    if (result == null) {
                        result = new ArrayList<EffectiveNodeTypeItem>(child);
                    }
                    else {
                        result.addAll(child);
                    }
                }
            }
            if (result != null) {
                return result;
            }
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        } catch (RepositoryException e) {
            throw new WebApplicationException(e, ResponseUtils.buildServerErrorResponse(e));
        }
    }

    @GET
    @Path("/ent/{type}/p/{prop}")
    public List<EffectiveNodeTypeProperty> listENTProperty(@PathParam("type") String type, @PathParam("prop") String prop) {

        ContentTypeService service = HippoServiceRegistry.getService(ContentTypeService.class);
        try {
            List<EffectiveNodeTypeProperty> result = null;
            EffectiveNodeType ent = service.getEffectiveNodeTypes().getType(type);
            if (ent != null) {
                result = ent.getProperties().get(prop);
            }
            if (result != null) {
                return result;
            }
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        } catch (RepositoryException e) {
            throw new WebApplicationException(e, ResponseUtils.buildServerErrorResponse(e));
        }
    }

    @GET
    @Path("/ent/{type}/c/{child}")
    public List<EffectiveNodeTypeChild> listENTChild(@PathParam("type") String type, @PathParam("child") String child) {

        ContentTypeService service = HippoServiceRegistry.getService(ContentTypeService.class);
        try {

            List<EffectiveNodeTypeChild> result = null;
            EffectiveNodeType ent = service.getEffectiveNodeTypes().getType(type);
            if (ent != null) {
                result = ent.getChildren().get(child);
            }
            if (result != null) {
                return result;
            }
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        } catch (RepositoryException e) {
            throw new WebApplicationException(e, ResponseUtils.buildServerErrorResponse(e));
        }
    }

    @GET
    @Path("/ct")
    public Set<String> listDocumentTypePrefixes() {

        ContentTypeService service = HippoServiceRegistry.getService(ContentTypeService.class);
        try {
            return service.getContentTypes().getTypesByPrefix().keySet();
        } catch (RepositoryException e) {
            throw new WebApplicationException(e, ResponseUtils.buildServerErrorResponse(e));
        }
    }

    @GET
    @Path("/ct/{prefix}")
    public Set<ContentType> listDocumentTypes(@PathParam("prefix") String prefix) {

        ContentTypeService service = HippoServiceRegistry.getService(ContentTypeService.class);
        try {
            Set<ContentType> result = service.getContentTypes().getTypesByPrefix().get(prefix);
            if (result != null) {
                return result;
            }
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        } catch (RepositoryException e) {
            throw new WebApplicationException(e, ResponseUtils.buildServerErrorResponse(e));
        }
    }

    @GET
    @Path("/ct/{prefix}:{name}")
    public ContentType listDocumentType(@PathParam("prefix") String prefix, @PathParam("name") String name) {

        ContentTypeService service = HippoServiceRegistry.getService(ContentTypeService.class);
        try {
            ContentType ct = service.getContentTypes().getType(prefix + ":" + name);
            if (ct != null) {
                return ct;
            }
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        } catch (RepositoryException e) {
            throw new WebApplicationException(e, ResponseUtils.buildServerErrorResponse(e));
        }
    }

    @GET
    @Path("/ct/{type}/{item}")
    public ContentTypeItem listDocumentTypeItem(@PathParam("type") String type, @PathParam("item") String item) {

        ContentTypeService service = HippoServiceRegistry.getService(ContentTypeService.class);
        try {
            ContentTypeItem result = null;
            ContentType ct = service.getContentTypes().getType(type);
            if (ct != null) {
                result = ct.getItem(item);
            }
            if (result != null) {
                return result;
            }
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        } catch (RepositoryException e) {
            throw new WebApplicationException(e, ResponseUtils.buildServerErrorResponse(e));
        }
    }

    @GET
    @Path("/ct/{type}/p/{prop}")
    public ContentTypeItem listDocumentTypeProperty(@PathParam("type") String type, @PathParam("prop") String prop) {

        ContentTypeService service = HippoServiceRegistry.getService(ContentTypeService.class);
        try {
            ContentTypeItem result = null;
            ContentType ct = service.getContentTypes().getType(type);
            if (ct != null) {
                result = ct.getProperties().get(prop);
            }
            if (result != null) {
                return result;
            }
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        } catch (RepositoryException e) {
            throw new WebApplicationException(e, ResponseUtils.buildServerErrorResponse(e));
        }
    }

    @GET
    @Path("/ct/{type}/c/{child}")
    public ContentTypeItem listDocumentTypeChild(@PathParam("type") String type, @PathParam("child") String child) {

        ContentTypeService service = HippoServiceRegistry.getService(ContentTypeService.class);
        try {
            ContentTypeItem result = null;
            ContentType ct = service.getContentTypes().getType(type);
            if (ct != null) {
                result = ct.getChildren().get(child);
            }
            if (result != null) {
                return result;
            }
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        } catch (RepositoryException e) {
            throw new WebApplicationException(e, ResponseUtils.buildServerErrorResponse(e));
        }
    }

    @GET
    @Path("/uuid/{uuid}")
    public ContentType listDocumentTypeForUuid(@Context HttpServletRequest servletRequest, @PathParam("uuid") String uuid) {

        ContentTypeService service = HippoServiceRegistry.getService(ContentTypeService.class);
        try {
            return service.getContentTypes().getContentTypeForNodeByUuid(getRequestContext(servletRequest).getSession(), uuid);
        } catch (ItemNotFoundException e) {
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        } catch (RepositoryException e) {
            throw new WebApplicationException(e, ResponseUtils.buildServerErrorResponse(e));
        }
    }

    @GET
    @Path("/path/{path:.+}")
    public ContentType listDocumentTypeForPath(@Context HttpServletRequest servletRequest, @PathParam("path") String path) {

        ContentTypeService service = HippoServiceRegistry.getService(ContentTypeService.class);
        try {
            return service.getContentTypes().getContentTypeForNodeByPath(getRequestContext(servletRequest).getSession(), "/" + path);
        } catch (PathNotFoundException e) {
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        } catch (RepositoryException e) {
            throw new WebApplicationException(e, ResponseUtils.buildServerErrorResponse(e));
        }
    }
}
