/*
 * Copyright 2007 Hippo
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
package org.hippoecm.frontend.sa.plugin.workflow;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import javax.jcr.RepositoryException;

import org.apache.wicket.Component;
import org.apache.wicket.Session;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.model.Model;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.WorkflowsModel;
import org.hippoecm.frontend.sa.core.PluginContext;
import org.hippoecm.frontend.sa.dialog.AbstractDialog;
import org.hippoecm.frontend.sa.dialog.AbstractWorkflowDialog;
import org.hippoecm.frontend.sa.dialog.DialogLink;
import org.hippoecm.frontend.sa.dialog.IDialogFactory;
import org.hippoecm.frontend.sa.plugin.render.RenderPlugin;
import org.hippoecm.frontend.service.IDialogService;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.MappingException;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AbstractWorkflowPlugin extends RenderPlugin {
    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(AbstractWorkflowPlugin.class);

    protected interface Visibility extends Serializable {
        boolean isVisible();
    }

    static class Action implements Serializable {
        private static final long serialVersionUID = 1L;

        Component component;
        Visibility visible;

        Action(Component comp, Visibility vis) {
            component = comp;
            visible = vis;
        }
    }

    private Map<String, Action> actions;

    public AbstractWorkflowPlugin() {
        actions = new HashMap<String, Action>();
    }

    @Override
    public PluginContext getPluginContext() {
        return super.getPluginContext();
    }

    protected void addWorkflowDialog(final String dialogName, final String dialogLink, final String dialogTitle,
            Visibility visible, final WorkflowDialogAction action) {
        add(new EmptyPanel(dialogName));

        actions.put(dialogName, new Action(new DialogLink(dialogName, new Model(dialogLink), new IDialogFactory() {
            private static final long serialVersionUID = 1L;

            public AbstractDialog createDialog(IDialogService dialogService) {
                return new AbstractWorkflowDialog(AbstractWorkflowPlugin.this, dialogService, dialogTitle) {
                    protected void execute() throws Exception {
                        action.execute(getWorkflow());
                    }
                };
            }
        }), visible));

        updateActions();
    }

    protected void addWorkflowAction(final String linkName, final String linkTitle, Visibility visible,
            final WorkflowDialogAction action) {
        add(new EmptyPanel(linkName));

        actions.put(linkName, new Action(new AjaxLink(linkName, new Model(linkTitle)) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                try {
                    // before saving (which possibly means deleting), find the handle
                    final WorkflowsModel workflowModel = (WorkflowsModel) AbstractWorkflowPlugin.this.getModel();
                    JcrNodeModel handle = workflowModel.getNodeModel();
                    while (handle.getParentModel() != null && !handle.getNode().isNodeType(HippoNodeType.NT_HANDLE)) {
                        handle = handle.getParentModel();
                    }
                    // save the handle so that the workflow uses the correct content
                    handle.getNode().save();
                    ((UserSession) Session.get()).getJcrSession().refresh(true);

                    Workflow workflow = null;
                    try {
                        WorkflowManager manager = ((UserSession) Session.get()).getWorkflowManager();
                        workflow = manager.getWorkflow(workflowModel.getWorkflowDescriptor());
                    } catch (MappingException e) {
                        log.error(e.getMessage());
                    } catch (RepositoryException e) {
                        log.error(e.getMessage());
                    } catch (Exception e) {
                        log.error(e.getMessage());
                    }

                    action.execute(workflow);
                } catch (RepositoryException ex) {
                    log.error("Invalid data to save", ex);
                } catch (Exception ex) {
                    log.error(ex.getMessage());
                    ex.printStackTrace();
                }

            }
        }, visible));

        updateActions();
    }

    protected void addWorkflowAction(final String linkName, final String linkTitle, final WorkflowDialogAction action) {
        addWorkflowAction(linkName, linkTitle, new Visibility() {
            private static final long serialVersionUID = 1L;

            public boolean isVisible() {
                return true;
            }
        }, action);
    }

    protected void addWorkflowDialog(final String dialogName, final String dialogLink, final String dialogTitle,
            final WorkflowDialogAction action) {
        addWorkflowDialog(dialogName, dialogTitle, dialogTitle, new Visibility() {
            private static final long serialVersionUID = 1L;

            public boolean isVisible() {
                return true;
            }
        }, action);
    }

    protected void updateActions() {
        for (Map.Entry<String, Action> entry : actions.entrySet()) {
            if (entry.getValue().visible.isVisible()) {
                replace(entry.getValue().component);
            } else {
                replace(new EmptyPanel(entry.getValue().component.getId()));
            }
        }
    }

    @Override
    public void onModelChanged() {
        super.onModelChanged();
        updateActions();
    }
}
