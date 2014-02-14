/*
 *  Copyright 2011-2013 Hippo B.V. (http://www.onehippo.com)
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

import org.apache.commons.lang.StringUtils;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.Plugin;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.service.IRestProxyService;
import org.hippoecm.hst.rest.DocumentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.onehippo.cms7.channelmanager.ChannelManagerConsts.CONFIG_REST_PROXY_SERVICE_ID;

/**
 * Returns the fully qualified, canonical URL to a document in a channel of a certain type (preview, live,
 * or any custom type used in the HST mount configuration). The implementation uses the CMS-HST REST API to
 * retrieve the URL.
 *
 * The following configuration properties are available:
 * <ul>
 * <li>'rest.proxy.service.id': Referenced by CONFIG_REST_PROXY_SERVICE_ID constant, The ID of the REST proxy service to use. If omitted, the default REST proxy service is
 *     used.</li>
 * <li>'service.id': the ID to register this service under.</li>
 * <li>'type': the type of mounts to use for link creation. If omitted or empty, the type 'live' is used.</li>
 * </ul>
 */
public class ChannelDocumentUrlService extends Plugin implements IDocumentUrlService {

    private static final String DEFAULT_TYPE = "live";
    private static final Logger log = LoggerFactory.getLogger(ChannelDocumentUrlService.class);
    private static final long serialVersionUID = 1L;

    private final IRestProxyService restProxyService;
    private final String type;

    public ChannelDocumentUrlService(IPluginContext context, IPluginConfig config) {
        super(context, config);

        // load REST proxy service
        final String restProxyServiceId = config.getString(CONFIG_REST_PROXY_SERVICE_ID, IRestProxyService.class.getName());
        log.debug("Using REST proxy service with id '{}'", restProxyServiceId);
        restProxyService = context.getService(restProxyServiceId, IRestProxyService.class);
        if (restProxyService == null) {
            throw new IllegalStateException("Unknown REST proxy service: '" + restProxyServiceId + "'. "
                    + "Please set the configuration property '" + CONFIG_REST_PROXY_SERVICE_ID + "'");
        }

        // read type from configuration
        type = config.getString("type", DEFAULT_TYPE);
        log.debug("Using type '{}'", type);

        final String serviceId = config.getString("service.id", IDocumentUrlService.DEFAULT_SERVICE_ID);
        context.registerService(this, serviceId);
    }

    @Override
    public String getUrl(final Node documentNode) {
        final String uuid = getUuidOfHandle(documentNode);

        if (uuid == null) {
            return null;
        }

        final DocumentService documentService = restProxyService.createRestProxy(DocumentService.class);
        if (documentService == null) {
            log.warn("The REST proxy service does provide a proxy for class '{}'. The document URL service can therefore not generate a URL for the document with UUID '{}'",
                    DocumentService.class.getCanonicalName(), uuid);

            return null;
        }

        String url = documentService.getUrl(uuid, type);

        return StringUtils.isBlank(url) ? null : url;
    }

    private String getUuidOfHandle(Node documentNode) {
        try {
            return documentNode.getParent().getIdentifier();
        } catch (RepositoryException e) {
            log.error("Error retrieving UUID from document handle", e);
        }
        return null;
    }

}
