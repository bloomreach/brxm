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

import java.util.Map;
import java.rmi.RemoteException;

import javax.jcr.RepositoryException;
import javax.jcr.Node;

import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.api.MappingException;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowException;

/**
 * Work-flow interface used internally by other work-flow implementation to hand-over documents from one container
 * document to another.  For instance when a document is copied from one folder to another directory container document,
 * then the EmbedWorkflow interface is a shared common interface between folder and directory.  A copy action is initiated
 * on the source location (#copyFrom) allowing the source location to prepare a copy action with any actions needed (like
 * taking the document offline).  This source location then follow-ups this call using the #copyTo method on the target
 * location which is able to pick up this copy and in its turn follows up the call using #copyOver on the source location
 * that finishes up any preparations that needed to be done.  Source and target container document may not refer to the
 * same location.
 */
public interface EmbedWorkflow extends Workflow {

    /**
     * Work-flow call performed on the source container document to copy a document, this call itself will escalate to
     * a copyTo and copyOver call.
     * @param offspring the document to be copied, that is a direct descendant of this document
     * @param targetEmbed the target container document also implementing an EmbedWorkflow
     * @param targetName the name the copied document should get in the target container document
     * @param arguments a set of options, these may include implementation specific re-write actions on the document
     * @return the copy of the document in the target destination
     * @throws WorkflowException  indicates that the work-flow call failed due work-flow specific conditions
     * @throws MappingException indicates that the work-flow call failed because of configuration problems, most notably because
     * the source container document has not been configured to the EmbedWorkflow interface
     * @throws RepositoryException  indicates that the work-flow call failed because of storage problems internal to the repository
     * @throws RemoteException indicates that the work-flow call failed because of a connection problem with the repository
     */
    public Document copyFrom(Document offspring, Document targetEmbed, String targetName, Map<String,String> arguments)
        throws WorkflowException, MappingException, RepositoryException, RemoteException;

    /**
     * Work-flow call performed on the target container document which will get the copied document.  This call is
     * invoked from within a copyFrom method.
     * @param sourceFolder the source container document also implementing an EmbedWorkflow that invoked the copyTo call
     * @param offspring the document that can be copied by the target container
     * @param targetName the name the copied document should get in the target container document
     * @param arguments a set of options, these may include implementation specific re-write actions on the document
     * @return the copy of the document in the target destination
     * @throws WorkflowException  indicates that the work-flow call failed due work-flow specific conditions
     * @throws MappingException indicates that the work-flow call failed because of configuration problems, most notably because
     * the source container document has not been configured to the EmbedWorkflow interface
     * @throws RepositoryException  indicates that the work-flow call failed because of storage problems internal to the repository
     * @throws RemoteException indicates that the work-flow call failed because of a connection problem with the repository
     */
    public Document copyTo(Document sourceFolder, Document offspring, String targetName, Map<String,String> arguments)
        throws WorkflowException, MappingException, RepositoryException, RemoteException;

    /**
     * Work-flow call performed on the source container document once the document has been copied.  This call is
     * invoked from within a copyTo method.
     * @param destination the target container document also implementing an EmbedWorkflow
     * @param offspring the document to be copied, that is a direct descendant of this document
     * @param result the copied document in the target container document
     * @param arguments a set of options, these may include implementation specific re-write actions on the document
     * @return the copy of the document in the target destination
     * @throws WorkflowException  indicates that the work-flow call failed due work-flow specific conditions
     * @throws MappingException indicates that the work-flow call failed because of configuration problems
     * @throws RepositoryException  indicates that the work-flow call failed because of storage problems internal to the repository
     * @throws RemoteException indicates that the work-flow call failed because of a connection problem with the repository
     */
    public Document copyOver(Node destination, Document offspring, Document result, Map<String,String> arguments)
        throws WorkflowException, MappingException, RepositoryException, RemoteException;

    /**
     * Work-flow call performed on the source container document to move a document, this call itself will escalate to
     * a moveTo and moveOver call.
     * @param offspring the document to be moved, that is a direct descendant of this document
     * @param targetEmbed the target container document also implementing an EmbedWorkflow
     * @param targetName the name the moved document should get in the target container document
     * @param arguments a set of options, these may include implementation specific re-write actions on the document
     * @return the moved document in the target destination
     * @throws WorkflowException  indicates that the work-flow call failed due work-flow specific conditions
     * @throws MappingException indicates that the work-flow call failed because of configuration problems, most notably because
     * the target container document has not been configured to the EmbedWorkflow interface
     * @throws RepositoryException  indicates that the work-flow call failed because of storage problems internal to the repository
     * @throws RemoteException indicates that the work-flow call failed because of a connection problem with the repository
     */
    public Document moveFrom(Document offspring, Document targetEmbed, String targetName, Map<String,String> arguments)
        throws WorkflowException, MappingException, RepositoryException, RemoteException;

    /**
     * Work-flow call performed on the target container document which will get the moved document.  This call is
     * invoked from within a moveFrom method.
     * @param sourceFolder the source container document also implementing an EmbedWorkflow that invoked the moveTo call
     * @param offspring the document that can be moved by the target container
     * @param targetName the name the moved document should get in the target container document
     * @param arguments a set of options, these may include implementation specific re-write actions on the document
     * @return the moved document in the target destination
     * @throws WorkflowException  indicates that the work-flow call failed due work-flow specific conditions
     * @throws MappingException indicates that the work-flow call failed because of configuration problems, most notably because
     * the source container document has not been configured to the EmbedWorkflow interface
     * @throws RepositoryException  indicates that the work-flow call failed because of storage problems internal to the repository
     * @throws RemoteException indicates that the work-flow call failed because of a connection problem with the repository
     */
    public Document moveTo(Document sourceFolder, Document offspring, String targetName, Map<String,String> arguments)
        throws WorkflowException, MappingException, RepositoryException, RemoteException;

    /**
     * Work-flow call performed on the source container document once the document has been moved.  This call is
     * invoked from within a moveTo method.
     * @param destination the target container document also implementing an EmbedWorkflow
     * @param offspring the document to be moved, that is a direct descendant of this document
     * @param result the moved document in the target container document
     * @param arguments a set of options, these may include implementation specific re-write actions on the document
     * @return the moved document in the target destination
     * @throws WorkflowException  indicates that the work-flow call failed due work-flow specific conditions
     * @throws MappingException indicates that the work-flow call failed because of configuration problems
     * @throws RepositoryException  indicates that the work-flow call failed because of storage problems internal to the repository
     * @throws RemoteException indicates that the work-flow call failed because of a connection problem with the repository
     */
    public Document moveOver(Node destination, Document offspring, Document result, Map<String,String> arguments)
        throws WorkflowException, MappingException, RepositoryException, RemoteException;
}
