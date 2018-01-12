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

package org.onehippo.cms7.essentials.plugins.blog;

import java.util.function.BiConsumer;

import javax.inject.Inject;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.onehippo.cms7.essentials.dashboard.ctx.PluginContext;
import org.onehippo.cms7.essentials.dashboard.instructions.Instruction;
import org.onehippo.cms7.essentials.dashboard.service.JcrService;
import org.onehippo.cms7.essentials.dashboard.service.SettingsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @version "$Id$"
 */
public class BlogFacetInstruction implements Instruction {

    private static final String DATE_FACET = ":publicationdate$[\n" +
            "                {name:'Last 7 days',resolution:'day', begin:-6, end:1},\n" +
            "                {name:'Last month',resolution:'day', begin:-30, end:1},\n" +
            "                {name:'Last 3 months',resolution:'day', begin:-91, end:1},\n" +
            "                {name:'Last 6 months',resolution:'day', begin:-182, end:1},\n" +
            "                {name:'Last year',resolution:'day', begin:-365, end:1}\n" +
            "                ]";
    private static final long DEFAULT_FACET_LIMIT = 100L;
    private static Logger log = LoggerFactory.getLogger(BlogFacetInstruction.class);

    @Inject private JcrService jcrService;
    @Inject private SettingsService settingsService;

    @Override
    public Status execute(final PluginContext context) {
        final String namespace = settingsService.getSettings().getProjectNamespace();
        final String targetNode = "/content/documents/" + namespace;
        final Session session = jcrService.createSession();
        if (session == null) {
            return Status.FAILED;
        }

        try {
            final Node root = session.getNode(targetNode);
            final String facetName = "blogFacets";
            if (root.hasNode(facetName)) {
                root.getNode(facetName).remove();
            }

            final Node blogFacets = root.addNode(facetName, "hippofacnav:facetnavigation");
            final String docRef = session.getNode(targetNode + "/blog").getIdentifier();
            blogFacets.setProperty("hippo:docbase", docRef);
            blogFacets.setProperty("hippo:count", "0");
            blogFacets.setProperty("hippofacnav:facetnodenames", new String[]{"Authors", "Categories", "Tags", "Date"});
            blogFacets.setProperty("hippofacnav:facets", new String[]{namespace + ":authornames", namespace + ":categories", "hippostd:tags", namespace + DATE_FACET});
            blogFacets.setProperty("hippofacnav:filters", new String[]{"jcr:primaryType = " + namespace + ":blogpost"});
            blogFacets.setProperty("hippofacnav:sortby", new String[]{namespace + ":publicationdate"});
            blogFacets.setProperty("hippofacnav:sortorder", new String[]{"descending"});
            blogFacets.setProperty("hippofacnav:limit", DEFAULT_FACET_LIMIT);
            session.save();
        } catch (RepositoryException e) {
            log.error("Error creating blog facet", e);
            return Status.FAILED;
        } finally {
            jcrService.destroySession(session);
        }

        return Status.SUCCESS;
    }

    @Override
    public void populateChangeMessages(final BiConsumer<Type, String> changeMessageQueue) {
        changeMessageQueue.accept(Type.EXECUTE, "Create blog facet at: /content/documents/{{namespace}}/blogFacets");
    }
}
