/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
 */

package org.onehippo.cms7.essentials.site.beans;

import org.hippoecm.hst.content.beans.Node;

/**
 * @version "$Id: PluginDocument.java 157469 2013-03-08 14:03:30Z mmilicevic $"
 */
@Node(jcrType = "hippoplugins:plugin")
public class PluginDocument extends BaseDocument {



    public String getPluginId() {
        return getProperty("hippoplugins:pluginid");
    }


    public VendorDocument getVendor(){
        return getLinkedBean("hippoplugins:vendor", VendorDocument.class);
    }
}
