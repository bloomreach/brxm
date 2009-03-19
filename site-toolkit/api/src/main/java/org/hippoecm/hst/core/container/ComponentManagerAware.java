package org.hippoecm.hst.core.container;

/**
 * Interface which a component bean should implement if it wants to
 * have access the component manager containing itself.
 * If a component which is initialized by {@link ComponentManager} implements
 * this interface, then it will be given the component manager at the 
 * initialization time. The component manager will invoke {@link #setComponentManager(ComponentManager)}
 * method to give itself to the component.
 * 
 * @version $Id$
 */
public interface ComponentManagerAware {
    
    /**
     * Sets the component manager
     * 
     * @param componentManager
     */
    void setComponentManager(ComponentManager componentManager);
    
}
