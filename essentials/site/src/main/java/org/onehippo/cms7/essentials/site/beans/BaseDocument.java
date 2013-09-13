package org.onehippo.cms7.essentials.site.beans;

import org.hippoecm.hst.content.beans.Node;
import org.hippoecm.hst.content.beans.standard.HippoDocument;

@Node(jcrType = "hippoplugins:basedocument")
public class BaseDocument extends HippoDocument {


    public String getTitle() {
        return getProperty("hippoplugins:title");
    }

    public String getDescription() {
        return getProperty("hippoplugins:description");
    }
}
