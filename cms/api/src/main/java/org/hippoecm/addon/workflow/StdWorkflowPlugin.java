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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.List;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.ListDataProvider;

import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.frontend.widgets.AbstractView;
import org.hippoecm.repository.api.HippoWorkspace;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowDescriptor;
import org.hippoecm.repository.api.WorkflowManager;

class StdWorkflowPlugin extends Panel {

    StdWorkflowPlugin(String id, WorkflowDescriptorModel model) {
        super(id, model);
        List<StdWorkflow> list = new LinkedList<StdWorkflow>();
        WorkflowDescriptor descriptor = (WorkflowDescriptor)model.getObject();
        try {
            Class<Workflow>[] interfaces = descriptor.getInterfaces();
            for (int i = 0; i < interfaces.length && i < 1; i++) {
                for (Method method : interfaces[i].getDeclaredMethods()) {
                    StdWorkflow wf = new StdWorkflow("id", method.getName()) {
                        @Override
                        protected void invoke() {
                            try {
                                WorkflowDescriptor descriptor = (WorkflowDescriptor) StdWorkflowPlugin.this.getDefaultModelObject();
                                Session session = UserSession.get().getJcrSession();
                                WorkflowManager manager = ((HippoWorkspace)session.getWorkspace()).getWorkflowManager();
                                session.save();
                                session.refresh(true);
                                Workflow workflow = manager.getWorkflow(descriptor);
                                ((Method)getDefaultModelObject()).invoke(workflow, new Object[0]);
                                session.refresh(false);
                            } catch(RepositoryException ex) {
                                System.err.println(ex.getClass().getName() + ": " + ex.getMessage());
                                ex.printStackTrace(System.err);           
                            } catch(IllegalAccessException ex) {
                                System.err.println(ex.getClass().getName() + ": " + ex.getMessage());
                                ex.printStackTrace(System.err); 
                            } catch(InvocationTargetException ex) {                        
                                System.err.println(ex.getClass().getName() + ": " + ex.getMessage());
                                ex.printStackTrace(System.err);
                            }
                        }
                    };
                    wf.setDefaultModel(new WorkflowMethodDescriptorModel((WorkflowDescriptorModel)StdWorkflowPlugin.this.getDefaultModel(), method));
                    list.add(wf);
                }
            }
        } catch (ClassNotFoundException ex) {
            System.err.println(ex.getClass().getName() + ": " + ex.getMessage());
            ex.printStackTrace(System.err);
        } catch (RepositoryException ex) {
            System.err.println(ex.getClass().getName() + ": " + ex.getMessage());
            ex.printStackTrace(System.err);
        }
        AbstractView view;
        add(view = new AbstractView("view", new ListDataProvider(list)) {
            @Override
            protected void populateItem(Item item) {
                item.add((StdWorkflow)item.getModelObject());
            }
        });
        view.populate();
    }
}
