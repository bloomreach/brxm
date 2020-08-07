/*
 * Copyright 2020 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.cms7.essentials.plugin.contentfeed;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.function.BiConsumer;

import javax.inject.Inject;

import org.onehippo.cms7.essentials.sdk.api.install.Instruction;
import org.onehippo.cms7.essentials.sdk.api.model.Module;
import org.onehippo.cms7.essentials.sdk.api.service.ProjectService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ContentFeedInstruction implements Instruction {

    private static final Logger log = LoggerFactory.getLogger(ContentFeedInstruction.class);

    private static final String HOST_ENVIRONMENT = "brx.contentfeed.environment";

    private static final String SEARCH_URI = "brx.search.uri";
    private static final String SEARCH_ACCOUNTID = "brx.search.accountId";
    private static final String SEARCH_CATALOGS = "brx.search.catalogs";

    private static final String DATACONNECT_BASEURL = "brx.dataconnect.api.baseurl";
    private static final String DATACONNECT_APIKEY = "brx.dataconnect.api.key";
    private static final String DATACONNECT_ACCOUNTID = "brx.dataconnect.accountId";
    private static final String DATACONNECT_CATALOG = "brx.dataconnect.catalog";
    private static final String DATACONNECT_HOST = "brx.dataconnect.transfer.connection.host";
    private static final String DATACONNECT_LOCATION = "brx.dataconnect.transfer.connection.location";
    private static final String DATACONNECT_USERNAME = "brx.dataconnect.transfer.connection.username";
    private static final String DATACONNECT_PRIVATEKEYPATH = "brx.dataconnect.transfer.connection.privatekeypath";
    private static final String DATACONNECT_SERVERHOSTKEY = "brx.dataconnect.transfer.connection.serverhostkey";

    private static final String DEFAULT_SEARCH_URI = "https://staging-core.dxpapi.com/api/v1/core/";
    private static final String DEFAULT_BASEURL = "http://api-staging.connect.bloomreach.com/dataconnect/api/v1/";
    private static final String DEFAULT_HOST = "sftp-staging.connect.bloomreach.com";

    @Inject
    private ProjectService projectService;

    @Override
    public Status execute(final Map<String, Object> parameters) {
        final Path siteConfig = projectService.getWebInfPathForModule(Module.SITE_WEBAPP).resolve("hst-config.properties");
        final Path cmsConfig = projectService.getWebInfPathForModule(Module.CMS).resolve("hst-config.properties");
        try {

            final StringBuilder siteConfigContent = new StringBuilder(new String(Files.readAllBytes(siteConfig)));
            siteConfigContent.append("\n\n");

            addProperty(siteConfigContent, SEARCH_URI, DEFAULT_SEARCH_URI);
            addProperty(siteConfigContent, SEARCH_ACCOUNTID, "");
            addProperty(siteConfigContent, SEARCH_CATALOGS, "");
            Files.write(siteConfig, siteConfigContent.toString().getBytes());

            final StringBuilder cmsConfigContent = new StringBuilder(new String(Files.readAllBytes(cmsConfig)));
            cmsConfigContent.append("\n\n");

            addProperty(cmsConfigContent, SEARCH_URI, DEFAULT_SEARCH_URI);
            addProperty(cmsConfigContent, SEARCH_ACCOUNTID, "");
            addProperty(cmsConfigContent, SEARCH_CATALOGS, "");

            cmsConfigContent.append("\n");

            addProperty(cmsConfigContent, HOST_ENVIRONMENT, "");
            addProperty(cmsConfigContent, DATACONNECT_BASEURL, DEFAULT_BASEURL);
            addProperty(cmsConfigContent, DATACONNECT_APIKEY, "");
            addProperty(cmsConfigContent, DATACONNECT_ACCOUNTID, "");
            addProperty(cmsConfigContent, DATACONNECT_CATALOG, "");
            addProperty(cmsConfigContent, DATACONNECT_HOST, DEFAULT_HOST);
            addProperty(cmsConfigContent, DATACONNECT_LOCATION, "");
            addProperty(cmsConfigContent, DATACONNECT_USERNAME, "");
            addProperty(cmsConfigContent, DATACONNECT_PRIVATEKEYPATH, "");
            addProperty(cmsConfigContent, DATACONNECT_SERVERHOSTKEY, "");
            Files.write(cmsConfig, cmsConfigContent.toString().getBytes());

        } catch (IOException e) {
            log.warn("Failed to update configuration properties.", e);
            return Status.FAILED;
        }

        return Status.SUCCESS;
    }

    private void addProperty(final StringBuilder configContent, final String propertyName, final String defaultValue) {
        if (!configContent.toString().contains(propertyName)) {
            configContent.append(propertyName).append(" = ").append(defaultValue).append("\n");
        }
    }

    @Override
    public void populateChangeMessages(final BiConsumer<Type, String> changeMessageQueue) {
        changeMessageQueue.accept(Type.EXECUTE,
                "Add HST configuration properties to configure Content Feed Export & Search in cms and site webapps.");
    }
}
