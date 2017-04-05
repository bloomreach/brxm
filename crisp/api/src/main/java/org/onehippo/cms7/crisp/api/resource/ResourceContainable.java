package org.onehippo.cms7.crisp.api.resource;

import java.util.Iterator;

public interface ResourceContainable {

    boolean hasChildren();

    Iterator<Resource> listChildren();

    Iterable<Resource> getChildren();

}
