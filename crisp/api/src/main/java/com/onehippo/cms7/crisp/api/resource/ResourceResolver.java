/*
 * Copyright 2017 Hippo B.V. (http://www.onehippo.com)
 */
package com.onehippo.cms7.crisp.api.resource;

import java.util.Map;

public interface ResourceResolver extends ResourceCacheResolvable {

    Resource resolve(String absPath) throws ResourceException;

    Resource resolve(String absPath, Map<String, Object> pathVariables) throws ResourceException;

    Resource findResources(String baseAbsPath) throws ResourceException;

    Resource findResources(String baseAbsPath, Map<String, Object> pathVariables) throws ResourceException;

    boolean isLive() throws ResourceException;

    void refresh() throws ResourceException;

    void close() throws ResourceException;

    ResourceLinkResolver getResourceLinkResolver();

}
