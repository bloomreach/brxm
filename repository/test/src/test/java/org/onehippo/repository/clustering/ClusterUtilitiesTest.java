/*
 * Copyright 2020-2022 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onehippo.repository.clustering;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;
import java.util.Set;

import javax.jcr.Credentials;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 *     Contains utilities required by independant cluster tests
 * </p>
 */
public abstract class ClusterUtilitiesTest {

    final static Logger log = LoggerFactory.getLogger(ClusterUtilitiesTest.class);

    protected static void closeRepository(final Object repo) {
        try {
            repo.getClass().getMethod("close").invoke(repo);
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            log.error("Failed to close repository: " + e);
        }
    }

    /**
     * Create a LocalHippoRepository running in its own isolated classloader. Because of this classloader
     * isolation all access to the repository internals must be done using reflection.
     */
    protected static Object createRepository(final String repoPath, final String repoConfig) throws Exception {
        final ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        URL[] contextClassLoaderURLs;
        if (contextClassLoader instanceof URLClassLoader) {
            contextClassLoaderURLs = ((URLClassLoader)contextClassLoader).getURLs();
        } else {
            // Java 11 no longer uses URLClassLoader... build URLs from current class path entries
            String[] classPathEntries = System.getProperty("java.class.path").split(System.getProperty("path.separator"));
            contextClassLoaderURLs = new URL[classPathEntries.length];
            for (int i = 0; i < classPathEntries.length; i++) {
                try {
                    if (classPathEntries[i].endsWith(".jar") || classPathEntries[i].endsWith("/")) {
                        contextClassLoaderURLs[i] = new URL("file:"+classPathEntries[i]);
                    } else {
                        // make sure to postfix classpath folders with a /
                        contextClassLoaderURLs[i] = new URL("file:"+classPathEntries[i]+"/");
                    }
                } catch (MalformedURLException unexpected) {
                    // should never happen
                    throw new RuntimeException(unexpected);
                }
            }
        }
        ClassLoader classLoader = new ClusterUtilitiesTest.RepositoryClassLoader(
                contextClassLoaderURLs, contextClassLoader);


        Thread.currentThread().setContextClassLoader(classLoader);
        try {
            return Class.forName("org.hippoecm.repository.LocalHippoRepository", true, classLoader).
                    getMethod("create", String.class, String.class).
                    invoke(null, repoPath, repoConfig);
        } finally {
            Thread.currentThread().setContextClassLoader(contextClassLoader);
        }
    }


    /**
     * Check the index consistency of a repository. All access to the repository must be invoked using
     * reflection because it runs in an isolated classloader.
     */
    protected boolean checkIndexConsistency(Object repo) throws RepositoryException, IOException {
        final Object searchIndex = getSearchIndex(repo);
        final List errors;
        try {
            final Object consistencyCheck = searchIndex.getClass().getMethod("runConsistencyCheck").invoke(searchIndex);
            consistencyCheck.getClass().getMethod("doubleCheckErrors").invoke(consistencyCheck);
            errors = (List) consistencyCheck.getClass().getMethod("getErrors").invoke(consistencyCheck);
            if (errors.isEmpty()) {
                return true;
            } else {
                log.error("Index consistency check failed");
                for (Object error : errors) {
                    log.error("Error: {}", error.toString());
                }
                return false;
            }
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            log.error("Failed to run index consistency check: " + e);
        }
        return false;
    }

    /**
     * Check the database consistency of a repository. All access to the repository must be invoked using
     * reflection because it runs in an isolated classloader.
     */
    protected boolean checkDatabaseConsistency(final Object repo, final Session session) throws RepositoryException {
        Object persistenceManager = getPersistenceManager(repo);
        ClassLoader classLoader = repo.getClass().getClassLoader();
        try {
            final Class<?> checkerClass = Class.forName("org.apache.jackrabbit.core.persistence.bundle.ConsistencyCheckerImpl", true, classLoader);
            final Class<?> pmClass = Class.forName("org.apache.jackrabbit.core.persistence.bundle.AbstractBundlePersistenceManager", true, classLoader);
            final Class<?> listenerClass = Class.forName("org.apache.jackrabbit.core.persistence.check.ConsistencyCheckListener", true, classLoader);
            final Class<?> channelClass = Class.forName("org.apache.jackrabbit.core.cluster.UpdateEventChannel", true, classLoader);
            final Constructor<?> checkerConstructor = checkerClass.getConstructor(pmClass, listenerClass, String.class, channelClass);
            final Object checker = checkerConstructor.newInstance(persistenceManager, null, null, null);
            checkerClass.getMethod("check", String[].class, boolean.class).invoke(checker, new String[] { session.getNode("/test").getIdentifier() }, true);
            // todo: check() with a determinate list of ids seems to be broken, we need to run a double check because of that
            checkerClass.getMethod("doubleCheckErrors").invoke(checker);
            final Object report = checkerClass.getMethod("getReport").invoke(checker);
            Set errors = (Set) report.getClass().getMethod("getItems").invoke(report);
            if (errors.isEmpty()) {
                return true;
            } else {
                log.error("Database consistency check failed");
                for (Object error : errors) {
                    log.error("Error: {}", error.toString());
                }
                return false;
            }
        } catch (ClassNotFoundException | NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
            log.error("Failed to run database consistency check: " + e);
        }
        return false;
    }

    private Object getPersistenceManager(Object repo) throws RepositoryException {
        final Repository repository = getRepository(repo);
        try {
            return repository.getClass().getMethod("getPersistenceManager", String.class).invoke(repository, "default");
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            log.error("Failed to get PersistenceManager from RepositoryImpl: " + e);
        }
        return null;
    }

    private Object getSearchIndex(Object repo) throws RepositoryException {
        final Repository repository = getRepository(repo);
        try {
            return repository.getClass().getMethod("getHippoQueryHandler", String.class).invoke(repository, "default");
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            log.error("Failed to get QueryHandler from RepositoryImpl:" + e);
        }
        return null;
    }

    protected String getTestSystemViewXML(Session session) throws RepositoryException, IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        session.exportSystemView("/test", out, false, false);
        return out.toString();
    }

    private Repository getRepository(Object repo) {
        try {
            Object decorator = repo.getClass().getMethod("getRepository").invoke(repo);
            final Field repository = decorator.getClass().getDeclaredField("repository");
            repository.setAccessible(true);
            return (Repository) repository.get(decorator);
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException | NoSuchFieldException e) {
            log.error("Failed to get internal RepositoryImpl from LocalHippoRepository: " + e);
        }
        return null;
    }

    protected Session loginSession(Object repo) {
        try {
            return (Session) repo.getClass().getMethod("login", Credentials.class).invoke(repo,
                    new SimpleCredentials("admin", "admin".toCharArray()));
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            log.error("Failed to log in session: " + e);
        }
        return null;
    }


    /**
     * ClassLoader to run each repository in isolation to each other. Only the JCR API
     * is shared with the test class so that test cases can use JCR without reflection.
     */
    private static class RepositoryClassLoader extends URLClassLoader {

        private final ClassLoader shared;

        RepositoryClassLoader(final URL[] urls, final ClassLoader shared) {
            super(urls, null);
            this.shared = shared;
        }

        @Override
        public Class<?> loadClass(final String name, boolean resolve) throws ClassNotFoundException {
            if (name.startsWith("javax.jcr") || name.startsWith("java.sql.") || name.startsWith("javax.sql.") ||
                    name.startsWith("org.onehippo.cm.model") || name.startsWith("javax.servlet"))
            {
                return shared.loadClass(name);
            }
            return super.loadClass(name, resolve);
        }
    }
}
