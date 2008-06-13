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

import javax.jcr.RepositoryException;

public interface WorkflowDescriptor {
    @SuppressWarnings("unused")
    final static String SVN_ID = "$Id$";

    /**
     * Conveniance method to obtain the human-interpretable display name of
     * this workflow.
     *
     * @returns a description of the workflow
     */
    public String getDisplayName() throws RepositoryException;

    /**
     * Conveniance method to access class name to be used by a front-end
     * application to access the workflow.  This front-end class is not part
     * of the repository interface.
     *
     * @returns classname of the class to be instantiated for the workflow
     * rendering in a front-end.
     */
    public String getRendererName() throws RepositoryException;
}
