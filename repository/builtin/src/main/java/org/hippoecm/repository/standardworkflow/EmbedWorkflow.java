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
import java.rmi.RemoteException;

import java.util.Set;
import javax.jcr.RepositoryException;
import javax.jcr.Node;

import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.api.MappingException;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowException;

public interface EmbedWorkflow extends Workflow {
    static final String SVN_ID = "$Id$";

    public Document copyFrom(Document offspring, Document targetEmbed, String targetName, Map<String,String> arguments)
        throws WorkflowException, MappingException, RepositoryException, RemoteException;

    public Document copyTo(Document sourceFolder, Document offspring, String targetName, Map<String,String> arguments)
        throws WorkflowException, MappingException, RepositoryException, RemoteException;

    public Document copyOver(Node destination, Document offspring, Document result, Map<String,String> arguments)
        throws WorkflowException, MappingException, RepositoryException, RemoteException;

    public Document moveFrom(Document offspring, Document targetEmbed, String targetName, Map<String,String> arguments)
        throws WorkflowException, MappingException, RepositoryException, RemoteException;

    public Document moveTo(Document sourceFolder, Document offspring, String targetName, Map<String,String> arguments)
        throws WorkflowException, MappingException, RepositoryException, RemoteException;

    public Document moveOver(Node destination, Document offspring, Document result, Map<String,String> arguments)
        throws WorkflowException, MappingException, RepositoryException, RemoteException;
}
