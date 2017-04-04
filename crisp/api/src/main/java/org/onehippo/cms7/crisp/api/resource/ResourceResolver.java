package org.onehippo.cms7.crisp.api.resource;

import java.util.Iterator;

public interface ResourceResolver {

    Resource resolve(String absPath);
 
    Resource getResource(Resource base, String path);

    boolean isLive();

    boolean isResourceType(Resource resource, String resourceType);

    Iterable<Resource> getChildren(Resource parent);

    Iterator<Resource> listChildren(Resource parent);

    Iterator<Resource> findResources(String query, String language);

    void refresh();

    void close();

}
