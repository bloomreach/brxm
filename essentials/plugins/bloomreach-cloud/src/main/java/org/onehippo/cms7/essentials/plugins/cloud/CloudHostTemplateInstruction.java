/*
 * Copyright 2019-2021 Hippo B.V. (http://www.onehippo.com)
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

import java.util.HashMap;
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
 * Adds bloomreach.cloud host configuration for hst:platform and the current site.
 */
public class CloudHostTemplateInstruction implements Instruction {

    private static final Logger log = LoggerFactory.getLogger(CloudHostTemplateInstruction.class);

    private static final String BR_CLOUD_GROUP = "br-cloud";
    private static final String HST_HOSTS_PATH = "/hst:hosts";
    private static final String PLATFORM_HOSTS_PATH = "/hst:platform" + HST_HOSTS_PATH;

    @Inject private JcrService jcrService;

    @Inject private SettingsService settingsService;

    @Override
    public Status execute(final Map<String, Object> parameters) {
        final Session session = jcrService.createSession();
        if (session == null) {
            return Status.FAILED;
        }
        try {
            final Map<String, Object> properties = new HashMap<>();
            properties.put("hostGroupName", BR_CLOUD_GROUP);
            properties.put("namespace",  settingsService.getSettings().getProjectNamespace());

            final Node hstHostsNode = session.getNode(settingsService.getSettings().getHstRoot() + HST_HOSTS_PATH);
            final Node platformHostsNode = session.getNode(PLATFORM_HOSTS_PATH);
            if(hstHostsNode.hasNode(BR_CLOUD_GROUP) || platformHostsNode.hasNode(BR_CLOUD_GROUP)) {
                final String msg = String.format("Host configuration for '%s' already exists, skipping creation.", BR_CLOUD_GROUP);
                log.warn(msg);
                return Status.SKIPPED;
            } else {
                jcrService.importResource(hstHostsNode, "/br-cloud-host-configuration.xml", properties);
                jcrService.importResource(platformHostsNode, "/br-cloud-platform-configuration.xml", properties);
                session.save();
            }
        } catch (RepositoryException e) {
            log.error("Error adding br-cloud host configuration", e);
            return Status.FAILED;
        } finally {
            jcrService.destroySession(session);
        }

        return Status.SUCCESS;
    }

    @Override
    public void populateChangeMessages(final BiConsumer<Type, String> changeMessageQueue) {
        changeMessageQueue.accept(Type.EXECUTE, "Add " + BR_CLOUD_GROUP + " host configuration");
    }
}
