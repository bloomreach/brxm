package org.hippoecm.frontend.plugins.yui.upload;

import org.apache.wicket.markup.html.panel.Panel;

public class AjaxMultiFileUploadComponent extends Panel {
    final static String SVN_ID = "$Id$";

    public AjaxMultiFileUploadComponent(String id, AjaxMultiFileUploadSettings settings) {
        super(id);
        setOutputMarkupId(true);

        add(new AjaxMultiFileUploadBehavior(settings));
    }
}
