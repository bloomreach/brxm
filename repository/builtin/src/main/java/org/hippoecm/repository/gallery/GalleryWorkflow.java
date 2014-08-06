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
package org.hippoecm.repository.gallery;

import java.rmi.RemoteException;
import java.util.List;

import javax.jcr.RepositoryException;

import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.api.WorkflowException;
import org.onehippo.repository.api.annotation.WorkflowAction;

public interface GalleryWorkflow extends Workflow {

    @WorkflowAction(loggable = false)
    public List<String> getGalleryTypes() throws RemoteException, RepositoryException;

    public Document createGalleryItem(String name, String type) throws RemoteException, RepositoryException, WorkflowException;

}
