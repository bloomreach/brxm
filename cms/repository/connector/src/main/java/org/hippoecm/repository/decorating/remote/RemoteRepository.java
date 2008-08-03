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
package org.hippoecm.repository.decorating.remote;

import java.io.Serializable;
import java.rmi.Remote;
import java.rmi.RemoteException;

import javax.jcr.RepositoryException;

import org.apache.jackrabbit.spi.rmi.remote.RemoteRepositoryService;

public interface RemoteRepository extends org.apache.jackrabbit.rmi.remote.RemoteRepository, Remote, Serializable {
    final static String SVN_ID = "$Id$";

    public RemoteRepositoryService getRepositoryService() throws RepositoryException, RemoteException;
}
