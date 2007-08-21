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
package org.hippoecm.repository;

import java.net.MalformedURLException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import javax.jcr.RepositoryException;

public class HippoRepositoryFactory {
    private final static String SVN_ID = "$Id$";

    static String defaultLocation = null; // FIXME: should become: "java:comp/env/jcr/repository";
    static HippoRepository defaultRepository = null;

    public static void setDefaultRepository(String location) {
        defaultLocation = location;
        defaultRepository = null;
    }

    public static void setDefaultRepository(HippoRepository repository) {
        defaultLocation = null;
        defaultRepository = repository;
    }

    private HippoRepositoryFactory() {
    }

    public static HippoRepository getHippoRepository() throws RepositoryException {
        if (defaultRepository != null) {
            return defaultRepository;
        } 
        if (defaultLocation != null) {
            defaultRepository = getHippoRepository(defaultLocation);
            return defaultRepository;
        }
        return new LocalHippoRepository();
    }

    public static HippoRepository getHippoRepository(String location) throws RepositoryException {
        // strip
        if (location.startsWith("file:")) {
            location = location.substring(5);
        }
        
        // already configured?
        if (defaultRepository != null && location.equals(defaultRepository.getLocation())) {
            return defaultRepository;
        } 
        
        // remote?
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
        
        // embedded/local default
        if (defaultRepository == null && location.equals(defaultLocation)) {
            return getHippoRepository();
        }
        
        // embedded/local with location
        return new LocalHippoRepository(location);
    }
}
