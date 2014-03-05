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

package org.onehippo.cms7.essentials.dashboard.blog;

import java.util.Map;

import javax.inject.Inject;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.onehippo.cms7.essentials.dashboard.ctx.PluginContext;
import org.onehippo.cms7.essentials.dashboard.instructions.Instruction;
import org.onehippo.cms7.essentials.dashboard.instructions.InstructionStatus;
import org.onehippo.cms7.essentials.dashboard.utils.EssentialConst;
import org.onehippo.cms7.essentials.dashboard.utils.GlobalUtils;
import org.onehippo.cms7.essentials.dashboard.utils.TemplateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;
import com.google.common.eventbus.EventBus;

/**
 * @version "$Id$"
 */
public class FacetInstruction implements Instruction {

    private static final String DATE_FACET = ":publicationdate$[\n" +
            "                {name:'last 7 days',resolution:'day', begin:-6, end:1},\n" +
            "                {name:'last month',resolution:'day', begin:-30, end:1},\n" +
            "                {name:'last 3 months',resolution:'day', begin:-91, end:1},\n" +
            "                {name:'last 6 months',resolution:'day', begin:-182, end:1},\n" +
            "                {name:'last year',resolution:'day', begin:-365, end:1}\n" +
            "                ]";
    private static Logger log = LoggerFactory.getLogger(FacetInstruction.class);


    @Inject
    private EventBus eventBus;

    private String message = "Created blog facet at: {{facetMessage}}";

    @Override
    public String getMessage() {

        return message;
    }

    @Override
    public void setMessage(final String message) {

    }

    @Override
    public String getAction() {
        return null;
    }

    @Override
    public void setAction(final String action) {

    }

    @Override
    public InstructionStatus process(final PluginContext context, final InstructionStatus previousStatus) {
        processPlaceholders(context.getPlaceholderData());
        final String namespace = (String) context.getPlaceholderData().get(EssentialConst.PLACEHOLDER_NAMESPACE);
        final String targetNode = "/content/documents/" + namespace;
        Session session = null;
        try {
            session = context.createSession();
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
            blogFacets.setProperty("hippofacnav:facets", new String[]{namespace + ":author", namespace + ":categories", "hippostd:tags", namespace + DATE_FACET});
            blogFacets.setProperty("hippofacnav:filters", new String[]{"jcr:primaryType = " + namespace + ":blogpost"});
            blogFacets.setProperty("hippofacnav:sortby", new String[]{namespace + ":publicationdate"});
            blogFacets.setProperty("hippofacnav:sortorder", new String[]{"descending"});
            blogFacets.setProperty("hippofacnav:limit", 100L);
            session.save();

        } catch (RepositoryException e) {
            log.error("Error creating blog facet", e);
            return InstructionStatus.FAILED;
        } finally {
            GlobalUtils.cleanupSession(session);
        }


        return InstructionStatus.SUCCESS;
    }

    @Override
    public void processPlaceholders(final Map<String, Object> data) {
        data.put("facetMessage", "Blog facets");
        final String myMessage = TemplateUtils.replaceTemplateData(message, data);
        if (!Strings.isNullOrEmpty(myMessage)) {
            message = myMessage;
        }
    }
}
