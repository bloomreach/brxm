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
 * <?xml version="1.0" encoding="UTF-8"?>
 * <sv:node xmlns:sv="http://www.jcp.org/jcr/sv/1.0" sv:name="blogfacets">
 * <sv:property sv:name="jcr:primaryType" sv:type="Name">
 * <sv:value>hippofacnav:facetnavigation</sv:value>
 * </sv:property>
 * <sv:property sv:name="hippo:count" sv:type="Long">
 * <sv:value>15</sv:value>
 * </sv:property>
 * <sv:property sv:name="hippo:docbase" sv:type="String">
 * <sv:value>0a137b2f-2e7c-411b-a810-cd86d3ee48cc</sv:value>
 * </sv:property>
 * <sv:property sv:name="hippofacnav:facetnodenames" sv:type="String" sv:multiple="true">
 * <sv:value>Author</sv:value>
 * </sv:property>
 * <sv:property sv:name="hippofacnav:facets" sv:type="String" sv:multiple="true">
 * <sv:value>connect:author</sv:value>
 * </sv:property>
 * <sv:property sv:name="hippofacnav:filters" sv:type="String" sv:multiple="true">
 * <sv:value>jcr:primaryType = connect:blogpost</sv:value>
 * </sv:property>
 * <sv:property sv:name="hippofacnav:sortby" sv:type="String" sv:multiple="true">
 * <sv:value>connect:publicationdate</sv:value>
 * </sv:property>
 * <sv:property sv:name="hippofacnav:sortorder" sv:type="String" sv:multiple="true">
 * <sv:value>descending</sv:value>
 * </sv:property>
 * </sv:node>
 *
 * @version "$Id$"
 */
public class FacetInstruction implements Instruction {

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
            final String docRef = session.getNode(targetNode +"/blog").getIdentifier();
            blogFacets.setProperty("hippo:docbase", docRef);
            blogFacets.setProperty("hippo:count", "2");
            // TODO read from config
            blogFacets.setProperty("hippofacnav:facetnodenames", new String[]{"Author"});
            blogFacets.setProperty("hippofacnav:facets", new String[]{namespace + ":author"});
            //..
            blogFacets.setProperty("hippofacnav:filters", new String[]{"jcr:primaryType = " + namespace + ":blogpost"});
            blogFacets.setProperty("hippofacnav:sortby", new String[]{namespace + ":publicationdate"});
            blogFacets.setProperty("hippofacnav:sortorder", new String[]{"descending"});
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
