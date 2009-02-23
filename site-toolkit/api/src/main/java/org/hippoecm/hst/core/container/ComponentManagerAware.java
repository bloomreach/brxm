package org.hippoecm.hst.core.container;

/**
 * Interface which a component bean should implement if it wants to
 * have access the component manager containing itself.
 */
public interface ComponentManagerAware {
    
    void setComponentManager(ComponentManager componentManager);
    
}
