package org.onehippo.cms7.crisp.api.resource;

public interface Resource extends ResourceContainer {

    String gerResourceType();

    boolean isResourceType(String resourceType);

    String getName();

    String getPath();

    ValueMap getMetadata();

    ValueMap getValueMap();

    Resource getParent();

}
