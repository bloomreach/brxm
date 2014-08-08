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

package org.onehippo.cms7.essentials.plugins.tagging;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.onehippo.cms7.essentials.dashboard.ctx.PluginContext;
import org.onehippo.cms7.essentials.dashboard.instructions.InstructionStatus;
import org.onehippo.cms7.essentials.dashboard.packaging.DefaultInstructionPackage;
import org.onehippo.cms7.essentials.dashboard.utils.GlobalUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Forget about instructions. All we need to do is create a facet navigation node.
 */
public class TaggingInstructionPackage extends DefaultInstructionPackage {

    private static Logger log = LoggerFactory.getLogger(TaggingInstructionPackage.class);

    @Override
    public InstructionStatus execute(final PluginContext context) {
        InstructionStatus status = InstructionStatus.SUCCESS;
        final Session session = context.createSession();
        try {
            if (!session.nodeExists("/tags")) {
                final Node rootNode = session.getRootNode();
                final Node tags = rootNode.addNode("tags", "hippofacnav:facetnavigation");
                final String rootIdentifier = session.getNode("/content").getIdentifier();
                tags.setProperty("hippo:docbase", rootIdentifier);
                tags.setProperty("hippofacnav:facets", new String[]{"hippostd:tags"});
                tags.setProperty("hippofacnav:limit", 100);
                session.save();
            } else {
                log.debug("/tags node already exists");
            }
        } catch (RepositoryException e) {
            status = InstructionStatus.FAILED;
            log.error("Error setting up /tags facet node", e);
        } finally {
            GlobalUtils.cleanupSession(session);
        }

        return status;
    }
}