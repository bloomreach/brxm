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

import java.util.Date;
import javax.jcr.RepositoryException;

public interface WorkflowContext {
    final static String SVN_ID = "$Id$";

    // WARNING: THE call getWorkflowContext is not yet standardized
    public WorkflowContext getWorkflowContext(Date timestamp);

    public Document getDocument(String category, String identifier) throws RepositoryException;
    public Workflow getWorkflow(String category) throws MappingException, WorkflowException, RepositoryException;
    public Workflow getWorkflow(String category, Document document) throws MappingException, WorkflowException,
                                                                           RepositoryException;
    public String getUsername();
}
