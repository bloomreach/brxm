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

package org.onehippo.cms7.essentials.dashboard.utils;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Workspace;
import javax.jcr.query.InvalidQueryException;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;

import org.onehippo.cms7.essentials.dashboard.ctx.PluginContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @version "$Id$"
 */
public final class HstUtils {

    private static final Logger log = LoggerFactory.getLogger(HstUtils.class);

    public static Set<Node> getHstMounts(final PluginContext context) {
        final Session session = context.createSession();

        try {
            final Workspace workspace = session.getWorkspace();
            final QueryManager queryManager = workspace.getQueryManager();
            final Query query = queryManager.createQuery("//hst:hst/hst:hosts//element(*, hst:mount)", "xpath");
            final QueryResult queryResult = query.execute();
            final NodeIterator nodes = queryResult.getNodes();
            final Set<Node> retVal = new HashSet<>();
            while (nodes.hasNext()) {
                final Node node = nodes.nextNode();
                retVal.add(node);
            }
            return retVal;
        } catch (InvalidQueryException e) {
            log.error("Error creating query", e);
        } catch (RepositoryException e) {
            log.error("Error fetching hst:host nodes", e);
        } finally {
            GlobalUtils.cleanupSession(session);
        }
        return Collections.emptySet();

    }


    private HstUtils() {
        // utility
    }


}
