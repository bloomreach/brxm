/*
 * Copyright 2021 Bloomreach. All rights reserved. (https://www.bloomreach.com/)
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
package org.onehippo.repository.documentworkflow.campaign;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.onehippo.repository.campaign.Campaign;
import org.onehippo.repository.campaign.VersionLabel;
import org.onehippo.repository.campaign.VersionsMeta;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.hippoecm.repository.api.HippoNodeType.HIPPO_VERSIONS_META;
import static org.hippoecm.repository.api.HippoNodeType.NT_HIPPO_VERSION_INFO;
import static org.hippoecm.repository.util.JcrUtils.getStringProperty;

public class JcrVersionsMetaUtils {

    final static Logger log = LoggerFactory.getLogger(JcrVersionsMetaUtils.class);

    final static ObjectMapper objectMapper = new ObjectMapper();

    public static VersionsMeta getVersionsMeta(final Node handle) throws RepositoryException {
        final String versionsMetaString = getStringProperty(handle, HIPPO_VERSIONS_META, null);
        if (versionsMetaString == null) {
            return new VersionsMeta();
        }
        try {
            return objectMapper.readValue(versionsMetaString, VersionsMeta.class);
        } catch (JsonProcessingException e) {
            if (log.isDebugEnabled()) {
                log.error("Invalid stored versionsMeta at '{}', return empty VersionsMeta", handle.getPath(),  e);
            } else {
                log.error("Invalid stored versionsMeta at '{}', return empty VersionsMeta : {}", handle.getPath(), e.getMessage());
            }
            return new VersionsMeta();
        }
    }

    public static void setCampaign(final Node handle, final Campaign campaign) throws RepositoryException {
        final VersionsMeta versionsMeta = getVersionsMeta(handle);
        versionsMeta.setCampaign(campaign);
        try {
            handle.addMixin(NT_HIPPO_VERSION_INFO);
            handle.setProperty(HIPPO_VERSIONS_META, objectMapper.writeValueAsString(versionsMeta));
        } catch (JsonProcessingException e) {
            throw new RepositoryException("Cannot add campaign", e);
        }
    }

    public static void removeCampaign(final Node handle, final String frozenNodeId) throws RepositoryException {
        final VersionsMeta versionsMeta = getVersionsMeta(handle);
        versionsMeta.removeCampaign(frozenNodeId);
        try {
            handle.addMixin(NT_HIPPO_VERSION_INFO);
            handle.setProperty(HIPPO_VERSIONS_META, objectMapper.writeValueAsString(versionsMeta));
        } catch (JsonProcessingException e) {
            throw new RepositoryException("Cannot add campaign", e);
        }
    }

    public static void setVersionLabel(final Node handle, final VersionLabel versionLabel) throws RepositoryException {
        final VersionsMeta versionsMeta = getVersionsMeta(handle);
        versionsMeta.setVersionLabel(versionLabel);
        try {
            handle.addMixin(NT_HIPPO_VERSION_INFO);
            handle.setProperty(HIPPO_VERSIONS_META, objectMapper.writeValueAsString(versionsMeta));
        } catch (JsonProcessingException e) {
            throw new RepositoryException("Cannot add campaign", e);
        }
    }

    public static void removeVersionLabel(final Node handle, final String frozenNodeId) throws RepositoryException {
        final VersionsMeta versionsMeta = getVersionsMeta(handle);
        versionsMeta.removeVersionLabel(frozenNodeId);
        try {
            handle.addMixin(NT_HIPPO_VERSION_INFO);
            handle.setProperty(HIPPO_VERSIONS_META, objectMapper.writeValueAsString(versionsMeta));
        } catch (JsonProcessingException e) {
            throw new RepositoryException("Cannot add campaign", e);
        }
    }
}
