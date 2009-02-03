package org.hippoecm.hst.core.container;

public interface ComponentManager
{
    void start();
    Object getComponent(String name);
    void stop();
}
