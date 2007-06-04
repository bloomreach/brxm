/*
 * Copyright 2007 Hippo
 *
 * Licensed under the Apache License, Version 2.0 (the  "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippocms.repository.jr.embedded;

import java.io.IOException;
import java.net.MalformedURLException;
import java.rmi.AlreadyBoundException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;

import javax.jcr.Repository;
import javax.jcr.RepositoryException;

public class HippoRepositoryFactory {
    private final static String SVN = "$Id$";

    static String defaultLocation = null;
    static HippoRepository defaultRepository = null;

    public static void setDefaultRepository(String location) {
        defaultLocation = location;
        defaultRepository = null;
    }

    public static void setDefaultRepository(HippoRepository repository) {
        defaultLocation = null;
        defaultRepository = repository;
    }

    public HippoRepositoryFactory() {
    }

    public static HippoRepository getHippoRepository() throws RepositoryException {
        if (defaultRepository != null) {
            return defaultRepository;
        } else if (defaultLocation != null) {
            defaultRepository = getHippoRepository(defaultLocation);
            return defaultRepository;
        }
        return new LocalHippoRepository();
    }

    public static HippoRepository getHippoRepository(String location) throws RepositoryException {
        if (defaultRepository != null
                && (location.equals(defaultRepository.getLocation()) || location.equals("file:"
                        + defaultRepository.getLocation())))
            return defaultRepository;
        else if (defaultRepository == null
                && (location.equals(defaultLocation) || location.equals("file:" + defaultLocation)))
            return getHippoRepository();
        if (location.startsWith("rmi://")) {
            try {
                return new RemoteHippoRepository(location);
            } catch (RemoteException ex) {
                return null;
                // FIXME
            } catch (NotBoundException ex) {
                return null;
                // FIXME
            } catch (MalformedURLException ex) {
                return null;
                // FIXME
            }
        } else if (location.startsWith("file:")) {
            System.out.println("HERE " + location.substring(5));
            return new LocalHippoRepository(location.substring(5));
        } else
            return new LocalHippoRepository(location);
    }
}
