package org.onehippo.repository.documentworkflow.version;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

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

    public static VersionsMeta setCampaign(final Node handle, final Campaign campaign) throws RepositoryException {
        if (!handle.isNodeType(NT_HIPPO_VERSION_INFO)) {
            handle.addMixin(NT_HIPPO_VERSION_INFO);
        }
        final VersionsMeta versionsMeta = getVersionsMeta(handle);
        versionsMeta.setCampaign(campaign);
        try {
            handle.setProperty(HIPPO_VERSIONS_META, objectMapper.writeValueAsString(versionsMeta));
        } catch (JsonProcessingException e) {
            throw new RepositoryException("Cannot add campaign", e);
        }
        return versionsMeta;
    }

    public static VersionsMeta removeCampaign(final Node handle, final String campaignId) throws RepositoryException {
        if (!handle.isNodeType(NT_HIPPO_VERSION_INFO)) {
            handle.addMixin(NT_HIPPO_VERSION_INFO);
        }
        final VersionsMeta versionsMeta = getVersionsMeta(handle);
        versionsMeta.removeCampaign(campaignId);
        try {
            handle.setProperty(HIPPO_VERSIONS_META, objectMapper.writeValueAsString(versionsMeta));
        } catch (JsonProcessingException e) {
            throw new RepositoryException("Cannot add campaign", e);
        }
        return versionsMeta;
    }

    public static VersionsMeta removeCampaign(final Node handle, final Campaign campaign) throws RepositoryException {
        return removeCampaign(handle, campaign.getUuid());
    }
}
