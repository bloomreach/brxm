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
package org.hippoecm.repository.standardworkflow;

import java.rmi.RemoteException;
import java.util.Locale;

import javax.jcr.RepositoryException;

import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.api.Localized;
import org.hippoecm.repository.api.MappingException;
import org.hippoecm.repository.api.WorkflowException;

public interface DefaultWorkflow extends CopyWorkflow {
    final static String SVN_ID = "$Id$";

    public void delete()
      throws WorkflowException, MappingException, RepositoryException, RemoteException;
    public void archive()
      throws WorkflowException, MappingException, RepositoryException, RemoteException;
    public void rename(String newName)
      throws WorkflowException, MappingException, RepositoryException, RemoteException;
    public void localizeName(Localized locale, String newName)
      throws WorkflowException, MappingException, RepositoryException, RemoteException;
    public void localizeName(Locale locale, String newName)
      throws WorkflowException, MappingException, RepositoryException, RemoteException;
    public void localizeName(String newName)
      throws WorkflowException, MappingException, RepositoryException, RemoteException;
    public void move(Document target, String newName)
        throws WorkflowException, MappingException, RepositoryException, RemoteException;
}
