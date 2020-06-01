/*
 *  Copyright 2009-2017 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.repository;

import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;

import javax.jcr.RepositoryException;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.hippoecm.repository.util.RepoUtils;

public class HippoRepositoryFactory {

    private static String defaultLocation = null; // FIXME: should become: "java:comp/env/jcr/repository";
    private static HippoRepository defaultRepository = null;
    private static Class localRepositoryClass = null;

    private static Class getLocalHippoRepositoryClass() throws ClassNotFoundException {
        if (localRepositoryClass == null) {
            try {
                localRepositoryClass = Class.forName("com.onehippo.repository.HippoEnterpriseRepository");
            } catch (ClassNotFoundException e) {
                localRepositoryClass = Class.forName("org.hippoecm.repository.LocalHippoRepository");
            }
        }
        return localRepositoryClass;
    }

    private HippoRepositoryFactory() {
    }

    /**
     * Sets the default location (url) to use when obtaining a repository through subsequent static getHippoRepository()
     * method calls.
     * @param location the new location to use as default location for future calls to getHippoRepository()
     */
    public static void setDefaultRepository(String location) {
        if(location == null || !location.equals(defaultLocation)) {
            defaultLocation = location;
            defaultRepository = null;
        }
    }

    /**
     * Sets the default repository to return when obtaining a repository through subsequent calls to the static
     * getHippoRepository() method.
     * @param repository the repository to be returned for getHippoRepository() method calls
     */
    public static void setDefaultRepository(HippoRepository repository) {
        defaultLocation = null;
        defaultRepository = repository;
    }

    /**
     * Obtains a new connection or instance (depending on protocol used) to the default HippoRepository.
     * @return the current default repository
     * @throws javax.jcr.RepositoryException
     */
    public static HippoRepository getHippoRepository() throws RepositoryException {
        if (defaultRepository != null) {
            return defaultRepository;
        }
        if (defaultLocation != null) {
            return getHippoRepository(defaultLocation);
        }

        try {
            defaultRepository = (HippoRepository) getLocalHippoRepositoryClass().getMethod("create", new Class[] { String.class } ).invoke(null, (String)null);
        } catch(NoSuchMethodException ex) {
            throw new RepositoryException(ex);
        } catch(InvocationTargetException ex) {
            throw new RepositoryException(ex);
        } catch(ClassNotFoundException ex) {
            throw new RepositoryException(ex);
        } catch(IllegalAccessException ex) {
            throw new RepositoryException(ex);
        }
        return defaultRepository;
    }

    /**
     * Obtains a new connection or instance (depending on protocol used) for a specific indicated repository.
     * @param location the specific location to use for the repository
     * @return a connection or instance to the indicated HippoRepository
     * @throws javax.jcr.RepositoryException
     */
    public static HippoRepository getHippoRepository(String location) throws RepositoryException {
        HippoRepository repository = null;

        location = RepoUtils.stripFileProtocol(location);

        if (location.startsWith("rmi://")) {
            try {
                if(!location.endsWith("/spi")) {
                    return RemoteHippoRepository.create(location);
                } else {
                    throw new RepositoryException("Remote SPI not supported at the moment");
                }
            } catch (RemoteException ex) {
                throw new RepositoryException("Unable to connect to repository", ex);
            } catch (NotBoundException ex) {
                throw new RepositoryException("Unable to find remote repository", ex);
            } catch (MalformedURLException ex) {
                throw new RepositoryException("Unable to locate remote repository", ex);
            }
        }

        if (defaultRepository != null && (location.equals(defaultRepository.getLocation()) ||
                                          (defaultLocation != null && location.equals(defaultLocation)))) {
            return defaultRepository;
        }


        if(location.startsWith("java:")) {
            try {
                InitialContext ctx = new InitialContext();
                return (HippoRepository) ctx.lookup(location);
            } catch (NamingException ex) {
                return null;
                // FIXME
            }
        }

        if(location.startsWith("vm:")) {
            return VMHippoRepository.create(location);
        }

        // embedded/local with location
        try {
            repository = (HippoRepository) getLocalHippoRepositoryClass().getMethod("create", new Class[] { String.class }).invoke(null, new Object[] { location });
        } catch(ClassNotFoundException ex) {
            throw new RepositoryException(ex);
        } catch(NoSuchMethodException ex) {
            throw new RepositoryException(ex);
        } catch(IllegalAccessException ex) {
            throw new RepositoryException(ex);
        } catch(InvocationTargetException ex) {
            final Throwable cause = unwindInvocationTargetException(ex);
            if (cause instanceof RepositoryException) {
                throw (RepositoryException) cause;
            } else if (cause instanceof IllegalArgumentException) {
                throw new RepositoryException("Invalid data: " + cause, cause);
            } else {
                throw new RepositoryException("unchecked exception: " + cause.getClass().getName() + ": " + cause.getMessage(), cause);
            }
        }

        return repository;
    }

    private static Throwable unwindInvocationTargetException(final InvocationTargetException e) {
        Throwable cause = e.getCause();
        if (cause != null && cause instanceof InvocationTargetException) {
            return unwindInvocationTargetException((InvocationTargetException)cause);
        }
        return cause == null ? e : cause;
    }

    static void unregister(HippoRepository repository) {
        if (repository == defaultRepository) {
            defaultRepository = null;
        }
    }
}
