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

package org.onehippo.cms7.essentials.plugins.taxonomy;

import javax.servlet.ServletContext;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import org.onehippo.cms7.essentials.dashboard.rest.BaseResource;
import org.onehippo.cms7.essentials.dashboard.rest.MessageRestful;
import org.onehippo.cms7.essentials.dashboard.rest.PostPayloadRestful;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @version "$Id$"
 */
@Produces({MediaType.APPLICATION_JSON})
@Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_FORM_URLENCODED})
@Path("/taxonomy-plugin/")
public class TaxonomyResource extends BaseResource {

    private static Logger log = LoggerFactory.getLogger(TaxonomyResource.class);


    @POST
    public MessageRestful runBeanWriter(final PostPayloadRestful payloadRestful, @Context ServletContext servletContext) throws Exception {

        log.info("payloadRestful {}", payloadRestful);
        return new MessageRestful();
    }






    /*private Node createOrGetTaxonomyContainer() {
        Node taxonomiesNode = null;
        try {
            if (hasTaxonomyContainer()) {
                taxonomiesNode = session.getNode("/content/taxonomies");
            } else if (session.itemExists("/content")) {
                final Node contentNode = session.getNode("/content");
                taxonomiesNode = contentNode.addNode("taxonomies", "hippotaxonomy:container");
                session.save();
            }
        } catch (RepositoryException e) {
            log.error("repository exception while trying to add or get the taxonomy container directory", e);
        }
        return taxonomiesNode;
    }*/
}
