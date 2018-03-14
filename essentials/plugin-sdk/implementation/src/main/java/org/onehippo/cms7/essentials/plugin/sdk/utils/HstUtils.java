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

package org.onehippo.cms7.essentials.plugin.sdk.utils;

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

import org.onehippo.cms7.essentials.sdk.api.service.JcrService;
import org.onehippo.cms7.essentials.sdk.api.service.SettingsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @version "$Id$"
 */
public final class HstUtils {

    private static final Logger log = LoggerFactory.getLogger(HstUtils.class);

    private HstUtils() {
        // utility
    }

    public static void erasePreview(final JcrService jcrService, final SettingsService settingsService) {
        final String namespace = settingsService.getSettings().getProjectNamespace();
        final String preview = namespace + "-preview";
        final Session session = jcrService.createSession();

        try {
            final Node configurations = session.getNode("/hst:hst/hst:configurations");
            if (configurations.hasNode(preview)) {
                configurations.getNode(preview).remove();
            }
            session.save();
        } catch (RepositoryException ex) {
            log.warn("Unable to delete preview configuration", ex);
        } finally {
            jcrService.destroySession(session);
        }
    }
}
