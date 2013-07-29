package org.onehippo.cms7.ckeditor;

import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.markup.head.JavaScriptUrlReferenceHeaderItem;

/**
 * Bundle for the CKEditor sources.
 */
public class CKEditorConstants {

    private CKEditorConstants() {
    }

    /**
     * Header item to include the CKEditor sources. To be able to load the resources in a running container, a
     * {@code org.onehippo.cms7.utilities.servlet.ResourceServlet} should be configured that serves files from jars
     * that start with /ckeditor.
     */
    public static final JavaScriptUrlReferenceHeaderItem CKEDITOR_JS = JavaScriptHeaderItem.forUrl("ckeditor/ckeditor.js");

}
