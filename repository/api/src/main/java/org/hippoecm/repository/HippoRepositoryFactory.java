/*
 *  Copyright 2008 Hippo.
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
import java.net.URL;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import javax.jcr.RepositoryException;

/**
 * The HippoRepositoryFactory class is a factory class for obtaining a reference to a Hippo Repository connection.
 * Typical usage is:
 * <pre>
 * HippoRepository repository = HippoRepositoryFactory.getHippoRepository();
 * </pre>
 * Which should return the default repository, configured from an external source using the default transport mechanism.
 * 
 * If you need to contact a specific instance of a repository, you can use:
 * <pre>
 * HippoRepository repository = HippoRepository.getHippoRepository(url);
 * </pre>
 * Where the url parameter is a string containing the location or address of the repository.  This url can take different
 * forms to indicate the location (filesystem or hostname) and the transportation mechanism to use.  Typically you
 * would use <code>rmi://hostname/hipporepository</code> to contact the indicated hostname using RMI transport mechanism
 * where a named service hipporepository is the Hippo repository.  A more efficient version of the RMI transport mechanism can
 * be used by appending <code>/spi</code> to this RMI based protocol address.  To start a new local repository, you can
 * use the location string <code>file:/absolute/filesystem/path</code>.
 * *
 * The returned Repository is a JCR-based repository, but is not a direct instance of the class javax.jcr.Repository to
 * allow some additional operations.
 * 
 * @author (Berry) A.W. van Halderen
 */
public class HippoRepositoryFactory {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static String defaultLocation = null; // FIXME: should become: "java:comp/env/jcr/repository";
    private static HippoRepository defaultRepository = null;

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

    private HippoRepositoryFactory() {
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
            defaultRepository = (HippoRepository) Class.forName("org.hippoecm.repository.LocalHippoRepository").newInstance();
        } catch(ClassNotFoundException ex) {
            throw new RepositoryException(ex);
        } catch(InstantiationException ex) {
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

        if (location.startsWith("file:")) {
            location = location.substring("file:".length());
        }

        if (defaultRepository != null && (location.equals(defaultRepository.getLocation()) ||
                                          (defaultLocation != null && location.equals(defaultLocation)))) {
            return defaultRepository;
        }

        if (location.startsWith("rmi://")) {
            try {
                defaultLocation = location;
                try {
                    if(!location.endsWith("/spi")) {
                        return (HippoRepository) Class.forName("org.hippoecm.repository.RemoteHippoRepository").getMethod("create", new Class[] { String.class }).invoke(null, new Object[] { location });
                    } else {
                        return (HippoRepository) Class.forName("org.hippoecm.repository.SPIHippoRepository").getMethod("create", new Class[] { String.class }).invoke(null, new Object[] { location });
                    }
                } catch(ClassNotFoundException ex) {
                    throw new RepositoryException(ex);
                } catch(NoSuchMethodException ex) {
                    throw new RepositoryException(ex);
                } catch(IllegalAccessException ex) {
                    throw new RepositoryException(ex);
                } catch(InvocationTargetException ex) {
                    if(ex.getCause() instanceof RemoteException) {
                        throw (RemoteException) ex.getCause();
                    } else if(ex.getCause() instanceof NotBoundException) {
                        throw (NotBoundException) ex.getCause();
                    } else if(ex.getCause() instanceof MalformedURLException) {
                        throw (MalformedURLException) ex.getCause();
                    } else if(ex.getCause() instanceof RepositoryException) {
                        throw (RepositoryException) ex.getCause();
                    } else {
                        throw new RepositoryException("unchecked exception: "+ex.getCause().getMessage(), ex);
                    }
                }
            } catch (RemoteException ex) {
                throw new RepositoryException("Unable to connect to repository", ex);
            } catch (NotBoundException ex) {
                throw new RepositoryException("Unable to find remote repository", ex);
            } catch (MalformedURLException ex) {
                throw new RepositoryException("Unable to locate remote repository", ex);
            }
        }

        if(location.startsWith("java:")) {
            try {
                defaultLocation = location;
                InitialContext ctx = new InitialContext();
                return (HippoRepository) ctx.lookup(location);
            } catch (NamingException ex) {
                return null;
                // FIXME
            }
        }

        if(location.startsWith("bootstrap:")) {
            try {
                defaultLocation = location;
                location = location.substring("bootstrap:".length());
                return (HippoRepository) Class.forName("org.hippoecm.repository.BootstrapHippoRepository").getMethod("create", new Class[] { String.class }).invoke(null, new Object[] { location });
            } catch(ClassNotFoundException ex) {
                throw new RepositoryException(ex);
            } catch(NoSuchMethodException ex) {
                throw new RepositoryException(ex);
            } catch(IllegalAccessException ex) {
                throw new RepositoryException(ex);
            } catch(InvocationTargetException ex) {
                if (ex.getCause() instanceof RepositoryException) {
                    throw (RepositoryException) ex.getCause();
                } else if (ex.getCause() instanceof IllegalArgumentException) {
                    throw new RepositoryException("Invalid data: " + ex.getCause());
                } else {
                    throw new RepositoryException("unchecked exception: " + ex.getMessage());
                }
            }
        }

        if(location.startsWith("vm:")) {
            try {
                defaultLocation = location;
                return (HippoRepository) Class.forName("org.hippoecm.repository.VMHippoRepository").getMethod("create", new Class[] { String.class }).invoke(null, new Object[] { location });
            } catch(ClassNotFoundException ex) {
                throw new RepositoryException(ex);
            } catch(NoSuchMethodException ex) {
                throw new RepositoryException(ex);
            } catch(IllegalAccessException ex) {
                throw new RepositoryException(ex);
            } catch(InvocationTargetException ex) {
                if (ex.getCause() instanceof RepositoryException) {
                    throw (RepositoryException) ex.getCause();
                } else if (ex.getCause() instanceof IllegalArgumentException) {
                    throw new RepositoryException("Invalid data: " + ex.getCause());
                } else {
                    throw new RepositoryException("unchecked exception: " + ex.getClass().getName() + ": " + ex.getMessage(), ex);
                }
            }
        }

         if(location.startsWith("proxy:")) {
            try {
                return (HippoRepository) Class.forName("org.hippoecm.repository.proxyrepository.ProxyHippoRepository").getMethod("create", new Class[] { String.class }).invoke(null, new Object[] { location });
            } catch(ClassNotFoundException ex) {
                throw new RepositoryException(ex);
            } catch(NoSuchMethodException ex) {
                throw new RepositoryException(ex);
            } catch(IllegalAccessException ex) {
                throw new RepositoryException(ex);
            } catch(InvocationTargetException ex) {
                if (ex.getCause() instanceof RepositoryException) {
                    throw (RepositoryException) ex.getCause();
                } else if (ex.getCause() instanceof IllegalArgumentException) {
                    throw new RepositoryException("Invalid data: " + ex.getCause());
                } else {
                    throw new RepositoryException("unchecked exception: " + ex.getClass().getName() + ": " + ex.getMessage(), ex);
                }
            }
        }

        // embedded/local with location
        try {
            repository = (HippoRepository) Class.forName("org.hippoecm.repository.LocalHippoRepository").getMethod("create", new Class[] { String.class }).invoke(null, new Object[] { location });
        } catch(ClassNotFoundException ex) {
            throw new RepositoryException(ex);
        } catch(NoSuchMethodException ex) {
            throw new RepositoryException(ex);
        } catch(IllegalAccessException ex) {
            throw new RepositoryException(ex);
        } catch(InvocationTargetException ex) {
            if (ex.getCause() instanceof RepositoryException) {
                throw (RepositoryException) ex.getCause();
            } else if (ex.getCause() instanceof IllegalArgumentException) {
                throw new RepositoryException("Invalid data: " + ex.getCause());
            } else {
                throw new RepositoryException("unchecked exception: " + ex.getClass().getName() + ": " + ex.getMessage(), ex);
            }
        }

        // in case this local repository is build from the default location
        if (defaultRepository == null && location.equals(defaultLocation)) {
            defaultRepository = repository;
        }

        return repository;
    }

    static void unregister(HippoRepository repository) {
        if (repository == defaultRepository) {
            defaultRepository = null;
        }
    }

    public static URL getManifest(Class clazz) {
        try {
            StringBuffer sb = new StringBuffer();
            String[] classElements = clazz.getName().split("\\.");
            for (int i=0; i<classElements.length-1; i++) {
                sb.append("../");
            }
            sb.append("META-INF/MANIFEST.MF");
            URL classResource = clazz.getResource(classElements[classElements.length-1]+".class");
            if (classResource != null) {
                return new URL(classResource, new String(sb));
            }
        } catch (MalformedURLException ex) {
            // ignore
        }
        return null;
    }
}
