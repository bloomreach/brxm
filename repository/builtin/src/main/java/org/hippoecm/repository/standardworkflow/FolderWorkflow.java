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

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.rmi.RemoteException;

import javax.jcr.RepositoryException;

import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.api.Localized;
import org.hippoecm.repository.api.MappingException;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowException;

/**
 * FolderWorkflow API
 * This interface defines the
 * FIXME: complete javadoc and add example usage
 *
 */
public interface FolderWorkflow extends Workflow {
    static final String SVN_ID = "$Id$";

    /**
     *
     * @deprecated
     */
    public Map<String,Set<String>> list()
      throws WorkflowException, MappingException, RepositoryException, RemoteException;

    /**
     *
     * @param category
     * @param type
     * @param relPath the relative path from the parent folder (including the index number)
     * @return
     * @throws WorkflowException
     * @throws MappingException
     * @throws RepositoryException
     * @throws RemoteException
     */
    public String add(String category, String type, String relPath)
      throws WorkflowException, MappingException, RepositoryException, RemoteException;

    /**
     * FIXME: document the argument map
     * @param category
     * @param type
     * @param arguments
     * @return
     * @throws WorkflowException
     * @throws MappingException
     * @throws RepositoryException
     * @throws RemoteException
     */
    public String add(String category, String type, Map<String,String> arguments)
      throws WorkflowException, MappingException, RepositoryException, RemoteException;

    /**
     *
     * @param relPath the relative path from the parent folder (including the index number)
     * @throws WorkflowException
     * @throws MappingException
     * @throws RepositoryException
     * @throws RemoteException
     */
    public void archive(String relPath)
      throws WorkflowException, MappingException, RepositoryException, RemoteException;

    /**
     *
     * @param offspring
     * @throws WorkflowException
     * @throws MappingException
     * @throws RepositoryException
     * @throws RemoteException
     */
    public void archive(Document offspring)
      throws WorkflowException, MappingException, RepositoryException, RemoteException;

    /**
     *
     * @param relPath the relative path from the parent folder (including the index number)
     * @throws WorkflowException
     * @throws MappingException
     * @throws RepositoryException
     * @throws RemoteException
     */
    public void delete(String relPath)
      throws WorkflowException, MappingException, RepositoryException, RemoteException;

    /**
     *
     * @param offspring
     * @throws WorkflowException
     * @throws MappingException
     * @throws RepositoryException
     * @throws RemoteException
     */
    public void delete(Document offspring)
      throws WorkflowException, MappingException, RepositoryException, RemoteException;

    /**
     *
     * @param relPath the relative path from the parent folder (including the index number)
     * @param newName the new name excluding the index number
     * @throws WorkflowException
     * @throws MappingException
     * @throws RepositoryException
     * @throws RemoteException
     */
    public void rename(String relPath, String newName)
      throws WorkflowException, MappingException, RepositoryException, RemoteException;

    /**
     *
     * @param offspring
     * @param newName the new name excluding the index number
     * @throws WorkflowException
     * @throws MappingException
     * @throws RepositoryException
     * @throws RemoteException
     */
    public void rename(Document offspring, String newName)
      throws WorkflowException, MappingException, RepositoryException, RemoteException;

    /**
     *
     * @param newOrder
     * @throws WorkflowException
     * @throws MappingException
     * @throws RepositoryException
     * @throws RemoteException
     */
    public void reorder(List<String> newOrder)
        throws WorkflowException, MappingException, RepositoryException, RemoteException;

    public Document duplicate(String relPath)
      throws WorkflowException, MappingException, RepositoryException, RemoteException;
    public Document duplicate(Document offspring)
      throws WorkflowException, MappingException, RepositoryException, RemoteException;
    public Document duplicate(String relPath, Map<String,String> arguments)
      throws WorkflowException, MappingException, RepositoryException, RemoteException;
    public Document duplicate(Document offspring, Map<String,String> arguments)
      throws WorkflowException, MappingException, RepositoryException, RemoteException;

    public Document copy(String relPath, String absPath)
      throws WorkflowException, MappingException, RepositoryException, RemoteException;
    public Document copy(Document offspring, Document target, String name)
      throws WorkflowException, MappingException, RepositoryException, RemoteException;
    public Document copy(String relPath, String absPath, Map<String,String> arguments)
      throws WorkflowException, MappingException, RepositoryException, RemoteException;
    public Document copy(Document offspring, Document target, String name, Map<String,String> arguments)
      throws WorkflowException, MappingException, RepositoryException, RemoteException;

    public Document move(String relPath, String absPath)
      throws WorkflowException, MappingException, RepositoryException, RemoteException;
    public Document move(Document offspring, Document target, String name)
      throws WorkflowException, MappingException, RepositoryException, RemoteException;
    public Document move(String relPath, String absPath, Map<String,String> arguments)
      throws WorkflowException, MappingException, RepositoryException, RemoteException;
    public Document move(Document offspring, Document target, String name, Map<String,String> arguments)
      throws WorkflowException, MappingException, RepositoryException, RemoteException;

}
