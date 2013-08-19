package org.hippoecm.frontend.plugins.ckeditor;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.behavior.AbstractAjaxBehavior;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptUrlReferenceHeaderItem;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IDetachable;
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
import org.json.JSONException;
import org.json.JSONObject;
import org.onehippo.cms7.ckeditor.CKEditorConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Renders an instance of CKEditor to edit the HTML in the given model.
 * Additional behavior can be added via the {@link #addBehavior(CKEditorPanelBehavior)} method.
 */
public class CKEditorPanel extends Panel {

    private static final String WICKET_ID_EDITOR = "editor";
    private static final ResourceReference CKEDITOR_PANEL_CSS = new PackageResourceReference(CKEditorPanel.class, "CKEditorPanel.css");
    private static final ResourceReference CKEDITOR_PANEL_JS = new PackageResourceReference(CKEditorPanel.class, "CKEditorPanel.js");
    private static final int LOGGED_EDITOR_CONFIG_INDENT_SPACES = 2;

    private static final Logger log = LoggerFactory.getLogger(CKEditorPanel.class);

    private final String editorConfigJson;
    private final String editorId;
    private final List<CKEditorPanelBehavior> behaviors;

    public CKEditorPanel(final String id,
                  final String editorConfigJson,
                  final IModel<String> editModel) {
        super(id);

        this.editorConfigJson = editorConfigJson;

        final TextArea<String> textArea = new TextArea<String>(WICKET_ID_EDITOR, editModel);
        textArea.setOutputMarkupId(true);
        add(textArea);

        editorId = textArea.getMarkupId();

        behaviors = new LinkedList<CKEditorPanelBehavior>();
    }

    /**
     * @return the ID of the editor instance.
     */
    public String getEditorId() {
        return editorId;
    }

    /**
     * Adds custom server-side behavior to this panel.
     * @param behavior the behavior to add.
     */
    public void addBehavior(CKEditorPanelBehavior behavior) {
        behaviors.add(behavior);

        for (AbstractAjaxBehavior ajaxBehavior : behavior.getAjaxBehaviors()) {
            add(ajaxBehavior);
        }
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

            // configure behaviors
            for (CKEditorPanelBehavior behavior : behaviors) {
                behavior.addCKEditorConfiguration(editorConfig);
            }

            // always use the language of the current CMS locale
            editorConfig.put("language", getLocale().getLanguage());

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

    @Override
    protected void onDetach() {
        for (CKEditorPanelBehavior behavior : behaviors) {
            behavior.detach();
        }
        super.onDetach();
    }

}
