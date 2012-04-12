/*
 *  Copyright 2011 Hippo.
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
package org.onehippo.cms7.channelmanager.widgets;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.Session;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IChainingModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;
import org.hippoecm.frontend.dialog.AbstractDialog;
import org.hippoecm.frontend.dialog.DialogLink;
import org.hippoecm.frontend.dialog.IDialogFactory;
import org.hippoecm.frontend.dialog.IDialogService;
import org.hippoecm.frontend.editor.plugins.linkpicker.LinkPickerDialog;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugin.config.impl.JavaPluginConfig;
import org.hippoecm.frontend.plugins.standards.picker.NodePickerControllerSettings;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.hst.core.parameters.JcrPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Renders a widget to select a JCR path. The given model is used to store the UUID of the selected path. The selected
 * path can also be removed again, which will store null in the model.
 *
 * The widget shows the selected JCR path as a string and two Ajax links: one link to select a new path,
 * and another link to 'remove' the current one. When no path is selected, the 'remove' link will not be shown.
 */
public class JcrPathWidget extends Panel {

    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(JcrPathWidget.class);

    private String previewName;
    private AjaxLink<Void> remove;

    public JcrPathWidget(final IPluginContext context, final String id, final JcrPath path, final String rootPath, final IModel<String> model) {
        super(id, model);

        String initialPath = path.pickerInitialPath();
        if (path.isRelative()) {
            initialPath = rootPath + (initialPath.startsWith("/") ? "" : "/") + initialPath;
        }
        JavaPluginConfig pickerConfig = createPickerConfig(
                path.pickerConfiguration(),
                path.pickerRemembersLastVisited(),
                path.pickerSelectableNodeTypes(),
                initialPath,
                rootPath
        );

        IDialogFactory dialogFactory = createDialogFactory(context, pickerConfig, model);
        IModel<String> selectLabelModel = new StringResourceModel("path.select", this, null);

        IDialogService dialogService = context.getService(IDialogService.class.getName(), IDialogService.class);
        DialogLink select = new DialogLink("select", selectLabelModel, dialogFactory, dialogService);
        add(select);

        remove = new AjaxLink<Void>("remove-link") {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                JcrPathWidget.this.updateDisplay(null);
                target.addComponent(JcrPathWidget.this);
            }

        };
        add(remove);

        previewName = resolvePreviewName(model.getObject());

        Label previewLabel = new Label("preview-name", new PropertyModel<String>(this, "previewName"));
        add(previewLabel);

        updateDisplay(model.getObject());

        setOutputMarkupId(true);
    }

    static JavaPluginConfig createPickerConfig(String pickerConfigPath, boolean remembersLastVisited,
                                        String[] selectableNodeTypes, String initialPath, String rootPath) {
        JavaPluginConfig pickerConfig = new JavaPluginConfig();
        pickerConfig.put("cluster.name", pickerConfigPath);
        pickerConfig.put(NodePickerControllerSettings.LAST_VISITED_ENABLED, Boolean.toString(remembersLastVisited));
        pickerConfig.put(NodePickerControllerSettings.SELECTABLE_NODETYPES, selectableNodeTypes);

        if (StringUtils.isNotEmpty(initialPath)) {
            javax.jcr.Session session = ((UserSession) Session.get()).getJcrSession();
            try {
                Node node = session.getNode(initialPath);
                pickerConfig.put(NodePickerControllerSettings.BASE_UUID, node.getIdentifier());
            } catch (PathNotFoundException e) {
                log.warn("Initial picker path not found: '{}'. Using the default initial path of '{}' instead.",
                        initialPath, pickerConfigPath);
            } catch (RepositoryException e) {
                log.error("Could not retrieve the UUID of initial picker path node '" + initialPath
                        + "'. Using the default initial path of '" + pickerConfigPath + "' instead.", e);
            }
        }
        if (StringUtils.isNotEmpty(rootPath)) {
            // set the cluster option 'root.path', which will be used as the root of the navigator in the document picker
            final JavaPluginConfig clusterOptions = new JavaPluginConfig();
            clusterOptions.put("root.path", rootPath);
            pickerConfig.put("cluster.options", clusterOptions);
        }
        return pickerConfig;
    }

    String getPreviewName() {
        return previewName;
    }

    /**
     * Updates the display of this widget. When a path is selected, its string value and the 'remove' link will
     * be shown. If no path is selected, the path and 'remove' link are hidden. The caller should take care
     * of re-rendering this widget, if necessary (e.g. in an Ajax call).
     *
     * @param uuid the UUID of the selected path, or null of no path is selected
     */
    private void updateDisplay(String uuid) {
        this.setDefaultModelObject(uuid);

        previewName = resolvePreviewName(uuid);

        if (uuid != null) {
            remove.setVisible(true);
        } else {
            remove.setVisible(false);
        }
    }
    
    private String resolvePreviewName(String uuid) {
        String previewPath = resolvePreviewPath(uuid);
        int offset = previewPath.lastIndexOf('/');
        if (offset != -1) {
            return previewPath.substring(offset+1);
        }
        return previewPath;
    }

    private String resolvePreviewPath(String uuid) {
        if (StringUtils.isBlank(uuid)) {
            return StringUtils.EMPTY;
        }
        final javax.jcr.Session session = ((UserSession) Session.get()).getJcrSession();
        try {
            Node node = session.getNodeByIdentifier(uuid);
            return node.getPath();
        } catch (RepositoryException e) {
            log.warn("Cannot retrieve node with UUID '" + uuid + "'", e);
        }
        return StringUtils.EMPTY;
    }

    private IDialogFactory createDialogFactory(final IPluginContext context, final IPluginConfig config, final IModel<String> model) {
        return new IDialogFactory() {
            private static final long serialVersionUID = 1L;

            public AbstractDialog<String> createDialog() {
                return new LinkPickerDialog(context, config, new IChainingModel<String>() {

                    private static final long serialVersionUID = 1L;

                    public String getObject() {
                        return model.getObject();
                    }

                    public void setObject(String uuid) {
                        updateDisplay(uuid);
                        AjaxRequestTarget.get().addComponent(JcrPathWidget.this);
                    }

                    public IModel<?> getChainedModel() {
                        return model;
                    }

                    public void setChainedModel(IModel<?> model) {
                        throw new UnsupportedOperationException("Value model cannot be changed");
                    }

                    public void detach() {
                        model.detach();
                    }
                });
            }
        };
    }

}