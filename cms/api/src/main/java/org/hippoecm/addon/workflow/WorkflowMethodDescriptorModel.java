/*
 *  Copyright 2009 Hippo.
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
package org.hippoecm.addon.workflow;

import java.lang.reflect.Method;

import javax.jcr.RepositoryException;

import org.apache.wicket.model.LoadableDetachableModel;

import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowDescriptor;

public class WorkflowMethodDescriptorModel extends LoadableDetachableModel {

    private WorkflowDescriptorModel descriptorModel;
    private String methodName;
    private String[] methodParameters;

    public WorkflowMethodDescriptorModel(WorkflowDescriptorModel descriptor, Method method) throws RepositoryException {
        super(method);
        methodName = method.getName();
        this.descriptorModel = descriptor;
        Class[] parameters = method.getParameterTypes();
        methodParameters = new String[parameters.length];
        for (int i = 0; i < parameters.length; i++) {
            methodParameters[i] = parameters[i].getName();
        }
    }

    @Override
    protected void onAttach() {
        descriptorModel.load();
    }

    @Override
    protected void onDetach() {
        descriptorModel.detach();
    }

    protected Object load() {
        if (!descriptorModel.isAttached()) {
            descriptorModel.load();
        }
        try {
            Class[] parameters = new Class[methodParameters.length];
            for (int i = 0; i < parameters.length; i++) {
                parameters[i] = Class.forName(methodParameters[i]);
            }
            WorkflowDescriptor descriptor = (WorkflowDescriptor)descriptorModel.getObject();
            Class<Workflow>[] interfaces = descriptor.getInterfaces();
            for (int i = 0; i < interfaces.length; i++) {
                try {
                    return interfaces[i].getDeclaredMethod(methodName, parameters);
                } catch (NoSuchMethodException ex) {
                    // deliberately ignored
                }
            }
        } catch (ClassNotFoundException ex) {
            System.err.println(ex.getClass().getName() + ": " + ex.getMessage());
            ex.printStackTrace(System.err);
        } catch (RepositoryException ex) {
            System.err.println(ex.getClass().getName() + ": " + ex.getMessage());
            ex.printStackTrace(System.err);
        }
        return null;
    }
}
