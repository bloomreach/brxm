/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.jackrabbit.jcr2spi;

import java.util.Enumeration;
import java.util.Map;

import javax.jcr.Credentials;
import javax.jcr.LoginException;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.naming.NamingException;
import javax.naming.RefAddr;
import javax.naming.Reference;
import javax.naming.Referenceable;
import javax.naming.StringRefAddr;

import org.apache.jackrabbit.commons.AbstractRepository;
import org.apache.jackrabbit.jcr2spi.config.RepositoryConfig;
import org.apache.jackrabbit.spi.SessionInfo;
import org.apache.jackrabbit.spi.XASessionInfo;

/**
 * <code>HippoRepositoryImpl</code>...
 */
public class HippoRepositoryImpl extends AbstractRepository implements Referenceable {

    // configuration of the repository
    private final RepositoryConfig config;
    private final Map descriptors;
    private Reference reference = null;

    private HippoRepositoryImpl(RepositoryConfig config) throws RepositoryException {
        this.config = config;
        descriptors = config.getRepositoryService().getRepositoryDescriptors();
    }

    public static Repository create(RepositoryConfig config) throws RepositoryException {
        return new HippoRepositoryImpl(config);
    }

    //---------------------------------------------------------< Repository >---
    /**
     * @see Repository#getDescriptorKeys()
     */
    public String[] getDescriptorKeys() {
        String[] keys = (String[]) descriptors.keySet().toArray(new String[descriptors.keySet().size()]);
        return keys;
    }

    /**
     * @see Repository#getDescriptor(String)
     */
    public String getDescriptor(String descriptorKey) {
        return (String) descriptors.get(descriptorKey);
    }

    /**
     * @see Repository#login(javax.jcr.Credentials, String)
     */
    public Session login(Credentials credentials, String workspaceName) throws LoginException, NoSuchWorkspaceException, RepositoryException {
        SessionInfo info = config.getRepositoryService().obtain(credentials, workspaceName);
        try {
            if (info instanceof XASessionInfo) {
                return new HippoXASessionImpl((XASessionInfo) info, this, config);
            } else {
                return new HippoSessionImpl(info, this, config);
            }
        } catch (RepositoryException ex) {
            config.getRepositoryService().dispose(info);
            throw ex;
        }
    }

    //---------------------------------------------------------< Rereferencable >---

    /**
     * @see Referenceable#getReference()
     */
    public Reference getReference() throws NamingException {
        if (config instanceof Referenceable) {
            Referenceable confref = (Referenceable)config;
            if (reference == null) {
                reference = new Reference(RepositoryImpl.class.getName(), RepositoryImpl.Factory.class.getName(), null);
                // carry over all addresses from referenceable config
                for (Enumeration en = confref.getReference().getAll(); en.hasMoreElements(); ) {
                    reference.add((RefAddr)(en.nextElement()));
                }

                // also add the information required by factory class
                reference.add(new StringRefAddr(RepositoryImpl.Factory.RCF, confref.getReference().getFactoryClassName()));
                reference.add(new StringRefAddr(RepositoryImpl.Factory.RCC, config.getClass().getName()));
            }

            return reference;
        }
        else {
            throw new javax.naming.OperationNotSupportedException("Contained RepositoryConfig needs to implement javax.naming.Referenceable");
        }
    }
}
