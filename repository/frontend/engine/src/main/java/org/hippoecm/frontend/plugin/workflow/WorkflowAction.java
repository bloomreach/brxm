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
package org.hippoecm.frontend.plugin.workflow;

import java.util.List;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.wicket.IClusterable;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.service.IValidateService;
import org.hippoecm.repository.api.Workflow;

public abstract class WorkflowAction implements IClusterable {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";
    private static final long serialVersionUID = 1L;

    protected boolean validateSession(List<IValidateService> validators) {
        for (IValidateService validator : validators) {
            validator.validate();
            if (validator.hasError()) {
                return false;
            }
        }
        return true;
    }

    protected void prepareSession(JcrNodeModel handleModel) throws RepositoryException {
        // save the handle so that the workflow uses the correct content
        Node handleNode = handleModel.getNode();
        handleNode.getSession().save();
        handleNode.getSession().refresh(true);
    }

    public abstract void execute(Workflow workflow) throws Exception;

}
