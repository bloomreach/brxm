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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.servlet.ServletContext;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import org.apache.wicket.util.string.Strings;
import org.hippoecm.repository.HippoStdNodeType;
import org.hippoecm.repository.HippoStdPubWfNodeType;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.StringCodec;
import org.hippoecm.repository.api.StringCodecFactory;
import org.onehippo.cms7.essentials.dashboard.ctx.PluginContext;
import org.onehippo.cms7.essentials.dashboard.rest.BaseResource;
import org.onehippo.cms7.essentials.dashboard.rest.ErrorMessageRestful;
import org.onehippo.cms7.essentials.dashboard.rest.KeyValueRestful;
import org.onehippo.cms7.essentials.dashboard.rest.MessageRestful;
import org.onehippo.cms7.essentials.dashboard.rest.PostPayloadRestful;
import org.onehippo.cms7.essentials.dashboard.utils.GlobalUtils;
import org.onehippo.repository.util.JcrConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;

/**
 * @version "$Id$"
 */
@Produces({MediaType.APPLICATION_JSON})
@Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_FORM_URLENCODED})
@Path("taxonomyplugin")
public class TaxonomyResource extends BaseResource {

    public static final String HIPPOTAXONOMY_TAXONOMY = "hippotaxonomy:taxonomy";
    public static final String HIPPOTAXONOMY_LOCALES = "hippotaxonomy:locales";
    public static final String HIPPOTAXONOMY_MIXIN = "hippotaxonomy:classifiable";
    private static final StringCodec codec = new StringCodecFactory.NameEncoding();


    private static Logger log = LoggerFactory.getLogger(TaxonomyResource.class);

    @POST
    @Path("/")
    public MessageRestful createTaxonomy(final PostPayloadRestful payloadRestful, @Context ServletContext servletContext) {
        final PluginContext context = getContext(servletContext);
        final Session session = context.createSession();
        try {
            final Map<String, String> values = payloadRestful.getValues();
            final String taxonomyName = values.get("taxonomyName");
            final String localeString = values.get("locales");
            final String[] locales;
            if (Strings.isEmpty(localeString)) {
                locales = new String[]{"en"};
            } else {
                final Splitter splitter = Splitter.on(",").omitEmptyStrings().trimResults();
                final Iterable<String> iterable = splitter.split(localeString);
                final List<String> strings = Lists.newArrayList(iterable);
                locales = strings.toArray(new String[strings.size()]);
            }
            if (!Strings.isEmpty(taxonomyName)) {
                final Node taxonomiesNode = createOrGetTaxonomyContainer(session);
                if(taxonomiesNode.hasNode(taxonomyName)){
                    return new ErrorMessageRestful("Taxonomy with name: " + taxonomyName + " already exists");
                }
                addTaxonomyNode(session, taxonomiesNode, taxonomyName, locales);

                return new MessageRestful("Successfully added taxonomy: " + taxonomyName);
            }
        } catch (RepositoryException e) {
            log.error("Error adding taxonomy", e);
        } finally {
            GlobalUtils.cleanupSession(session);
        }
        return new ErrorMessageRestful("Failed to create taxonomy");
    }


    @GET
    @Path("/taxonomies")
    public List<KeyValueRestful> getTaxonomies(@Context ServletContext servletContext) {
        final List<KeyValueRestful> taxonomies = new ArrayList<>();
        final PluginContext context = getContext(servletContext);
        final Session session = context.createSession();

        try {
            final QueryManager queryManager = session.getWorkspace().getQueryManager();
            final Query xpath = queryManager.createQuery("//content//element(*, hippotaxonomy:taxonomy)", "xpath");
            final NodeIterator nodes = xpath.execute().getNodes();
            while (nodes.hasNext()) {
                final Node node = nodes.nextNode();
                final String path = node.getPath();
                taxonomies.add(new KeyValueRestful(node.getName(), path));
            }
        } catch (RepositoryException e) {
            log.error("Error fetching taxonomies", e);

        } finally {
            GlobalUtils.cleanupSession(session);
        }


        return taxonomies;


    }


    private Node createOrGetTaxonomyContainer(final Session session) {
        Node taxonomiesNode = null;
        try {
            if (hasTaxonomyContainer(session)) {
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
    }

    private boolean addTaxonomyNode(final Session session, final Node taxonomiesNode, final String taxonomyName, final String[] locales) throws RepositoryException {
        final Node handleNode = taxonomiesNode.addNode(codec.encode(taxonomyName), HippoNodeType.NT_HANDLE);
        final Node taxonomyNode = handleNode.addNode(codec.encode(taxonomyName), HIPPOTAXONOMY_TAXONOMY);
        taxonomyNode.addMixin(JcrConstants.MIX_VERSIONABLE);
        taxonomyNode.addMixin(JcrConstants.MIX_REFERENCEABLE);
        taxonomyNode.addMixin(HippoStdNodeType.NT_PUBLISHABLESUMMARY);
        taxonomyNode.setProperty(HippoStdPubWfNodeType.HIPPOSTDPUBWF_CREATION_DATE, Calendar.getInstance());
        taxonomyNode.setProperty(HippoStdPubWfNodeType.HIPPOSTDPUBWF_LAST_MODIFIED_DATE, Calendar.getInstance());
        taxonomyNode.setProperty(HippoStdPubWfNodeType.HIPPOSTDPUBWF_PUBLICATION_DATE, Calendar.getInstance());
        taxonomyNode.setProperty(HippoStdPubWfNodeType.HIPPOSTDPUBWF_LAST_MODIFIED_BY, session.getUserID());
        taxonomyNode.setProperty(HippoStdPubWfNodeType.HIPPOSTDPUBWF_CREATED_BY, session.getUserID());
        taxonomyNode.setProperty(HippoStdNodeType.HIPPOSTD_STATE, HippoStdNodeType.PUBLISHED);
        taxonomyNode.setProperty(HippoStdNodeType.HIPPOSTD_HOLDER, session.getUserID());
        taxonomyNode.setProperty(HippoStdNodeType.HIPPOSTD_STATESUMMARY, "live");
        taxonomyNode.setProperty(HIPPOTAXONOMY_LOCALES, locales);
        session.save();
        return true;

    }

    private boolean hasTaxonomyContainer(final Session session) {
        try {
            return session.itemExists("/content/taxonomies") && session.getNode("/content/taxonomies").getPrimaryNodeType().getName().equals("hippotaxonomy:container");
        } catch (RepositoryException e) {
            log.error("Error: {}", e);
        }
        return false;
    }
}
