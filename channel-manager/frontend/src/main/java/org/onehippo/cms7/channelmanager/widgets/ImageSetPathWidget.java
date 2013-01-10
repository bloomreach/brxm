/*
 *  Copyright 2011-2013 Hippo B.V. (http://www.onehippo.com)
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

import org.apache.wicket.Session;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
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
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.hst.core.parameters.ImageSetPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Renders a widget to select an image. The given model is used to store the UUID of the selected image. The selected
 * image can also be removed again, which will store null in the model. The given dialog service is used to create
 * the dialog in which the image can be selected.
 *
 * The widget shows a small preview version of the selected image and two Ajax links: one link to select a new image,
 * and another link to 'remove' the current one. When no image is selected, the 'remove' link will not be shown.
 */
public class ImageSetPathWidget extends Panel {

    private static final long serialVersionUID = 1L;

    private final Logger log = LoggerFactory.getLogger(ImageSetPathWidget.class);

    private IModel<String> model;
    private InlinePreviewImage previewImage;
    private AjaxLink<Void> remove;

    public ImageSetPathWidget(final IPluginContext context, final String id, final ImageSetPath imageSetPath, final IModel<String> model) {
        super(id);

        this.model = model;

        JavaPluginConfig pickerConfig = new JavaPluginConfig();
        pickerConfig.put("cluster.name", imageSetPath.pickerConfiguration());
        pickerConfig.put(NodePickerControllerSettings.LAST_VISITED_ENABLED, Boolean.toString(imageSetPath.pickerRemembersLastVisited()));
        pickerConfig.put(NodePickerControllerSettings.SELECTABLE_NODETYPES, imageSetPath.pickerSelectableNodeTypes());

        String pickerInitialPath = imageSetPath.pickerInitialPath();
        if (pickerInitialPath != null && !"".equals(pickerInitialPath)) {
            javax.jcr.Session session = ((UserSession) Session.get()).getJcrSession();
            try {
                Node node = session.getNode(pickerInitialPath);
                pickerConfig.put(NodePickerControllerSettings.BASE_UUID, node.getIdentifier());
            } catch (PathNotFoundException e) {
                log.warn("Initial image picker path not found: '{}'. Using the default initial path of '{}' instead.",
                        pickerInitialPath, imageSetPath.pickerConfiguration());
            } catch (RepositoryException e) {
                log.error("Could not retrieve the UUID of initial image picker path node '" + pickerInitialPath
                        + "'. Using the default initial path of '" + imageSetPath.pickerConfiguration() + "' instead.", e);
            }
        }

        IDialogFactory dialogFactory = createDialogFactory(context, pickerConfig, model);
        IModel<String> selectLabelModel = new StringResourceModel("imageset.select", this, null);

        IDialogService dialogService = context.getService(IDialogService.class.getName(), IDialogService.class);
        DialogLink select = new DialogLink("select", selectLabelModel, dialogFactory, dialogService);
        add(select);

        remove = new AjaxLink<Void>("remove-link") {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                ImageSetPathWidget.this.updateDisplay(null);
                target.addComponent(ImageSetPathWidget.this);
            }

        };
        add(remove);

        previewImage = new InlinePreviewImage("preview-image", model, imageSetPath.previewVariant());
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
                        AjaxRequestTarget.get().addComponent(ImageSetPathWidget.this);
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