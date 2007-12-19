package org.hippoecm.repository.frontend.wysiwyg;

import org.apache.wicket.Page;
import org.apache.wicket.ResourceReference;
import org.apache.wicket.behavior.HeaderContributor;
import org.hippoecm.repository.frontend.wysiwyg.texteditor.TextEditor;
import org.hippoecm.repository.frontend.wysiwyg.xinha.XinhaEditor;
import org.hippoecm.repository.frontend.wysiwyg.xinha.XinhaEditorConfigurationBehaviour;

public class HtmlEditorFactory {
    private static XinhaEditorConfigurationBehaviour configurationBehaviour = null;

    private HtmlEditorFactory() {

    }

    public static HtmlEditor createHtmlEditor(final String id, Page page, String type) {
        if (type.equals("xinha")) {

            if (configurationBehaviour == null) {
                configurationBehaviour = XinhaEditorConfigurationBehaviour.getInstance((String) page
                        .urlFor(new ResourceReference(XinhaEditor.class, "impl/")));
                page.add(configurationBehaviour);
            }

            return new XinhaEditor(id, configurationBehaviour);
        } else if (type.equals("texteditor")) {
            return new TextEditor(id);
        }

        return null;
    }

}