/*
 *  Copyright 2008-2014 Hippo B.V. (http://www.onehippo.com)
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
import java.util.List;
import java.util.Map;

import javax.jcr.RepositoryException;

import org.apache.jackrabbit.rmi.remote.RemoteNode;
import org.hippoecm.repository.api.Localized;

public interface RemoteServicingNode extends RemoteNode, Remote, Serializable {

    RemoteNode getCanonicalNode() throws RepositoryException, RemoteException;

    String getLocalizedName(Localized localized) throws RepositoryException, RemoteException;

    Map<Localized, String> getLocalizedNames() throws RepositoryException, RemoteException;

    boolean recomputeDerivedData() throws RepositoryException, RemoteException;
}
