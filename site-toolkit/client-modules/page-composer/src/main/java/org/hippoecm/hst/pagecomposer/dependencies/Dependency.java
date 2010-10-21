package org.hippoecm.hst.pagecomposer.dependencies;

import java.util.Collection;

public interface Dependency {

    Collection<Dependency> getDependencies(boolean devMode);

    String getPath();

    String asString(String path);

    void setParent(Dependency parent);
}
