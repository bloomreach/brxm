/*
 * Copyright 2019 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.cms7.essentials.plugins.cloud;

import java.util.Map;
import java.util.function.BiConsumer;

import javax.inject.Inject;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.onehippo.cms7.essentials.sdk.api.install.Instruction;
import org.onehippo.cms7.essentials.sdk.api.service.JcrService;
import org.onehippo.cms7.essentials.sdk.api.service.SettingsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Adds a single property with a constant value to the dev-localhost host group for hst:platform and the current site.
 */
public class AutoHostTemplateInstruction implements Instruction {

    private static Logger log = LoggerFactory.getLogger(AutoHostTemplateInstruction.class);

    private static final String HST_AUTOHOSTTEMPLATE_PROPERTY = "hst:autohosttemplate";
    private static final String BRC_CLUSTER_HOST_PATTERN = "https://*.onehippo.io";
    private static final String LOCALHOST_GROUP = "/hst:hosts/dev-localhost";
    private static final String PLATFORM_PATH = "/hst:platform" + LOCALHOST_GROUP;

    @Inject private JcrService jcrService;
    @Inject private SettingsService settingsService;

    @Override
    public Status execute(final Map<String, Object> parameters) {
        final String hstRoot = settingsService.getSettings().getHstRoot();
        final String sitePath = hstRoot + LOCALHOST_GROUP;

        final Session session = jcrService.createSession();
        if (session == null) {
            return Status.FAILED;
        }

        try {
            final Node siteNode = session.getNode(sitePath);
            final Node platformNode = session.getNode(PLATFORM_PATH);
            siteNode.setProperty(HST_AUTOHOSTTEMPLATE_PROPERTY, new String[] {BRC_CLUSTER_HOST_PATTERN});
            platformNode.setProperty(HST_AUTOHOSTTEMPLATE_PROPERTY, new String[] {BRC_CLUSTER_HOST_PATTERN});
            session.save();
        } catch (RepositoryException e) {
            log.error("Error adding HST autohosttemplate property", e);
            return Status.FAILED;
        } finally {
            jcrService.destroySession(session);
        }

        return Status.SUCCESS;
    }

    @Override
    public void populateChangeMessages(final BiConsumer<Type, String> changeMessageQueue) {
        changeMessageQueue.accept(Type.EXECUTE, "Add " + HST_AUTOHOSTTEMPLATE_PROPERTY + " property to dev-localhost group");
    }
}
