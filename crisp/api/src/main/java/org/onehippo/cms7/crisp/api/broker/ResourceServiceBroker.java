package org.onehippo.cms7.crisp.api.broker;

import java.util.Map;

import org.onehippo.cms7.crisp.api.resource.Resource;
import org.onehippo.cms7.crisp.api.resource.ResourceContainer;
import org.onehippo.cms7.crisp.api.resource.ResourceException;

public interface ResourceServiceBroker {

    Resource resolve(String resourceSpace, String absResourcePath) throws ResourceException;

    Resource resolve(String resourceSpace, String absResourcePath, Map<String, Object> pathVariables)
            throws ResourceException;

    ResourceContainer findResources(String resourceSpace, String baseAbsPath) throws ResourceException;

    ResourceContainer findResources(String resourceSpace, String baseAbsPath, Map<String, Object> pathVariables)
            throws ResourceException;

}
