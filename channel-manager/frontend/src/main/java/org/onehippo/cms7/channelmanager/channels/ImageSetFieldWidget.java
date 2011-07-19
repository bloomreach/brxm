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

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IChainingModel;
import org.apache.wicket.model.IModel;
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
import org.hippoecm.hst.core.parameters.ImageSetLink;

/**
 * Renders a widget to select an image. The given model is used to store the UUID of the selected image. The selected
 * image can also be removed again, which will store null in the model. The given dialog service is used to create
 * the dialog in which the image can be selected.
 *
 * The widget shows a small preview version of the selected image and two Ajax links: one link to select a new image,
 * and another link to 'remove' the current one. When no image is selected, the 'remove' link will not be shown.
 */
public class ImageSetFieldWidget extends Panel {

    private static final long serialVersionUID = 1L;

    private IModel<String> model;
    private InlinePreviewImage previewImage;
    private AjaxLink<Void> remove;

    public ImageSetFieldWidget(final IPluginContext context, final String id, final ImageSetLink imageSetLink, final IModel<String> model) {
        super(id);

        this.model = model;

        JavaPluginConfig pickerConfig = new JavaPluginConfig();
        pickerConfig.put("cluster.name", imageSetLink.pickerConfiguration());
        pickerConfig.put(NodePickerControllerSettings.BASE_UUID, imageSetLink.pickerInitialUuid());
        pickerConfig.put(NodePickerControllerSettings.LAST_VISITED_ENABLED, Boolean.toString(imageSetLink.pickerRemembersLastVisited()));
        pickerConfig.put(NodePickerControllerSettings.SELECTABLE_NODETYPES, imageSetLink.pickerSelectableNodeTypes());

        IDialogFactory dialogFactory = createDialogFactory(context, pickerConfig, model);
        IModel<String> selectLabelModel = new StringResourceModel("imageset.select", this, null);

        IDialogService dialogService = context.getService(IDialogService.class.getName(), IDialogService.class);
        DialogLink select = new DialogLink("select", selectLabelModel, dialogFactory, dialogService);
        add(select);

        remove = new AjaxLink<Void>("remove-link") {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                ImageSetFieldWidget.this.updateDisplay(null);
                target.addComponent(ImageSetFieldWidget.this);
            }

        };
        remove.add(new Label("remove-link-text", new StringResourceModel("imageset.remove", this, null)));
        add(remove);

        previewImage = new InlinePreviewImage("preview-image", model, imageSetLink.previewVariant());
        add(previewImage);

        updateDisplay(model.getObject());

        setOutputMarkupId(true);
    }

    /**
     * Updates the display of this widget. When an image is selected, a small preview and the 'remove' link will
     * be shown. If no image is selected, the preview and 'remove' link are hidden. The caller should take care
     * of re-rendering this widget, if necessary (e.g. in an Ajax call).
     *
     * @param imageUuid the UUID of the selected image, or null of no image is selected
     */
    private void updateDisplay(String imageUuid) {
        model.setObject(imageUuid);

        if (previewImage.isValid()) {
            previewImage.setVisible(true);
            remove.setVisible(true);
        } else {
            previewImage.setVisible(false);
            remove.setVisible(false);
        }
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
                        AjaxRequestTarget.get().addComponent(ImageSetFieldWidget.this);
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