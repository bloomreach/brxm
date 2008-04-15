package org.hippoecm.frontend.core;


public interface Plugin {

    String CLASSNAME = "plugin.class";

    String NAME = "plugin.name";

    void start(PluginContext context);

    void stop();
}
