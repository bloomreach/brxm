/*
 * Copyright 2019-2020 Hippo B.V. (http://www.onehippo.com)
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

package com.onehippo.cms7.essentials.plugins.spademo;

import java.util.Map;
import java.util.function.BiConsumer;

import javax.inject.Inject;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.hippoecm.hst.configuration.HstNodeTypes;
import org.onehippo.cms7.essentials.sdk.api.install.Instruction;
import org.onehippo.cms7.essentials.sdk.api.service.JcrService;
import org.onehippo.cms7.essentials.sdk.api.service.SettingsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SpaDemoInstruction implements Instruction {

    private static Logger log = LoggerFactory.getLogger(SpaDemoInstruction.class);

    @Inject private JcrService jcrService;
    @Inject private SettingsService settingsService;

    @Override
    public Status execute(final Map<String, Object> parameters) {
        final String namespace = settingsService.getSettings().getProjectNamespace();
        final String hstRootNode = String.format("/hst:%s/hst:hosts/dev-localhost/localhost/hst:root", namespace);
        final Session session = jcrService.createSession();
        if (session == null) {
            return Status.FAILED;
        }

        try {
            // Configure Page Model API
            final Node root = session.getNode(hstRootNode);
            root.setProperty(HstNodeTypes.GENERAL_PROPERTY_PAGE_MODEL_API, "resourceapi");
            root.setProperty(HstNodeTypes.GENERAL_PROPERTY_RESPONSE_HEADERS, new String[] {
                    "Access-Control-Allow-Origin: *"
            });
            root.setProperty(HstNodeTypes.VIRTUALHOST_ALLOWED_ORIGINS, new String[] {
                    "http://localhost:3000"
            });

            // Update URL Rewriter configuration
            final Node urlRewriterConfig = session.getNode("/hippo:configuration/hippo:modules/urlrewriter/hippo:moduleconfig");
            urlRewriterConfig.setProperty("urlrewriter:ignorecontextpath", false);
            urlRewriterConfig.setProperty("urlrewriter:usequerystring", true);
            urlRewriterConfig.setProperty("urlrewriter:skippedprefixes", new String[]{
                    "/site/_cmsrest",
                    "/site/_cmssessioncontext",
                    "/site/_rp",
                    "/site/_hn:",
                    "/site/ping/"
            });

            session.save();
        } catch (RepositoryException e) {
            log.error("Error setting up Page Model API", e);
            return Status.FAILED;
        } finally {
            jcrService.destroySession(session);
        }

        return Status.SUCCESS;
    }

    @Override
    public void populateChangeMessages(final BiConsumer<Type, String> changeMessageQueue) {
        changeMessageQueue.accept(Type.EXECUTE, "Set up the Page Model API for the default channel.");
        changeMessageQueue.accept(Type.EXECUTE, "Configure the URL Rewriter to work as a reverse proxy.");
    }
}
