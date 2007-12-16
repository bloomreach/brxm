package org.hippoecm.repository.servicing.client;

import org.apache.jackrabbit.rmi.client.ClientObject;

/* FIXME: [BvH] the decorating layer it probably not the right point for
 * most of this functionality
 */

public abstract class ClientManager extends ClientObject {

    protected ClientManager(LocalServicingAdapterFactory factory) {
        super(factory);
    }

    /**
     * Utility routine to set the thread context to the repository class loader.
     * Call it when making an RMI call and an object could be returned whose
     * class resides in the repository.
     */
    protected ClassLoader setContextClassLoader() {
        ClassLoader current = Thread.currentThread().getContextClassLoader();
        LocalServicingAdapterFactory factory = (LocalServicingAdapterFactory) getFactory();
        Thread.currentThread().setContextClassLoader(factory.getClassLoader());
        return current;
    }

}
