/*
 * Copyright 2008 Hippo
 *
 * Licensed under the Apache License, Version 2.0 (the  "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.frontend.plugins.standardworkflow;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.rmi.RemoteException;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.jcr.RepositoryException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.wicket.Session;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.IDataProvider;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import org.hippoecm.frontend.dialog.AbstractDialog;
import org.hippoecm.frontend.dialog.CustomizableDialogLink;
import org.hippoecm.frontend.dialog.IDialogService;
import org.hippoecm.frontend.dialog.IDialogFactory;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.WorkflowsModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugin.workflow.AbstractWorkflowPlugin;
import org.hippoecm.frontend.service.IViewService;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.frontend.widgets.AbstractView;
import org.hippoecm.repository.api.MappingException;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.api.WorkflowManager;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.standardworkflow.FolderWorkflow;

public class FolderWorkflowPlugin extends AbstractWorkflowPlugin {

    private static final long serialVersionUID = 1L;
    transient Logger log = LoggerFactory.getLogger(FolderWorkflowPlugin.class);
    Map<String, Set<String>> templates;

    public FolderWorkflowPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);
        add(new AbstractView("items", new IDataProvider() {

            public IModel model(Object object) {
                return new Model((String) object);
            }

            public int size() {
                return templates != null ? templates.size() : 0;
            }

            public Iterator iterator(int skip, int count) {
                return templates != null ? templates.keySet().iterator() : new TreeSet<String>().iterator();
            }

            public void detach() {
            }
        }) {

            protected void populateItem(Item item) {
                final IModel model = item.getModel();
                // final String dialogTitle = "Add " + ((String) model.getObject());
                final String dialogTitle = ((String) model.getObject());
                CustomizableDialogLink link;
                link = new CustomizableDialogLink("add-dialog", new Model(dialogTitle), new IDialogFactory() {

                    private static final long serialVersionUID = 1L;

                    public AbstractDialog createDialog(IDialogService dialogService) {
                      if(dialogTitle.contains("content selection"))
                        return new FolderWorkflowExtendedDialog(FolderWorkflowPlugin.this, dialogService, ((String)model.getObject()));
                      else
                        return new FolderWorkflowDialog(FolderWorkflowPlugin.this, dialogService, ((String)model.getObject()));
                    }
                }, getDialogService());

                // FIXME: proper procedure to get an icon
                if(dialogTitle.contains("folder") || dialogTitle.contains("Folder")) {
                    link.setIcon("addfolder_ico");
                } else if(dialogTitle.contains("document") || dialogTitle.contains("Document")) {
                    link.setIcon("adddocument_ico");
                } else {
                    link.setIcon("addextended_ico");
                }

                item.add(link);
            }
        });

        onModelChanged();
    }

    private void writeObject(ObjectOutputStream out)
            throws IOException {
        out.defaultWriteObject();
    }

    private void readObject(ObjectInputStream in)
            throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        log = LoggerFactory.getLogger(FolderWorkflowPlugin.class);
    }

    @Override
    public void onModelChanged() {
        super.onModelChanged();
        WorkflowsModel model = (WorkflowsModel) FolderWorkflowPlugin.this.getModel();
        WorkflowManager manager = ((UserSession) Session.get()).getWorkflowManager();
        try {
            FolderWorkflow workflow = (FolderWorkflow) manager.getWorkflow(model.getWorkflowDescriptor());
            templates = workflow.list();
        } catch (MappingException ex) {
        } catch (WorkflowException ex) {
        } catch (RepositoryException ex) {
        } catch (RemoteException ex) {
        }
    }

    public void select(JcrNodeModel nodeModel) {
        IViewService view = getPluginContext().getService(getPluginConfig().getString(IViewService.VIEWER_ID), IViewService.class);
        if (view != null) {
            try {
                if(nodeModel.getNode().isNodeType(HippoNodeType.NT_DOCUMENT) &&
                   !nodeModel.getNode().isNodeType("hippostd:folder")) {
                    view.view(nodeModel);
                }
            } catch(RepositoryException ex) {
                System.err.println(ex.getClass().getName()+": "+ex.getMessage());
                ex.printStackTrace(System.err);
            }
        }
    }
}
