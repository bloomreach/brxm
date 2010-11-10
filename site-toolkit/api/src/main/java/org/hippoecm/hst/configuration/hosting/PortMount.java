package org.hippoecm.hst.configuration.hosting;

public interface PortMount {

    /**
     * @return Returns the portnumber associated with this {@link PortMount} object. 
     */
    int getPortNumber();
    
    /**
     * A {@link PortMount} has to have at least a root {@link Mount}, otherwise it is not a valid PortNumber and cannot be used.
     * @return the root {@link Mount} for this PortNumber object. When this PortMount has an invalid configured {@link Mount} or no {@link Mount}, <code>null</code> will be returned
     */
    Mount getRootMount();
    
   
}
