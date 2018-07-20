/*
 *  Copyright 2012-2018 Hippo B.V. (http://www.onehippo.com)
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

import java.util.Iterator;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.StringResourceModel;
import org.hippoecm.frontend.PluginRequestTarget;
import org.hippoecm.frontend.dialog.Dialog;
import org.hippoecm.frontend.dialog.DialogConstants;
import org.hippoecm.frontend.model.IModelReference;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.NodeModelWrapper;
import org.hippoecm.frontend.model.event.IEvent;
import org.hippoecm.frontend.model.event.IObserver;
import org.hippoecm.frontend.plugin.IClusterControl;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IClusterConfig;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugin.config.IPluginConfigService;
import org.hippoecm.frontend.plugins.standards.list.resolvers.CssClass;
import org.hippoecm.frontend.service.IRenderService;
import org.hippoecm.frontend.service.ServiceTracker;
import org.hippoecm.frontend.widgets.TextFieldWidget;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class DestinationDialog extends Dialog<Void> implements IWorkflowInvoker {

    static final Logger log = LoggerFactory.getLogger(DestinationDialog.class);

    private IRenderService dialogRenderer;
    private IClusterControl control;
    private String modelServiceId;
    private ServiceTracker tracker;
    private final IPluginContext context;
    private final String intialPath;
    private final NodeModelWrapper<Node> destination;

    public DestinationDialog(IModel<String> title, IModel<String> question, IModel<String> answer,
                             final NodeModelWrapper destination,
                             final IPluginContext context, IPluginConfig config) {

        setTitle(title);
        setSize(DialogConstants.LARGE_AUTO);
        setFocusOnCancel();
        setOkEnabled(false);

        this.context = context;
        this.destination = destination;
        this.intialPath = getDestinationPath();

        add(createQuestionPanel("question", question, answer));

        IPluginConfigService pluginConfigService = context.getService(IPluginConfigService.class.getName(),
                                                                      IPluginConfigService.class);
        IClusterConfig cluster = pluginConfigService.getCluster("cms-pickers/folders");
        control = context.newCluster(cluster, config.getPluginConfig("cluster.options"));
        IClusterConfig decorated = control.getClusterConfig();

        control.start();

        modelServiceId = decorated.getString("model.folder");
        tracker = new ServiceTracker<IModelReference>(IModelReference.class) {

            IModelReference modelRef;
            IObserver modelObserver;

            @Override
            protected void onServiceAdded(IModelReference service, String name) {
                super.onServiceAdded(service, name);
                if (modelRef == null) {
                    modelRef = service;
                    modelRef.setModel(destination.getChainedModel());
                    context.registerService(modelObserver = new IObserver<IModelReference>() {

                        public IModelReference getObservable() {
                            return modelRef;
                        }

                        public void onEvent(Iterator<? extends IEvent<IModelReference>> events) {
                            IModel model = modelRef.getModel();
                            if (model != null && model instanceof JcrNodeModel && ((JcrNodeModel) model).getNode() != null) {
                                destination.setChainedModel(model);
                            }
                            DestinationDialog.this.setOkEnabled(isOkEnabled());
                        }
                    }, IObserver.class.getName());
                }
            }

            @Override
            protected void onRemoveService(IModelReference service, String name) {
                if (service == modelRef) {
                    context.unregisterService(modelObserver, IObserver.class.getName());
                    modelObserver = null;
                    modelRef = null;
                }
                super.onRemoveService(service, name);
            }

        };
        context.registerTracker(tracker, modelServiceId);

        dialogRenderer = context.getService(decorated.getString("wicket.id"), IRenderService.class);
        dialogRenderer.bind(null, "picker");
        add(dialogRenderer.getComponent());
    }

    @Override
    public void render(PluginRequestTarget target) {
        if (dialogRenderer != null) {
            dialogRenderer.render(target);
        }
        super.render(target);
    }

    protected Panel createQuestionPanel(final String id, final IModel<String> question, final IModel<String> answer) {
        if (question != null && answer != null) {
            return new QuestionPanel(id, question, answer);
        } else {
            return new EmptyPanel(id);
        }
    }

    @Override
    protected void onOk() {
        try {
            if (checkPermissions()) {
                if (checkFolderTypes()) {
                    invokeWorkflow();
                } else {
                    error(new StringResourceModel("foldertypes.denied", this).getString());
                }
            } else {
                error(new StringResourceModel("permission.denied", this).getString());
            }
        } catch (Exception e) {
            log.info("Could not execute workflow.", e);
            error(e);
        }
    }

    @Override
    public final void onClose() {
        super.onClose();
        dialogRenderer.unbind();
        dialogRenderer = null;
        control.stop();
        context.unregisterTracker(tracker, modelServiceId);
        tracker = null;
    }

    protected boolean isOkEnabled() {
        return !intialPath.equals(getDestinationPath());
    }

    protected boolean checkPermissions() {
        return true;
    }

    /**
     * Check if the destination folder is allowed by foldertype configuration to contain the type of document being
     * worked on.
     */
    protected boolean checkFolderTypes() {
        return true;
    }

    private String getDestinationPath() {
        try {
            return destination.getChainedModel().getObject().getPath();
        } catch (RepositoryException e) {
            log.error("Failed to get path of the destination node", e);
            return "";
        }
    }

    private static class QuestionPanel extends Panel {

        QuestionPanel(final String id, final IModel<String> question, final IModel<String> answer) {
            super(id);

            add(CssClass.append("question-answer"));
            add(new Label("question", question));
            add(new TextFieldWidget("answer", answer));
        }
    }
}
