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
package org.onehippo.cms7.channelmanager.channels;

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
import org.hippoecm.hst.core.parameters.AssetLink;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Renders a widget to select an asset. The given model is used to store the UUID of the selected asset. The selected
 * asset can also be removed again, which will store null in the model. The given dialog service is used to create
 * the dialog in which the asset can be selected.
 *
 * The widget shows the JCR node name of the selected asset and two Ajax links: one link to select a new asset,
 * and another link to 'remove' the current one. When no asset is selected, the 'remove' link will not be shown.
 */
public class AssetFieldWidget extends Panel {

    private static final long serialVersionUID = 1L;

    private final Logger log = LoggerFactory.getLogger(AssetFieldWidget.class);

    private IModel<String> model;
    private String previewAssetName;
    private AjaxLink<Void> remove;

    public AssetFieldWidget(final IPluginContext context, final String id, final AssetLink assetLink, final IModel<String> model) {
        super(id);

        this.model = model;

        JavaPluginConfig pickerConfig = new JavaPluginConfig();
        pickerConfig.put("cluster.name", assetLink.pickerConfiguration());
        pickerConfig.put(NodePickerControllerSettings.LAST_VISITED_ENABLED, Boolean.toString(assetLink.pickerRemembersLastVisited()));
        pickerConfig.put(NodePickerControllerSettings.SELECTABLE_NODETYPES, assetLink.pickerSelectableNodeTypes());

        String pickerInitialPath = assetLink.pickerInitialPath();
        if (pickerInitialPath != null && !"".equals(pickerInitialPath)) {
            javax.jcr.Session session = ((UserSession) Session.get()).getJcrSession();
            try {
                Node node = session.getNode(pickerInitialPath);
                pickerConfig.put(NodePickerControllerSettings.BASE_UUID, node.getIdentifier());
            } catch (PathNotFoundException e) {
                log.warn("Initial asset picker path not found: '{}'. Using the default initial path of '{}' instead.",
                        pickerInitialPath, assetLink.pickerConfiguration());
            } catch (RepositoryException e) {
                log.error("Could not retrieve the UUID of initial asset picker path node '" + pickerInitialPath
                        + "'. Using the default initial path of '" + assetLink.pickerConfiguration() + "' instead.", e);
            }
        }

        IDialogFactory dialogFactory = createDialogFactory(context, pickerConfig, model);
        IModel<String> selectLabelModel = new StringResourceModel("asset.select", this, null);

        IDialogService dialogService = context.getService(IDialogService.class.getName(), IDialogService.class);
        DialogLink select = new DialogLink("select", selectLabelModel, dialogFactory, dialogService);
        add(select);

        remove = new AjaxLink<Void>("remove-link") {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                AssetFieldWidget.this.updateDisplay(null);
                target.addComponent(AssetFieldWidget.this);
            }

        };
        remove.add(new Label("remove-link-text", new StringResourceModel("asset.remove", this, null)));
        add(remove);

        previewAssetName = resolvePreviewAssetName(model.getObject());

        Label previewAsset = new Label("preview-asset", new PropertyModel<String>(this, "previewAssetName"));
        add(previewAsset);

        updateDisplay(model.getObject());

        setOutputMarkupId(true);
    }

    String getPreviewAssetName() {
        return previewAssetName;
    }

    /**
     * Updates the display of this widget. When an asset is selected, a small preview and the 'remove' link will
     * be shown. If no asset is selected, the preview and 'remove' link are hidden. The caller should take care
     * of re-rendering this widget, if necessary (e.g. in an Ajax call).
     *
     * @param assetUuid the UUID of the selected asset, or null of no asset is selected
     */
    private void updateDisplay(String assetUuid) {
        model.setObject(assetUuid);

        previewAssetName = resolvePreviewAssetName(assetUuid);

        if (assetUuid != null) {
            remove.setVisible(true);
        } else {
            remove.setVisible(false);
        }
    }

    private String resolvePreviewAssetName(String assetUuid) {
        if (StringUtils.isBlank(assetUuid)) {
            return StringUtils.EMPTY;
        }
        final javax.jcr.Session session = ((UserSession) Session.get()).getJcrSession();
        try {
            Node handle = session.getNodeByIdentifier(assetUuid);
            return handle.getName();
        } catch (RepositoryException e) {
            log.warn("Cannot retrieve asset handle UUID '" + assetUuid + "'", e);
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
                        AjaxRequestTarget.get().addComponent(AssetFieldWidget.this);
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