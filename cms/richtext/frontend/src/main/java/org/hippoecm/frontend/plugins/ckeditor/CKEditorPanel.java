package org.hippoecm.frontend.plugins.ckeditor;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptUrlReferenceHeaderItem;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.apache.wicket.request.resource.ResourceReference;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.ckeditor.dialog.images.CKEditorImageService;
import org.hippoecm.frontend.plugins.ckeditor.dialog.images.ImagePickerBehavior;
import org.hippoecm.frontend.plugins.ckeditor.dialog.links.CKEditorLinkService;
import org.hippoecm.frontend.plugins.ckeditor.dialog.links.DocumentPickerBehavior;
import org.hippoecm.frontend.plugins.ckeditor.hippopicker.HippoPicker;
import org.hippoecm.frontend.plugins.richtext.IHtmlCleanerService;
import org.hippoecm.frontend.plugins.richtext.IImageURLProvider;
import org.hippoecm.frontend.plugins.richtext.IRichTextImageFactory;
import org.hippoecm.frontend.plugins.richtext.IRichTextLinkFactory;
import org.hippoecm.frontend.plugins.richtext.RichTextImageURLProvider;
import org.hippoecm.frontend.plugins.richtext.RichTextModel;
import org.hippoecm.frontend.plugins.richtext.jcr.JcrRichTextImageFactory;
import org.hippoecm.frontend.plugins.richtext.jcr.JcrRichTextLinkFactory;
import org.hippoecm.frontend.plugins.richtext.model.PrefixingModel;
import org.hippoecm.frontend.plugins.richtext.view.RichTextPreviewPanel;
import org.hippoecm.frontend.service.IBrowseService;
import org.json.JSONException;
import org.json.JSONObject;
import org.onehippo.cms7.ckeditor.CKEditorConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Renders an instance of CKEditor to edit the given model.
 */
class CKEditorPanel extends Panel {

    private static final String WICKET_ID_EDITOR = "editor";
    private static final ResourceReference CKEDITOR_PANEL_CSS = new PackageResourceReference(CKEditorPanel.class, "CKEditorPanel.css");
    private static final ResourceReference CKEDITOR_PANEL_JS = new PackageResourceReference(CKEditorPanel.class, "CKEditorPanel.js");
    private static final int LOGGED_EDITOR_CONFIG_INDENT_SPACES = 2;

    private static final Logger log = LoggerFactory.getLogger(CKEditorPanel.class);

    private final String editorConfigJson;
    private final String editorId;
    private final CKEditorImageService imageService;
    private final ImagePickerBehavior imagePickerBehavior;
    private final CKEditorLinkService linkService;
    private final DocumentPickerBehavior documentPickerBehavior;

    CKEditorPanel(final String id,
                  final IPluginContext context,
                  final IPluginConfig imagePickerConfig,
                  final IPluginConfig documentPickerConfig,
                  final String editorConfigJson,
                  final JcrNodeModel nodeModel,
                  final IModel<String> htmlModel,
                  IHtmlCleanerService htmlCleaner) {
        super(id);

        this.editorConfigJson = editorConfigJson;

        final TextArea<String> textArea = createTextArea(nodeModel, htmlModel, htmlCleaner);
        add(textArea);

        editorId = textArea.getMarkupId();

        imageService = new CKEditorImageService(new JcrRichTextImageFactory(nodeModel), editorId);
        imagePickerBehavior = new ImagePickerBehavior(context, imagePickerConfig, imageService, editorId);
        add(imagePickerBehavior);

        linkService = new CKEditorLinkService(new JcrRichTextLinkFactory(nodeModel), editorId);
        documentPickerBehavior = new DocumentPickerBehavior(context, documentPickerConfig, linkService, editorId);
        add(documentPickerBehavior);
    }

    private TextArea<String> createTextArea(final JcrNodeModel nodeModel, final IModel<String> htmlModel, final IHtmlCleanerService htmlCleaner) {
        final IModel<String> editModel = createEditModel(nodeModel, htmlModel, htmlCleaner);
        final TextArea<String> textArea = new TextArea<String>(WICKET_ID_EDITOR, editModel);
        textArea.setOutputMarkupId(true);
        return textArea;
    }

    private IModel<String> createEditModel(final JcrNodeModel nodeModel, final IModel<String> htmlModel, final IHtmlCleanerService htmlCleaner) {
        final IRichTextImageFactory imageFactory = new JcrRichTextImageFactory(nodeModel);
        final IRichTextLinkFactory linkFactory = new JcrRichTextLinkFactory(nodeModel);

        RichTextModel model = new RichTextModel(htmlModel);
        model.setCleaner(htmlCleaner);
        model.setLinkFactory(linkFactory);

        IImageURLProvider urlProvider = new RichTextImageURLProvider(imageFactory, linkFactory);
        return new PrefixingModel(model, urlProvider);
    }

    @Override
    public void renderHead(final IHeaderResponse response) {
        super.renderHead(response);

        response.render(CssHeaderItem.forReference(CKEDITOR_PANEL_CSS));
        response.render(JavaScriptUrlReferenceHeaderItem.forReference(CKEditorConstants.CKEDITOR_JS));
        response.render(JavaScriptUrlReferenceHeaderItem.forReference(CKEDITOR_PANEL_JS));
        response.render(OnDomReadyHeaderItem.forScript(getJavaScriptForEditor()));
    }

    private String getJavaScriptForEditor() {
        return "Hippo.createCKEditor('" + editorId + "', " + getConfigurationForEditor() + ");";
    }

    private String getConfigurationForEditor() {
        try {
            JSONObject editorConfig = JsonUtils.createJSONObject(editorConfigJson);

            // always use the language of the current CMS locale
            editorConfig.put("language", getLocale().getLanguage());

            // load and configure Hippo CKEditor plugins
            JsonUtils.appendToCommaSeparatedString(editorConfig, "extraPlugins", HippoPicker.PLUGIN_NAME);
            editorConfig.put(HippoPicker.CONFIG_KEY, createHippoPickerConfiguration());

            // use a div-based editor instead of an iframe-based one to decrease loading time for many editor instances
            JsonUtils.appendToCommaSeparatedString(editorConfig, "extraPlugins", "divarea");

            // disable custom config loading if not configured
            JsonUtils.putIfAbsent(editorConfig, "customConfig", StringUtils.EMPTY);

            if (log.isInfoEnabled()) {
                log.info("CKEditor configuration:\n" + editorConfig.toString(LOGGED_EDITOR_CONFIG_INDENT_SPACES));
            }

            return editorConfig.toString();
        } catch (JSONException e) {
            throw new IllegalStateException("Error creating CKEditor configuration.", e);
        }
    }

    private JSONObject createHippoPickerConfiguration() throws JSONException {
        JSONObject config = new JSONObject();

        JSONObject imagePickerConfig = new JSONObject();
        imagePickerConfig.put(HippoPicker.IMAGE_PICKER_CONFIG_CALLBACK_URL, imagePickerBehavior.getCallbackUrl().toString());
        config.put(HippoPicker.IMAGE_PICKER_CONFIG_KEY, imagePickerConfig);

        JSONObject internalLinkPickerConfig = new JSONObject();
        internalLinkPickerConfig.put(HippoPicker.INTERNAL_LINK_PICKER_CONFIG_CALLBACK_URL, documentPickerBehavior.getCallbackUrl().toString());
        config.put(HippoPicker.INTERNAL_LINK_PICKER_CONFIG_KEY, internalLinkPickerConfig);

        return config;
    }

    @Override
    protected void onDetach() {
        if (imageService != null) {
            imageService.detach();
        }
        if (linkService != null) {
            linkService.detach();
        }
        super.onDetach();
    }

}
