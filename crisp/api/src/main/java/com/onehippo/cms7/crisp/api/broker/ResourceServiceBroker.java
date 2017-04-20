package com.onehippo.cms7.crisp.api.broker;

import java.util.Map;

import org.onehippo.cms7.services.SingletonService;

import com.onehippo.cms7.crisp.api.resource.Resource;
import com.onehippo.cms7.crisp.api.resource.ResourceContainer;
import com.onehippo.cms7.crisp.api.resource.ResourceDataCache;
import com.onehippo.cms7.crisp.api.resource.ResourceException;
import com.onehippo.cms7.crisp.api.resource.ResourceLink;

@SingletonService
public interface ResourceServiceBroker {

    Resource resolve(String resourceSpace, String absResourcePath) throws ResourceException;

    Resource resolve(String resourceSpace, String absResourcePath, Map<String, Object> pathVariables)
            throws ResourceException;

    ResourceContainer findResources(String resourceSpace, String baseAbsPath) throws ResourceException;

    ResourceContainer findResources(String resourceSpace, String baseAbsPath, Map<String, Object> pathVariables)
            throws ResourceException;

    ResourceLink resolveLink(String resourceSpace, ResourceContainer resource) throws ResourceException;

    ResourceLink resolveLink(String resourceSpace, ResourceContainer resource, Map<String, Object> linkVariables)
            throws ResourceException;

    ResourceDataCache getResourceDataCache(String resourceSpace);

}
