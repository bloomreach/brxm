package org.onehippo.cms7.crisp.api.resource;

import java.util.Map;

public interface ResourceResolver extends ResourceCacheResolvable {

    Resource resolve(String absPath) throws ResourceException;

    Resource resolve(String absPath, Map<String, Object> pathVariables) throws ResourceException;

    ResourceContainable findResources(String baseAbsPath) throws ResourceException;

    ResourceContainable findResources(String baseAbsPath, Map<String, Object> pathVariables) throws ResourceException;

    boolean isLive() throws ResourceException;

    void refresh() throws ResourceException;

    void close() throws ResourceException;

}
