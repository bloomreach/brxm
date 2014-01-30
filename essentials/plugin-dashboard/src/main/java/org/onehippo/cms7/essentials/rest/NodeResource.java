/*
 * Copyright 2014 Hippo B.V. (http://www.onehippo.com)
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
 */

package org.onehippo.cms7.essentials.rest;

import java.util.Collection;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.servlet.ServletContext;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.onehippo.cms7.essentials.dashboard.utils.GlobalUtils;
import org.onehippo.cms7.essentials.rest.exc.RestException;
import org.onehippo.cms7.essentials.rest.model.KeyValueRestful;
import org.onehippo.cms7.essentials.rest.model.PostPayloadRestful;
import org.onehippo.cms7.essentials.rest.model.RestfulList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;

/**
 * @version "$Id$"
 */
@Produces({MediaType.APPLICATION_JSON})
@Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_FORM_URLENCODED})
@Path("/node/")
public class NodeResource extends BaseResource {

    private static Logger log = LoggerFactory.getLogger(NodeResource.class);

    @POST
    @Path("/property")
    public RestfulList<KeyValueRestful> getProperty(@Context ServletContext servletContext, final PostPayloadRestful payload) {

        final RestfulList<KeyValueRestful> list = new RestfulList<>();
        final Map<String, String> values = payload.getValues();
        final String name = values.get("property");
        final String path = values.get("path");
        if (Strings.isNullOrEmpty(path) || Strings.isNullOrEmpty(name)) {
            throw new RestException("Path or property name were empty", Response.Status.NOT_FOUND);
        }


        Session session = null;
        try {

            session = GlobalUtils.createSession();
            final Node node = session.getNode(path);
            final Property property = node.getProperty(name);
            if (property.isMultiple()) {
                final Value[] v = property.getValues();
                for (Value value : v) {
                    final String val = value.getString();
                    list.add(new KeyValueRestful(val, val));
                }

            }
        } catch (RepositoryException e) {
            log.error("Error fetching property", e);
            throw new RestException(e, Response.Status.NOT_FOUND);
        } finally {
            GlobalUtils.cleanupSession(session);
        }
        return list;

    }

    @POST
    @Path("/property/save")
    public RestfulList<KeyValueRestful> saveProperty(@Context ServletContext servletContext, final PostPayloadRestful payload) {
        final RestfulList<KeyValueRestful> list = new RestfulList<>();
        final Map<String, String> values = payload.getValues();
        final String path = values.remove("path");
        final String name = values.remove("property");
        final String multiple = values.remove("multiple");
        boolean multiValue = Boolean.parseBoolean(multiple);

        Session session = null;
        try {
            if (multiValue) {
                session = GlobalUtils.createSession();
                final Node node = session.getNode(path);
                final Property property = node.getProperty(name);
                final Collection<String> vals = values.values();
                final String[] newValue = vals.toArray(new String[vals.size()]);
                property.setValue(newValue);
                session.save();
            }
        } catch (RepositoryException e) {
            log.error("Error saving property", e);
            throw new RestException(e, Response.Status.NOT_FOUND);
        } finally {
            GlobalUtils.cleanupSession(session);
        }
        return list;

    }


}
