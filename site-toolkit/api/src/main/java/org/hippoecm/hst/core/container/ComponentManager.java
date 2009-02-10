package org.hippoecm.hst.core.container;

public interface ComponentManager
{
    void initialize();
    void start();
    Object getComponent(String name);
    void stop();
    void close();
}
