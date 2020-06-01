/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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
import org.hippoecm.repository.api.MappingException;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowException;
import org.onehippo.repository.api.annotation.WorkflowAction;

/**
 * Work-flow interface that is generally available on container documents.  Container documents are documents that contain
 * a set of other documents and are most of the time themselves content-free.  Two default container documents are folders
 * and directories, which are mostly the same except for the fact that the entries in directories do not have an ordering, while
 * entries in a folder are ordered.  The work-flow interface FolderWorkflow is returned by methods in the WorkflowManager when
 * passed a container document as argument.
 *
 * The arguments map that can be passed to add, copy and move operations allow you perform rewrite rules upon the document being
 * handled.  For each relative path specified as key in the map, if defines the new value to be used instead of the original
 * value in the prototype or source document that is being copied or moved.  Relative paths may start with <code>./</code> and
 * the pseudo names _name and _node refer to the name of the node actually being copied and _node to any child node.  To set a
 * property x to any document variant of a node being copied, you would use the relative path <code>./_name/_node/x</code>.
 * The new value of the property --or in case it refers to a node, the new node name-- becomes the value in the map.  There
 * are a number of symbolic values that are allowed in the map, which are:
 * <ul>
 * <li><code>$inherit</code> the value under the same path in the parent document</li>
 * <li><code>$now</code> substitutes with the current datetime</li>
 * <li><code>$holder</code> replaced by the user id of the original user invoking the workflow</li>
 * <li><code>$uuid</code> a uniquely generated UUID</li>
 * </ul>
 * 
 * Note that the add work-flow methods return an absolute path to a document handle (a JCR node of type hippo:handle), while
 * a org.hippoecm.repository.api.Document would have been more appropriate.
 */
public interface FolderWorkflow extends Workflow {

    /**
     * Returns the possible set of documents that may normally be contained within this container document.  Only those
     * document prototypes may be added through a call #FolderWorkflow.add().
     * @return a set of possible document types that can be created directly below this container document.  The possible
     * options are sub-classed into configurable categories.  Common categorization is to have container document types (e.g. a
     * folder or directory) to be one category, while (namespace) specific documents to be another type.
     * @throws WorkflowException indicates that the work-flow call failed due work-flow specific conditions
     * @throws MappingException indicates that the work-flow call failed because of configuration problems
     * @throws RepositoryException  indicates that the work-flow call failed because of storage problems internal to the repository
     * @throws RemoteException indicates that the work-flow call failed because of a connection problem with the repository
     * @deprecated use FolderWorkflow.hints().get("prototypes") instead which can be cast into the same return type.
     */
    @WorkflowAction(loggable = false)
    public Map<String,Set<String>> list()
      throws WorkflowException, MappingException, RepositoryException, RemoteException;

    /**
     * Adds a new document to this container document from the specified category of the indicated type.  The category and type
     * correspond to the key and value pair in the map returned by the hints.get("prototypes") which is a map of String to String.
     * @param category the category of document type to create (e.g. <code>new-folder</code> or <code>new-document</code>)
     * @param type the actual document type within the category (e.g. <code>directory</code> or <code>mynamespace:mydocument</code>)
     * @param relPath the relative path from the parent folder that is to be the new name of the document
     * @return a absolute path to the created document handle
     * @throws WorkflowException indicates that the work-flow call failed due work-flow specific conditions
     * @throws MappingException indicates that the work-flow call failed because of configuration problems
     * @throws RepositoryException  indicates that the work-flow call failed because of storage problems internal to the repository
     * @throws RemoteException indicates that the work-flow call failed because of a connection problem with the repository
     */
    public String add(String category, String type, String relPath)
      throws WorkflowException, MappingException, RepositoryException, RemoteException;

    /**
     * Adds a new document to this container document from the specified category of the indicated type.  This call is similar
     * to #add(String,String,relPath) but instead of specifying a new name, a list or rewrite rules to be applied (which include
     * the new name of the document) is provided.  This allows you to set the new name (use <code>./_name</code> and
     * <code>./_node/_name</code> for regular documents and just <code>./_name</code> as keys in the argument map to set the name),
     * as well as any other properties that should be overwritten in the created document, such as current date and time.
     * @param category the category of document type to create
     * @param type the actual document type within the category
     * @param arguments the rewrite rule to be applied
     * @return the absolute JCR path to the created document 
     * @throws WorkflowException  indicates that the work-flow call failed due work-flow specific conditions
     * @throws MappingException indicates that the work-flow call failed because of configuration problems
     * @throws RepositoryException  indicates that the work-flow call failed because of storage problems internal to the repository
     * @throws RemoteException indicates that the work-flow call failed because of a connection problem with the repository
     */
    public String add(String category, String type, Map<String,String> arguments)
      throws WorkflowException, MappingException, RepositoryException, RemoteException;

    /**
     * Archives the document indicated by the argument.  This effectively deletes the document for usage within the CMS, web-site
     * and any other location, except that the history of the document if available is retained.  This way be document can be
     * un-deleted and may still be inspected though the history.  The actual achiving method is implementation dependent, and
     * may be moving the document to a special location, and/or deleting all variants or other implementation.
     * @param relPath the relative path from the parent folder (including the index number)
     * @throws WorkflowException  indicates that the work-flow call failed due work-flow specific conditions
     * @throws MappingException indicates that the work-flow call failed because of configuration problems
     * @throws RepositoryException  indicates that the work-flow call failed because of storage problems internal to the repository
     * @throws RemoteException indicates that the work-flow call failed because of a connection problem with the repository
     */
    public void archive(String relPath)
      throws WorkflowException, MappingException, RepositoryException, RemoteException;

    /**
     * Archives the document indicated by the argument.  This effectively deletes the document for usage within the CMS, web-site
     * and any other location, except that the history of the document if available is retained.  This way be document can be
     * un-deleted and may still be inspected though the history.  The actual achiving method is implementation dependent, and
     * may be moving the document to a special location, and/or deleting all variants or other implementation.
     * @param offspring the document object which should be archived and must be located directly as descendant
     * @throws WorkflowException  indicates that the work-flow call failed due work-flow specific conditions
     * @throws MappingException indicates that the work-flow call failed because of configuration problems
     * @throws RepositoryException  indicates that the work-flow call failed because of storage problems internal to the repository
     * @throws RemoteException indicates that the work-flow call failed because of a connection problem with the repository
     */
    public void archive(Document offspring)
      throws WorkflowException, MappingException, RepositoryException, RemoteException;

    /**
     * Permanently removes the document indicated by the argument from this container document.  Unlike the #archive method, this
     * makes it impossible to resurrect the document from version history, even though the document may occupy resources in the
     * version history.
     * @param relPath the relative path from the parent folder (including the index number)
     * @throws WorkflowException  indicates that the work-flow call failed due work-flow specific conditions
     * @throws MappingException indicates that the work-flow call failed because of configuration problems
     * @throws RepositoryException  indicates that the work-flow call failed because of storage problems internal to the repository
     * @throws RemoteException indicates that the work-flow call failed because of a connection problem with the repository
     */
    public void delete(String relPath)
      throws WorkflowException, MappingException, RepositoryException, RemoteException;

    /**
     * Permanently removes the document indicated by the argument from this container document.  Unlike the #archive method, this
     * makes it impossible to resurrect the document from version history, even though the document may occupy resources in the
     * version history.
     * @param offspring the document object which should be archived and must be located directly as descendant
     * @throws WorkflowException  indicates that the work-flow call failed due work-flow specific conditions
     * @throws MappingException indicates that the work-flow call failed because of configuration problems
     * @throws RepositoryException  indicates that the work-flow call failed because of storage problems internal to the repository
     * @throws RemoteException indicates that the work-flow call failed because of a connection problem with the repository
     */
    public void delete(Document offspring)
      throws WorkflowException, MappingException, RepositoryException, RemoteException;

    /**
     * Renames a document contained in this container document.  The document should be a direct descendant in this container
     * document, not in a sub-folder.  The rename also cannot move a document to another folder, use the #move() operation for
     * this.
     * @param relPath the relative path from the parent folder (including the index number)
     * @param newName the new name excluding the index number
     * @throws WorkflowException  indicates that the work-flow call failed due work-flow specific conditions
     * @throws MappingException indicates that the work-flow call failed because of configuration problems
     * @throws RepositoryException  indicates that the work-flow call failed because of storage problems internal to the repository
     * @throws RemoteException indicates that the work-flow call failed because of a connection problem with the repository
     */
    public void rename(String relPath, String newName)
      throws WorkflowException, MappingException, RepositoryException, RemoteException;

    /**
     * Renames a document contained in this container document.  The document should be a direct descendant in this container
     * document, not in a sub-folder.  The rename also cannot move a document to another folder, use the #move() operation for
     * this.
     * @param offspring the document object which should be renamed and should be located directly as descendant
     * @param newName the new name excluding the index number
     * @throws WorkflowException  indicates that the work-flow call failed due work-flow specific conditions
     * @throws MappingException indicates that the work-flow call failed because of configuration problems
     * @throws RepositoryException  indicates that the work-flow call failed because of storage problems internal to the repository
     * @throws RemoteException indicates that the work-flow call failed because of a connection problem with the repository
     */
    public void rename(Document offspring, String newName)
      throws WorkflowException, MappingException, RepositoryException, RemoteException;

    /**
     * Reorders the documents contained
     * @param newOrder An ordered list of relative names of the direct siblings within this folder, including any indices (e.g.
     * {"first", "next", "next[2]" }) indicating the order in which they should appear after this call.  The ordering of the other
     * items not included in the newOrder list is undefined, but they are not removed
     * @throws WorkflowException  indicates that the work-flow call failed due work-flow specific conditions, such as ordering is
     * not allowed on this container document
     * @throws MappingException indicates that the work-flow call failed because of configuration problems
     * @throws RepositoryException  indicates that the work-flow call failed because of storage problems internal to the repository
     * @throws RemoteException indicates that the work-flow call failed because of a connection problem with the repository
     */
    public void reorder(List<String> newOrder)
        throws WorkflowException, MappingException, RepositoryException, RemoteException;

    /**
     * Duplicates a document contained directly in this folder.  This creates a copy of the document in the same folder of the
     * indicated document, but with unique identification.  Creating a duplicate is a distinct operation from copy operation
     * (creating a copy of a document in a different folder) because different rules may apply.
     * @param relPath the relative path to the document to be duplicated
     * @return the document reference of the created copy of the document
     * @throws WorkflowException  indicates that the work-flow call failed due work-flow specific conditions
     * @throws MappingException indicates that the work-flow call failed because of configuration problems
     * @throws RepositoryException  indicates that the work-flow call failed because of storage problems internal to the repository
     * @throws RemoteException indicates that the work-flow call failed because of a connection problem with the repository
     */
    public Document duplicate(String relPath)
      throws WorkflowException, MappingException, RepositoryException, RemoteException;

    /**
     * Duplicates a document contained directly in this folder. This creates a copy of the document in the same folder of the indicated
     * document, but with unique identification. Creating a duplicate is a distinct operation from copy operation (creating a copy of a
     * document in a different folder) because different rules may apply.
     * @param offspring the document reference of the document to be duplicated
     * @return the document reference of the created copy of the document
     * @throws WorkflowException indicates that the work-flow call failed due work-flow specific conditions
     * @throws MappingException indicates that the work-flow call failed because of configuration problems
     * @throws RepositoryException indicates that the work-flow call failed because of storage problems internal to the repository
     * @throws RemoteException indicates that the work-flow call failed because of a connection problem with the repository
     */
    public Document duplicate(Document offspring)
      throws WorkflowException, MappingException, RepositoryException, RemoteException;

    /**
     * Duplicates a document contained directly in this folder.  This creates a copy of the document in the same folder of the
     * indicated document, but with unique identification.  Creating a duplicate is a distinct operation from copy operation
     * (creating a copy of a document in a different folder) because different rules may apply.
     * @param relPath the relative path to the document to be duplicated
     * @param arguments a map of replacement patterns to apply, see add()
     * @return the document reference of the created copy of the document
     * @throws WorkflowException indicates that the work-flow call failed due work-flow specific conditions
     * @throws MappingException indicates that the work-flow call failed because of configuration problems
     * @throws RepositoryException indicates that the work-flow call failed because of storage problems internal to the repository
     * @throws RemoteException indicates that the work-flow call failed because of a connection problem with the repository
     */
    public Document duplicate(String relPath, Map<String,String> arguments)
      throws WorkflowException, MappingException, RepositoryException, RemoteException;

    /**
     * Duplicates a document contained directly in this folder. This creates a copy of the document in the same folder of the indicated
     * document, but with unique identification. Creating a duplicate is a distinct operation from copy operation (creating a copy of a
     * document in a different folder) because different rules may apply.
     * @param offspring the document reference of the document to be duplicated
     * @param arguments a map of replacement patterns to apply, see add()
     * @return the document reference of the created copy of the document
     * @throws WorkflowException  indicates that the work-flow call failed due work-flow specific conditions
     * @throws MappingException indicates that the work-flow call failed because of configuration problems
     * @throws RepositoryException  indicates that the work-flow call failed because of storage problems internal to the repository
     * @throws RemoteException indicates that the work-flow call failed because of a connection problem with the repository
     */
    public Document duplicate(Document offspring, Map<String,String> arguments)
      throws WorkflowException, MappingException, RepositoryException, RemoteException;

    /**
     * Initiates a copy operation to copy a document directly contained by the container document defining this work-flow
     * to another location.  The destination may not be the same folder as the source.  Both source and destination should
     * also implement the EmbedWorkflow in the work-flow category "embedded".
     * @param relPath the document name or relative path in case indices are needed
     * @param absPath the absolute path where the document should be copied to, including the new document name
     * @return the document reference of the moved document
     * @throws WorkflowException  indicates that the work-flow call failed due work-flow specific conditions
     * @throws MappingException indicates that the work-flow call failed because of configuration problems
     * @throws RepositoryException  indicates that the work-flow call failed because of storage problems internal to the repository
     * @throws RemoteException indicates that the work-flow call failed because of a connection problem with the repository
     */
    public Document copy(String relPath, String absPath)
      throws WorkflowException, MappingException, RepositoryException, RemoteException;

    /**
     * Initiates a copy operation to copy a document directly contained by the container document defining this work-flow
     * to another location.  The destination may not be the same folder as the source.  Both source and destination should
     * also implement the EmbedWorkflow in the work-flow category "embedded".
     * @param offspring the document reference which should be located directly beneath this container document
     * @param target the destination folder reference
     * @param arguments any rewrite options that may be performed on the moved node (see class description #FolderWorkflow)
     * @return the document reference of the moved document
     * @throws WorkflowException  indicates that the work-flow call failed due work-flow specific conditions
     * @throws MappingException indicates that the work-flow call failed because of configuration problems
     * @throws RepositoryException  indicates that the work-flow call failed because of storage problems internal to the repository
     * @throws RemoteException indicates that the work-flow call failed because of a connection problem with the repository
     */
    public Document copy(Document offspring, Document target, String name)
      throws WorkflowException, MappingException, RepositoryException, RemoteException;

    /**
     * Initiates a copy operation to copy a document directly contained by the container document defining this work-flow
     * to another location.  The destination may not be the same folder as the source.  Both source and destination should
     * also implement the EmbedWorkflow in the work-flow category "embedded".
     * @param relPath the document name or relative path in case indices are needed
     * @param absPath the absolute path where the document should be copied to, including the new document name
     * @param arguments any rewrite options that may be performed on the moved node (see class description #FolderWorkflow)
     * @return the document reference of the moved document
     * @throws WorkflowException  indicates that the work-flow call failed due work-flow specific conditions
     * @throws MappingException indicates that the work-flow call failed because of configuration problems
     * @throws RepositoryException  indicates that the work-flow call failed because of storage problems internal to the repository
     * @throws RemoteException indicates that the work-flow call failed because of a connection problem with the repository
     */
    public Document copy(String relPath, String absPath, Map<String,String> arguments)
      throws WorkflowException, MappingException, RepositoryException, RemoteException;

    /**
     * Initiates a copy operation to copy a document directly contained by the container document defining this work-flow
     * to another location.  The destination may not be the same folder as the source.  Both source and destination should
     * also implement the EmbedWorkflow in the work-flow category "embedded".
     * @param offspring the document reference which should be located directly beneath this container document
     * @param target the destination folder reference
     * @param name the new name of the document in the target destination, this may not be a relative path
     * @param arguments any rewrite options that may be performed on the moved node (see class description #FolderWorkflow)
     * @return the document reference of the moved document
     * @throws WorkflowException  indicates that the work-flow call failed due work-flow specific conditions
     * @throws MappingException indicates that the work-flow call failed because of configuration problems
     * @throws RepositoryException  indicates that the work-flow call failed because of storage problems internal to the repository
     * @throws RemoteException indicates that the work-flow call failed because of a connection problem with the repository
     */
    public Document copy(Document offspring, Document target, String name, Map<String,String> arguments)
      throws WorkflowException, MappingException, RepositoryException, RemoteException;

    /**
     * Initiates a move operation to move a document directly contained by the container document defining this work-flow
     * interface to another location.  Both source and destination should also implement the EmbedWorkflow in the work-flow
     * category "embedded".
     * @param relPath the document name or relative path in case indices are needed
     * @param absPath the absolute path where the document should be moved to, including the new document name
     * @return the document reference of the moved document
     * @throws WorkflowException  indicates that the work-flow call failed due work-flow specific conditions
     * @throws MappingException indicates that the work-flow call failed because of configuration problems
     * @throws RepositoryException  indicates that the work-flow call failed because of storage problems internal to the repository
     * @throws RemoteException indicates that the work-flow call failed because of a connection problem with the repository
     */
    public Document move(String relPath, String absPath)
      throws WorkflowException, MappingException, RepositoryException, RemoteException;

    /**
     * Initiates a move operation to move a document directly contained by the container document defining this work-flow
     * interface to another location.  Both source and destination should also implement the EmbedWorkflow in the work-flow
     * category "embedded".
     * @param offspring the document reference which should be located directly beneath this container document
     * @param target the destination folder reference
     * @param name the new name of the document in the target destination, this may not be a relative path
     * @return the document reference of the moved document
     * @throws WorkflowException  indicates that the work-flow call failed due work-flow specific conditions
     * @throws MappingException indicates that the work-flow call failed because of configuration problems
     * @throws RepositoryException  indicates that the work-flow call failed because of storage problems internal to the repository
     * @throws RemoteException indicates that the work-flow call failed because of a connection problem with the repository
     */

    public Document move(Document offspring, Document target, String name)
      throws WorkflowException, MappingException, RepositoryException, RemoteException;

    /**
     * Initiates a move operation to move a document directly contained by the container document defining this work-flow
     * interface to another location.  Both source and destination should also implement the EmbedWorkflow in the work-flow
     * category "embedded".
     * @param relPath the document name or relative path in case indices are needed
     * @param absPath the absolute path where the document should be moved to, including the new document name
     * @param arguments any rewrite options that may be performed on the moved node (see class description #FolderWorkflow)
     * @return the document reference of the moved document
     * @throws WorkflowException  indicates that the work-flow call failed due work-flow specific conditions
     * @throws MappingException indicates that the work-flow call failed because of configuration problems
     * @throws RepositoryException  indicates that the work-flow call failed because of storage problems internal to the repository
     * @throws RemoteException indicates that the work-flow call failed because of a connection problem with the repository
     */
    public Document move(String relPath, String absPath, Map<String,String> arguments)
      throws WorkflowException, MappingException, RepositoryException, RemoteException;

    /**
     * Initiates a move operation to move a document directly contained by the container document defining this work-flow
     * interface to another location.  Both source and destination should also implement the EmbedWorkflow in the work-flow
     * category "embedded".
     * @param offspring the document reference which should be located directly beneath this container document
     * @param target the destination folder reference
     * @param name the new name of the document in the target destination, this may not be a relative path
     * @param arguments any rewrite options that may be performed on the moved node (see class description #FolderWorkflow)
     * @return the document reference of the moved document
     * @throws WorkflowException  indicates that the work-flow call failed due work-flow specific conditions
     * @throws MappingException indicates that the work-flow call failed because of configuration problems
     * @throws RepositoryException  indicates that the work-flow call failed because of storage problems internal to the repository
     * @throws RemoteException indicates that the work-flow call failed because of a connection problem with the repository
     */
    public Document move(Document offspring, Document target, String name, Map<String,String> arguments)
      throws WorkflowException, MappingException, RepositoryException, RemoteException;
}
