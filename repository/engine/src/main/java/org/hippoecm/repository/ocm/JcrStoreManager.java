/*
 *  Copyright 2009 Hippo.
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.hippoecm.repository.ocm;

import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.datanucleus.ClassLoaderResolver;
import org.datanucleus.OMFContext;
import org.datanucleus.ObjectManager;
import org.datanucleus.metadata.AbstractClassMetaData;
import org.datanucleus.store.AbstractStoreManager;
import org.datanucleus.store.DefaultCandidateExtent;
import org.datanucleus.store.ExecutionContext;
import org.datanucleus.store.Extent;
import org.datanucleus.store.NucleusConnection;
import org.datanucleus.store.NucleusConnectionImpl;
import org.datanucleus.store.connection.ConnectionFactory;
import org.datanucleus.store.connection.ManagedConnection;
import org.datanucleus.store.exceptions.NoExtentException;
import org.hippoecm.repository.ocm.ConnectionFactoryImpl.JCRManagedConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JcrStoreManager extends AbstractStoreManager {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    protected final Logger log = LoggerFactory.getLogger(JcrStoreManager.class);

    public Session session;

    public JcrStoreManager(ClassLoaderResolver clr, OMFContext omfContext) throws RepositoryException {
        super("jcr", clr, omfContext);

        // Handler for persistence process
        persistenceHandler2 = new JcrPersistenceHandler(this);
        logConfiguration();
    }

    public void setSession(Session session) {
        this.session = session;
    }

    
   /* @Override
    public Date getDatastoreDate() {
        // FIXME
        throw new UnsupportedOperationException();
    }*/

    /**
     * Accessor for an Extent for a class.
     * @param om The Object Manager
     * @param c The class requiring the Extent
     * @param subclasses Whether to include subclasses of 'c'
     * @return The Extent.
    public Extent getExtent(final ObjectManager om, final Class c, final boolean subclasses) {
        AbstractClassMetaData cmd = getMetaDataManager().getMetaDataForClass(c, om.getClassLoaderResolver());
        if (!cmd.isRequiresExtent()) {
            throw new NoExtentException(c.getName());
        }
        return new DefaultCandidateExtent(om.getExecutionContext(), c, subclasses, cmd);
    }
     */

    /**
     * Method to return a DataNucleus Connection for the ObjectManager.
     * @param om ObjectManager
     * @return The NucleusConnection
    public NucleusConnection getNucleusConnection(ObjectManager om) {
        ConnectionFactory cf = getOMFContext().getConnectionFactoryRegistry().lookupConnectionFactory(
                txConnectionFactoryName);

        final ManagedConnection mc;
        final boolean enlisted;
        if (!om.getTransaction().isActive()) {
            // no active transaction so don't enlist
            enlisted = false;
        } else {
            enlisted = true;
        }
        mc = cf.getConnection(enlisted ? om : null, null); // Will throw exception if already locked

        // Lock the connection now that it is in use by the user
        mc.lock();

        Runnable closeRunnable = new Runnable() {
            public void run() {
                // Unlock the connection now that the user has finished with it
                mc.unlock();
                if (!enlisted) {
                    // Close the (unenlisted) connection (committing its statements)
                    // TODO Anything to do here to commit with an JCR connection?
                }
            }
        };
        return new NucleusConnectionImpl(mc.getConnection(), closeRunnable);
    }*/
    @Override
    public NucleusConnection getNucleusConnection(ExecutionContext ec)
    {
        ConnectionFactory cf = connectionMgr.lookupConnectionFactory(txConnectionFactoryName);

        final ManagedConnection mc;
        final boolean enlisted;
        if (!ec.getTransaction().isActive())
        {
            // no active transaction so don't enlist
            enlisted = false;
        }
        else
        {
            enlisted = true;
        }
        mc = cf.getConnection(enlisted ? ec : null, enlisted ? ec.getTransaction() : null, null); // Will throw exception if already locked

        // Lock the connection now that it is in use by the user
        mc.lock();

        Runnable closeRunnable = new Runnable()
        {
            public void run()
            {
                // Unlock the connection now that the user has finished with it
                mc.unlock();
                if (!enlisted)
                {
                    // Close the (unenlisted) connection (committing its statements)
                    mc.close();
                }
            }
        };
        return new NucleusConnectionImpl(mc.getConnection(), closeRunnable);
    }

    /*
 
    @Override
    public ConnectionFactory getConnectionFactory() {
        try {
            return new ConnectionFactoryImpl(getOMFContext(), txConnectionFactoryName);
        } catch (RepositoryException ex) {
            // FIXME: log something?
            return null;
        }
    }

    @Override
    public ConnectionFactory getConnectionFactory2() {
        return getConnectionFactory();
    }
    */

    /**
     * Accessor for the supported options in string form
     */
    @Override
    public Collection<String> getSupportedOptions() {
        Set<String> set = new HashSet<String>();
        set.add("DatastoreIdentity"); // TODO: check
        set.add("ApplicationIdentity");
        set.add("TransactionIsolationLevel.read-committed");
        return set;
    }
}
