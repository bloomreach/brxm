package org.onehippo.cms7.ckeditor;

import java.nio.charset.Charset;
import java.util.Arrays;

import org.apache.wicket.markup.head.HeaderItem;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.markup.head.JavaScriptUrlReferenceHeaderItem;
import org.apache.wicket.request.Url;
import org.apache.wicket.request.resource.ResourceReference;
import org.apache.wicket.request.resource.UrlResourceReference;

/**
 * Bundle for the CKEditor sources.
 */
public class CKEditorConstants {

    private CKEditorConstants() {
    }

    /**
     * Resource reference to the CKEditor source. To be able to load the CKEditor resources in a running container, a
     * {@code org.onehippo.cms7.utilities.servlet.ResourceServlet} should be configured that serves files from jars
     * that start with /ckeditor.
     */
    public static final ResourceReference CKEDITOR_JS = new UrlResourceReference(new Url(Arrays.asList("ckeditor", "ckeditor.js"), Charset.forName("UTF-8")));

}
