/*
 * Copyright 2013-2018 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.frontend.plugins.ckeditor;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;

import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.editor.plugins.linkpicker.LinkPickerDialogConfig;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.properties.JcrPropertyModel;
import org.hippoecm.frontend.model.properties.JcrPropertyValueModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugin.config.impl.JavaPluginConfig;
import org.hippoecm.frontend.plugins.richtext.RichTextModel;
import org.hippoecm.frontend.plugins.richtext.dialog.images.ImagePickerManager;
import org.hippoecm.frontend.plugins.richtext.dialog.links.LinkPickerManager;
import org.hippoecm.frontend.plugins.richtext.htmlprocessor.WicketModel;
import org.hippoecm.frontend.plugins.richtext.model.RichTextModelFactory;
import org.hippoecm.frontend.plugins.richtext.view.RichTextDiffWithLinksAndImagesPanel;
import org.hippoecm.frontend.plugins.richtext.view.RichTextPreviewWithLinksAndImagesPanel;
import org.hippoecm.frontend.plugins.standards.diff.DefaultHtmlDiffService;
import org.hippoecm.frontend.plugins.standards.diff.DiffService;
import org.hippoecm.frontend.plugins.standards.picker.NodePickerControllerSettings;
import org.hippoecm.frontend.service.IBrowseService;
import org.hippoecm.repository.HippoStdNodeType;
import org.hippoecm.repository.util.JcrUtils;
import org.onehippo.ckeditor.CKEditorConfig;
import org.onehippo.cms7.services.htmlprocessor.model.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Node field plugin for editing HTML in the String property {@link HippoStdNodeType#HIPPOSTD_CONTENT}
 * using CKEditor. The plugin enables the 'hippopicker' CKEditor plugin for picking internal links and images.
 * Configuration properties:
 * <ul>
 *     <li>imagepicker: child node with node picker controller settings for the image picker dialog.
 *         Default image picker settings: {@link #DEFAULT_IMAGE_PICKER_CONFIG}</li>
 *     <li>linkpicker: child node with node picker controller settings for the document picker dialog
 *         opened by the internal link picker button.
 *         Default link picker settings: {@link #DEFAULT_LINK_PICKER_CONFIG}</li>
 * </ul>
 *
 * @see NodePickerControllerSettings
 */
public class CKEditorNodePlugin extends AbstractCKEditorPlugin<Node> {

    public static final String CONFIG_CHILD_IMAGE_PICKER = "imagepicker";
    public static final String CONFIG_CHILD_LINK_PICKER = "linkpicker";

    public static final IPluginConfig DEFAULT_IMAGE_PICKER_CONFIG = createNodePickerSettings(
            "cms-pickers/images", "ckeditor-imagepicker", "hippostd:gallery");

    public static final IPluginConfig DEFAULT_LINK_PICKER_CONFIG = createNodePickerSettings(
            "cms-pickers/documents", "ckeditor-linkpicker", "hippostd:folder");

    private static final Logger log = LoggerFactory.getLogger(CKEditorNodePlugin.class);

    public CKEditorNodePlugin(final IPluginContext context, final IPluginConfig config) {
        super(context, config, CKEditorConfig.DEFAULT_RICH_TEXT_CONFIG);
    }

    /**
     * @return a panel that shows a preview version of the HTML, including clickable links and visible images.
     */
    @Override
    protected Panel createViewPanel(final String id) {
        return new RichTextPreviewWithLinksAndImagesPanel(id, getNodeModel(), getHtmlModel(), getBrowser(), getHtmlProcessorId());
    }

    /**
     * Creates the {@link CKEditorPanel} with the CKEditor instance to edit HTML.
     * Override this method to add custom server-side {@link CKEditorPanelExtension}
     * to the returned panel.
     *
     * @param id the Wicket ID of the panel
     * @param editorConfigJson the JSON configuration of the CKEditor instance to create.
     * @return the CKEditorPanel for editing the HTML content.
     */
    @Override
    protected CKEditorPanel createEditPanel(final String id, final String editorConfigJson) {
        final CKEditorPanel editPanel = super.createEditPanel(id, editorConfigJson);
        addPickerExtension(editPanel);
        return editPanel;
    }

    private void addPickerExtension(final CKEditorPanel editPanel) {
        final String editorId = editPanel.getEditorId();
        final LinkPickerManager linkPicker = createLinkPicker(editorId);
        final ImagePickerManager imagePicker = createImagePicker(editorId);
        final CKEditorPanelPickerExtension pickerExtension = new CKEditorPanelPickerExtension(linkPicker, imagePicker);
        editPanel.addExtension(pickerExtension);
    }

    private ImagePickerManager createImagePicker(final String editorId) {
        final IPluginConfig imagePickerConfig = getChildPluginConfig(CONFIG_CHILD_IMAGE_PICKER, DEFAULT_IMAGE_PICKER_CONFIG);
        final Model<Node> nodeModel = WicketModel.of(getNodeModel());

        final ImagePickerManager richTextImagePicker = new ImagePickerManager(getPluginContext(), imagePickerConfig,
                nodeModel);

        richTextImagePicker.setCloseAction(new CKEditorInsertImageAction(editorId));
        return richTextImagePicker;
    }

    private LinkPickerManager createLinkPicker(final String editorId) {
        final IPluginConfig dialogConfig = LinkPickerDialogConfig.fromPluginConfig(
                getChildPluginConfig(CONFIG_CHILD_LINK_PICKER, DEFAULT_LINK_PICKER_CONFIG), (JcrPropertyValueModel) getHtmlModel());
        final Model<Node> nodeModel = WicketModel.of(getNodeModel());

        final LinkPickerManager richTextLinkPicker = new LinkPickerManager(getPluginContext(), dialogConfig,
                nodeModel);
        richTextLinkPicker.setCloseAction(new CKEditorInsertInternalLinkAction(editorId));
        return richTextLinkPicker;
    }

    @Override
    protected IModel<String> createEditModel() {
        return new RichTextModel(getHtmlProcessorId(), getHtmlModel(), getNodeModel());
    }

    @Override
    protected Panel createComparePanel(final String id, final IModel<Node> baseModel, final IModel<Node> currentModel) {
        final JcrNodeModel baseNodeModel = (JcrNodeModel) baseModel;
        final JcrNodeModel currentNodeModel = (JcrNodeModel) currentModel;
        final RichTextModelFactory modelFactory = new RichTextModelFactory(getHtmlProcessorId());

        return new RichTextDiffWithLinksAndImagesPanel(id, baseNodeModel, currentNodeModel,
                                                       getBrowser(), getDiffService(), modelFactory);
    }

    private DiffService getDiffService() {
        final String serviceId = getPluginConfig().getString(DiffService.SERVICE_ID);
        return getPluginContext().getService(serviceId, DefaultHtmlDiffService.class);
    }


    private IPluginConfig getChildPluginConfig(final String key, final IPluginConfig defaultConfig) {
        final IPluginConfig childConfig = getPluginConfig().getPluginConfig(key);
        return childConfig != null ? childConfig : defaultConfig;
    }

    /**
     * @return a model for the the String property {@link HippoStdNodeType#HIPPOSTD_CONTENT} of the model node.
     */
    @Override
    protected IModel<String> getHtmlModel() {
        final JcrNodeModel nodeModel = (JcrNodeModel) getDefaultModel();
        final Node contentNode = nodeModel.getNode();
        try {
            final Property contentProperty = contentNode.getProperty(HippoStdNodeType.HIPPOSTD_CONTENT);
            return new JcrPropertyValueModel<>(new JcrPropertyModel(contentProperty));
        } catch (final RepositoryException e) {
            final String nodePath = JcrUtils.getNodePathQuietly(contentNode);
            final String propertyPath = nodePath + "/@" + HippoStdNodeType.HIPPOSTD_CONTENT;
            log.warn("Cannot get value of HTML field property '{}' in plugin {}, using null instead",
                    propertyPath, getPluginConfig().getName());
        }
        return null;
    }

    private IBrowseService<IModel<Node>> getBrowser() {
        final String browserId = getPluginConfig().getString(IBrowseService.BROWSER_ID, "service.browse");
        return (IBrowseService<IModel<Node>>) getPluginContext().getService(browserId, IBrowseService.class);
    }

    private IModel<Node> getNodeModel() {
        return (IModel<Node>) getDefaultModel();
    }

    private static IPluginConfig createNodePickerSettings(final String clusterName, final String lastVisitedKey, final String... lastVisitedNodeTypes) {
        final JavaPluginConfig config = new JavaPluginConfig();
        config.put("cluster.name", clusterName);
        config.put(NodePickerControllerSettings.LAST_VISITED_KEY, lastVisitedKey);
        config.put(NodePickerControllerSettings.LAST_VISITED_NODETYPES, lastVisitedNodeTypes);
        config.makeImmutable();
        return config;
    }

}
