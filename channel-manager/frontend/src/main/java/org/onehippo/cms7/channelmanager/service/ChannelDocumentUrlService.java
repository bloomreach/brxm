/*
 *  Copyright 2011-2018 Hippo B.V. (http://www.onehippo.com)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.onehippo.cms7.channelmanager.service;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.Plugin;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.hst.platform.api.PlatformServices;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.onehippo.cms7.channelmanager.HstUtil.getHostGroup;

/**
 * Returns the fully qualified, canonical URL to a document in a channel of a certain type (preview, live,
 * or any custom type used in the HST mount configuration). The implementation uses the CMS-HST REST API to
 * retrieve the URL.
 *
 * The following configuration properties are available:
 * <ul>
 * <li>'service.id': the ID to register this service under.</li>
 * <li>'type': the type of mounts to use for link creation. If omitted or empty, the type 'live' is used.</li>
 * </ul>
 */
public class ChannelDocumentUrlService extends Plugin implements IDocumentUrlService {

    private static final String DEFAULT_TYPE = "live";
    private static final Logger log = LoggerFactory.getLogger(ChannelDocumentUrlService.class);
    private static final long serialVersionUID = 1L;

    private final String type;

    public ChannelDocumentUrlService(IPluginContext context, IPluginConfig config) {
        super(context, config);

        // read type from configuration
        type = config.getString("type", DEFAULT_TYPE);

        log.debug("Using type '{}'", type);

        final String serviceId = config.getString("service.id", IDocumentUrlService.DEFAULT_SERVICE_ID);
        context.registerService(this, serviceId);
    }

    private javax.jcr.Session getUserJcrSession() {
        return UserSession.get().getJcrSession();
    }

    @Override
    public String getUrl(final Node documentNode) {
        final String path;
        try {
            path = documentNode.getPath();
        } catch (RepositoryException e) {
            log.error("Error retrieving path from document", e);
            return null;
        }
        final String uuid = getUuidOfHandle(documentNode);
        if (uuid == null) {
            log.info("Could not find handle for document '{}'", path);
            return null;
        }

        try {
            return HippoServiceRegistry.getService(PlatformServices.class).getDocumentService()
                    .getUrl(getUserJcrSession(), getHostGroup(), uuid, type);
        } catch (IllegalStateException e) {
            log.info("Cannot get document URL: '{}'", e.getMessage());
            return null;
        }
    }

    private String getUuidOfHandle(Node documentNode) {
        try {
            return documentNode.getIdentifier();
        } catch (RepositoryException e) {
            log.error("Error retrieving UUID from document handle", e);
        }
        return null;
    }

}
