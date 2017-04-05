package org.onehippo.cms7.crisp.api.resource;

import java.util.Iterator;
import java.util.Map;

public interface ResourceResolver {

    Resource resolve(String absPath, Map<String, Object> variables) throws ResourceException;
 
    Iterator<Resource> findResources(String baseAbsPath, Map<String, Object> variables, String query, String language) throws ResourceException;

    boolean isLive() throws ResourceException;

    void refresh() throws ResourceException;

    void close() throws ResourceException;

}
