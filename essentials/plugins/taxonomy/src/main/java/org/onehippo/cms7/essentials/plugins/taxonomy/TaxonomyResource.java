/*
 * Copyright 2014-2018 Hippo B.V. (http://www.onehippo.com)
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
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import org.apache.commons.lang.StringUtils;
import org.hippoecm.repository.HippoStdNodeType;
import org.hippoecm.repository.HippoStdPubWfNodeType;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.StringCodec;
import org.hippoecm.repository.api.StringCodecFactory;
import org.onehippo.repository.util.JcrConstants;
import org.onehippo.cms7.essentials.sdk.api.rest.UserFeedback;
import org.onehippo.cms7.essentials.sdk.api.service.ContentTypeService;
import org.onehippo.cms7.essentials.sdk.api.service.JcrService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @version "$Id$"
 */
@Produces({MediaType.APPLICATION_JSON})
@Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_FORM_URLENCODED})
@Path("taxonomyplugin")
public class TaxonomyResource {

    private static final String HIPPOTAXONOMY_TAXONOMY = "hippotaxonomy:taxonomy";
    private static final String HIPPOTAXONOMY_LOCALES = "hippotaxonomy:locales";
    private static final String HIPPOTAXONOMY_MIXIN = "hippotaxonomy:classifiable";
    private static final String TAXONOMY_FIELD_MARKER = "essentials-taxonomy-name";
    private static final StringCodec codec = new StringCodecFactory.NameEncoding();
    private static final Logger log = LoggerFactory.getLogger(TaxonomyResource.class);

    @Inject private JcrService jcrService;
    @Inject private ContentTypeService contentTypeService;

    /**
     * Returns list of taxonomies found under {@code /content/taxonomies/} node.
     *
     * @return list of taxonomies (name and node path pairs)
     */
    @GET
    @Path("/taxonomies")
    public List<TaxonomyRestful> getTaxonomies() {
        final List<TaxonomyRestful> taxonomies = new ArrayList<>();
        final Session session = jcrService.createSession();

        try {
            final QueryManager queryManager = session.getWorkspace().getQueryManager();
            final Query xpath = queryManager.createQuery("//content//element(*, hippotaxonomy:taxonomy)[@hippostd:state='published']", "xpath");
            final NodeIterator nodes = xpath.execute().getNodes();
            while (nodes.hasNext()) {
                final Node node = nodes.nextNode();
                final TaxonomyRestful taxonomy  = new TaxonomyRestful();
                taxonomy.setName(node.getName());
                taxonomy.setPath(node.getPath());
                final Value[] localeValues = node.getProperty(HIPPOTAXONOMY_LOCALES).getValues();
                final String[] locales = new String[localeValues.length];
                for (int i = 0; i < locales.length; i++) {
                    locales[i] = localeValues[i].getString();
                }
                taxonomy.setLocales(locales);
                taxonomies.add(taxonomy);
            }
        } catch (RepositoryException e) {
            log.error("Error fetching taxonomies", e);
        } finally {
            jcrService.destroySession(session);
        }

        return taxonomies;
    }

    @GET
    @Path("taxonomies/{jcr-content-type}")
    public List<String> getTaxonomyFields(@PathParam("jcr-content-type") final String jcrContentType) {
        List<String> fields = new ArrayList<>();

        final Session session = jcrService.createSession();
        try {
            final String editorTemplatePath = contentTypeService.jcrBasePathForContentType(jcrContentType);
            if (session.nodeExists(editorTemplatePath)) {
                final Node editorTemplateNode = session.getNode(editorTemplatePath);
                final NodeIterator it = editorTemplateNode.getNodes();
                while (it.hasNext()) {
                    final Node field = it.nextNode();
                    if (field.hasProperty(TAXONOMY_FIELD_MARKER)) {
                        fields.add(field.getProperty(TAXONOMY_FIELD_MARKER).getString());
                    }
                }
            }
        } catch (RepositoryException ex) {
            log.error("Problem checking taxonomy fields for document " + jcrContentType, ex);
        } finally {
            jcrService.destroySession(session);
        }

        return fields;
    }

    /**
     * Adds taxonomy to {@code /content/taxonomies/} node.
     *
     * @param taxonomyRestful taxonomy data
     * @param response servlet response
     * @return message or an error message in case of error
     */
    @POST
    @Path("/taxonomies/add")
    public UserFeedback createTaxonomy(final TaxonomyRestful taxonomyRestful, @Context HttpServletResponse response) {
        final Session session = jcrService.createSession();
        try {
            final String taxonomyName = taxonomyRestful.getName();
            String[] locales = taxonomyRestful.getLocales();
            if (locales == null || locales.length == 0) {
                locales = new String[]{"en"};
            }
            if (!StringUtils.isBlank(taxonomyName)) {
                final Node taxonomiesNode = createOrGetTaxonomyContainer(session);
                if (taxonomiesNode.hasNode(taxonomyName)) {
                    response.setStatus(HttpServletResponse.SC_PRECONDITION_FAILED);
                    return new UserFeedback().addError("Taxonomy with name: " + taxonomyName + " already exists");
                }
                addTaxonomyNode(session, taxonomiesNode, taxonomyName, locales);

                return new UserFeedback().addSuccess("Successfully added taxonomy: " + taxonomyName);
            }
        } catch (RepositoryException e) {
            log.error("Error adding taxonomy", e);
        } finally {
            jcrService.destroySession(session);
        }
        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        return new UserFeedback().addError("Failed to create taxonomy");
    }

    @POST
    @Path("/add")
    public UserFeedback addTaxonomyToDocument(final List<TaxonomyField> taxonomyFields, @Context HttpServletResponse response) {
        final Session session = jcrService.createSession();
        final Collection<String> changedDocuments = new HashSet<>();
        final UserFeedback feedback = new UserFeedback();
        try {
            for (TaxonomyField field : taxonomyFields) {
                final String jcrContentType = field.getJcrContentType();
                final String editorTemplatePath = contentTypeService.jcrBasePathForContentType(jcrContentType) + "/editor:templates/_default_";
                if (!session.nodeExists(editorTemplatePath)) {
                    feedback.addError("Document type '" + jcrContentType + "' not suitable for adding taxonomy field");
                    continue;
                }

                final Node editorTemplateNode = session.getNode(editorTemplatePath);
                if (editorTemplateNode.hasNode("classifiable")) {
                    feedback.addError("Document type '" + jcrContentType + "' already has a taxonomy.");
                    continue;
                }

                if (!contentTypeService.addMixinToContentType(jcrContentType, HIPPOTAXONOMY_MIXIN, session, true)) {
                    feedback.addError("Failed adding taxonomy to type '" + jcrContentType + "'.");
                    continue;
                }

                // create first taxonomy field
                final String taxonomyName = field.getTaxonomyName();
                final Node fieldNode = editorTemplateNode.addNode("classifiable", "frontend:plugin");
                fieldNode.setProperty("mixin", HIPPOTAXONOMY_MIXIN);
                fieldNode.setProperty("plugin.class", "org.hippoecm.frontend.editor.plugins.mixin.MixinLoaderPlugin");
                fieldNode.setProperty("wicket.id", contentTypeService.determineDefaultFieldPosition(jcrContentType));
                final Node clusterNode = fieldNode.addNode("cluster.options", "frontend:pluginconfig");
                clusterNode.setProperty("taxonomy.name", taxonomyName);
                markAsTaxonomyField(fieldNode, taxonomyName);
                session.save();
                changedDocuments.add(jcrContentType);
            }
        } catch (RepositoryException e) {
            log.error("Error adding taxonomy to a document", e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return feedback.addError("Failed to add taxonomy fields. See back-end logs for more info.");
        } finally {
            jcrService.destroySession(session);
        }

        final String message = changedDocuments.isEmpty()
                ? "No taxonomy fields were added"
                : "Added field(s) to following documents: " + changedDocuments.stream().collect(Collectors.joining(", "));
        return feedback.addSuccess(message);
    }

    /**
     * Note that the taxonomy plugin has a very intricate way of configuring taxonomy fields.
     * To avoid this complexity when checking which document type uses which taxonomies,
     * this plugin marks taxonomy fields by adding a plugin-specific property. That property
     * has no other function than to convey information to the plugin.
     */
    private void markAsTaxonomyField(final Node fieldNode, final String taxonomyName) {
        try {
            fieldNode.setProperty(TAXONOMY_FIELD_MARKER, taxonomyName);
        } catch (RepositoryException ex) {
            log.error("Error marking a taxonomy field.", ex);
        }
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

    private void addTaxonomyNode(final Session session, final Node taxonomiesNode, final String taxonomyName,
                                 final String[] locales) throws RepositoryException {
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
        taxonomyNode.setProperty("hippo:availability", new String[]{"live","preview"});
        taxonomyNode.setProperty(HIPPOTAXONOMY_LOCALES, locales);
        session.save();
    }

    private boolean hasTaxonomyContainer(final Session session) {
        try {
            return session.itemExists("/content/taxonomies")
                    && session.getNode("/content/taxonomies").getPrimaryNodeType().getName().equals("hippotaxonomy:container");
        } catch (RepositoryException e) {
            log.error("Error: {}", e);
        }
        return false;
    }
}
