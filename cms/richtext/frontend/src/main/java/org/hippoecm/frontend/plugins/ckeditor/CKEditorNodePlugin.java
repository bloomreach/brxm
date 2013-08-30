/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
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
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.properties.JcrPropertyModel;
import org.hippoecm.frontend.model.properties.JcrPropertyValueModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugin.config.impl.JavaPluginConfig;
import org.hippoecm.frontend.plugins.richtext.dialog.images.RichTextEditorImageService;
import org.hippoecm.frontend.plugins.richtext.dialog.images.ImagePickerBehavior;
import org.hippoecm.frontend.plugins.richtext.dialog.links.RichTextEditorLinkService;
import org.hippoecm.frontend.plugins.richtext.dialog.links.DocumentPickerBehavior;
import org.hippoecm.frontend.plugins.richtext.IImageURLProvider;
import org.hippoecm.frontend.plugins.richtext.IRichTextImageFactory;
import org.hippoecm.frontend.plugins.richtext.IRichTextLinkFactory;
import org.hippoecm.frontend.plugins.richtext.RichTextImageURLProvider;
import org.hippoecm.frontend.plugins.richtext.RichTextModel;
import org.hippoecm.frontend.plugins.richtext.jcr.JcrRichTextImageFactory;
import org.hippoecm.frontend.plugins.richtext.jcr.JcrRichTextLinkFactory;
import org.hippoecm.frontend.plugins.richtext.model.PrefixingModel;
import org.hippoecm.frontend.plugins.richtext.view.RichTextDiffWithLinksAndImagesPanel;
import org.hippoecm.frontend.plugins.richtext.view.RichTextPreviewWithLinksAndImagesPanel;
import org.hippoecm.frontend.plugins.standards.picker.NodePickerControllerSettings;
import org.hippoecm.frontend.service.IBrowseService;
import org.hippoecm.repository.HippoStdNodeType;
import org.hippoecm.repository.util.JcrUtils;
import org.onehippo.cms7.ckeditor.CKEditorConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Node field plugin for editing HTML in the String property {@link HippoStdNodeType#HIPPOSTD_CONTENT}
 * using CKEditor. The plugin enables the 'hippopicker' CKEditor plugin for picking internal links and images.
 * Configuration properties:
 * <ul>
 *     <li>imagepicker: child node with node picker controller settings for the image picker dialog.
 *         Default image picker settings: @{link DEFAULT_IMAGE_PICKER_CONFIG}</li>
 *     <li>documentpicker: child node with node picker controller settings for the document picker dialog
 *         opened by the internal link picker button.
 *         Default document picker settings: @{link DEFAULT_DOCUMENT_PICKER_CONFIG}</li>
 * </ul>
 *
 * @see NodePickerControllerSettings
 */
public class CKEditorNodePlugin extends AbstractCKEditorPlugin<Node> {

    public static final String DEFAULT_EDITOR_CONFIG = "{"
            + "  codemirror: {"
            + "    autoFormatOnStart: true"
            + "  },"
            + "  contentsCss: 'ckeditor/hippocontents.css',"
            + "  dialog_buttonsOrder: 'ltr',"
            + "  keystrokes: ["
            + "    [ " + (CKEditorConstants.CTRL + asciiCode('M')) + ", 'maximize' ],"
            + "    [ " + (CKEditorConstants.ALT + asciiCode('B')) + ", 'showblocks' ]"
            + "  ],"
            + "  linkShowAdvancedTab: false,"
            + "  plugins: 'dialogui,dialog,a11yhelp,dialogadvtab,basicstyles,clipboard,button,panelbutton,panel,floatpanel,menu,contextmenu,resize,toolbar,elementspath,enterkey,entities,popup,floatingspace,listblock,richcombo,htmlwriter,wysiwygarea,indent,indentblock,indentlist,justify,link,list,liststyle,magicline,maximize,pastetext,pastefromword,removeformat,showblocks,showborders,sourcearea,specialchar,menubutton,stylescombo,tab,table,tabletools,undo,codemirror,youtube',"
            + "  removeFormatAttributes: 'style,lang,width,height,align,hspace,valign',"
            + "  toolbarGroups: ["
            + "    { name: 'styles' },"
            + "    { name: 'basicstyles' },"
            + "    { name: 'undo' },"
            + "    { name: 'listindentalign',  groups: [ 'list', 'indent', 'align' ] },"
            + "    { name: 'links' },"
            + "    { name: 'insert' },"
            + "    { name: 'tools' },"
            + "    { name: 'mode' }"
            + "  ]"
            + "}";

    public static final String CONFIG_CHILD_IMAGE_PICKER = "imagepicker";
    public static final String CONFIG_CHILD_DOCUMENT_PICKER = "documentpicker";

    public static final IPluginConfig DEFAULT_IMAGE_PICKER_CONFIG = createNodePickerSettings(
            "cms-pickers/images", "ckeditor-imagepicker", "hippostd:gallery");

    public static final IPluginConfig DEFAULT_DOCUMENT_PICKER_CONFIG = createNodePickerSettings(
            "cms-pickers/documents", "ckeditor-documentpicker", "hippostd:folder");

    private static final Logger log = LoggerFactory.getLogger(CKEditorNodePlugin.class);

    public CKEditorNodePlugin(final IPluginContext context, final IPluginConfig config) {
        super(context, config, DEFAULT_EDITOR_CONFIG);
    }

    /**
     * @return a panel that shows a preview version of the HTML, including clickable links and visible images.
     */
    @Override
    protected Panel createViewPanel(final String id) {
        return new RichTextPreviewWithLinksAndImagesPanel(id, getNodeModel(), getHtmlModel(), getBrowser());
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
        final CKEditorPanel editPanel = new CKEditorPanel(id, editorConfigJson, createEditModel());
        addPickerBehavior(editPanel);
        return editPanel;
    }

    private void addPickerBehavior(final CKEditorPanel editPanel) {
        final String editorId = editPanel.getEditorId();
        final DocumentPickerBehavior documentPickerBehavior = createDocumentPickerBehavior(editorId);
        final ImagePickerBehavior imagePickerBehavior = createImagePickerBehavior(editorId);
        final CKEditorPanelPickerExtension pickerBehavior = new CKEditorPanelPickerExtension(documentPickerBehavior, imagePickerBehavior);
        editPanel.addExtension(pickerBehavior);
    }

    private DocumentPickerBehavior createDocumentPickerBehavior(final String editorId) {
        final IPluginConfig documentPickerConfig = getChildPluginConfig(CONFIG_CHILD_DOCUMENT_PICKER, DEFAULT_DOCUMENT_PICKER_CONFIG);

        final IRichTextLinkFactory linkFactory = createLinkFactory();
        RichTextEditorLinkService linkService = new RichTextEditorLinkService(linkFactory);

        final DocumentPickerBehavior behavior = new DocumentPickerBehavior(getPluginContext(), documentPickerConfig, linkService);
        behavior.setCloseAction(new CKEditorInsertInternalLinkAction(editorId));

        return behavior;
    }

    private ImagePickerBehavior createImagePickerBehavior(final String editorId) {
        final IPluginConfig imagePickerConfig = getChildPluginConfig(CONFIG_CHILD_IMAGE_PICKER, DEFAULT_IMAGE_PICKER_CONFIG);
        final IRichTextImageFactory imageFactory = new JcrRichTextImageFactory(getNodeModel());
        final RichTextEditorImageService imageService = new RichTextEditorImageService(imageFactory);

        final ImagePickerBehavior behavior = new ImagePickerBehavior(getPluginContext(), imagePickerConfig, imageService);
        behavior.setCloseAction(new CKEditorInsertImageAction(editorId));

        return behavior;
    }

    protected IModel<String> createEditModel() {
        final IRichTextLinkFactory linkFactory = createLinkFactory();

        final IModel<String> htmlModel = getHtmlModel();
        final RichTextModel model = new RichTextModel(htmlModel);
        model.setCleaner(getHtmlCleanerOrNull());
        model.setLinkFactory(linkFactory);

        final IRichTextImageFactory imageFactory = createImageFactory();
        final IImageURLProvider urlProvider = createImageUrlProvider(imageFactory, linkFactory);

        return new PrefixingModel(model, urlProvider);
    }

    protected IRichTextLinkFactory createLinkFactory() {
        return new JcrRichTextLinkFactory(getNodeModel());
    }

    protected IRichTextImageFactory createImageFactory() {
        return new JcrRichTextImageFactory(getNodeModel());
    }

    protected IImageURLProvider createImageUrlProvider(final IRichTextImageFactory imageFactory, final IRichTextLinkFactory linkFactory) {
        return new RichTextImageURLProvider(imageFactory, linkFactory);
    }

    @Override
    protected Panel createComparePanel(final String id, final IModel<Node> baseModel, final IModel<Node> currentModel) {
        final JcrNodeModel baseNodeModel = (JcrNodeModel) baseModel;
        final JcrNodeModel currentNodeModel = (JcrNodeModel) currentModel;
        return new RichTextDiffWithLinksAndImagesPanel(id, baseNodeModel, currentNodeModel, getBrowser());
    }

    private IPluginConfig getChildPluginConfig(final String key, IPluginConfig defaultConfig) {
        IPluginConfig childConfig = getPluginConfig().getPluginConfig(key);
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
            Property contentProperty = contentNode.getProperty(HippoStdNodeType.HIPPOSTD_CONTENT);
            return new JcrPropertyValueModel<String>(new JcrPropertyModel(contentProperty));
        } catch (RepositoryException e) {
            final String nodePath = JcrUtils.getNodePathQuietly(contentNode);
            final String propertyPath = nodePath + "/@" + HippoStdNodeType.HIPPOSTD_CONTENT;
            log.warn("Cannot get value of HTML field property '{}' in plugin {}, using null instead",
                    propertyPath, getPluginConfig().getName());
        }
        return null;
    }

    private IBrowseService getBrowser() {
        final String browserId = getPluginConfig().getString(IBrowseService.BROWSER_ID, "service.browse");
        return getPluginContext().getService(browserId, IBrowseService.class);
    }

    private JcrNodeModel getNodeModel() {
        return (JcrNodeModel) getDefaultModel();
    }

    private static IPluginConfig createNodePickerSettings(final String clusterName, final String lastVisitedKey, final String lastVisitedNodeTypes) {
        JavaPluginConfig config = new JavaPluginConfig();
        config.put("cluster.name", clusterName);
        config.put(NodePickerControllerSettings.LAST_VISITED_KEY, lastVisitedKey);
        config.put(NodePickerControllerSettings.LAST_VISITED_NODETYPES, lastVisitedNodeTypes);
        config.makeImmutable();
        return config;
    }

    private static final int asciiCode(char c) {
        return (int)c;
    }

}
