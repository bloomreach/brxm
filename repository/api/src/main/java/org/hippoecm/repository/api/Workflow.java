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
package org.hippoecm.repository.api;

import java.io.Serializable;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Map;

import javax.jcr.RepositoryException;

/**
 * Implementors of this interface should never return subclasses of the
 * #Document class in their interface.  It is allowed to return an instance of
 * a subclass of a #Document, but the repository will force recreating the
 * object returned as a direct instance of an #Document.
 */
public interface Workflow extends Remote, Serializable {
    final static String SVN_ID = "$Id$";

    public Map<String,Serializable> hints() throws WorkflowException, RemoteException, RepositoryException;
}
