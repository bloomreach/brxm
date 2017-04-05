package org.onehippo.cms7.crisp.api.resource;

import java.util.Map;

public interface ResourceResolver extends ResourceCacheResolvable {

    Resource resolve(String absPath, Map<String, Object> variables) throws ResourceException;

    ResourceContainable findResources(String baseAbsPath, Map<String, Object> variables, String query)
            throws ResourceException;

    ResourceContainable findResources(String baseAbsPath, Map<String, Object> variables, String query, String language)
            throws ResourceException;

    boolean isLive() throws ResourceException;

    void refresh() throws ResourceException;

    void close() throws ResourceException;

}
