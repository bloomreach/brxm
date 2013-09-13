/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
 */

package org.onehippo.cms7.essentials.site.beans;

import org.hippoecm.hst.content.beans.Node;
import org.hippoecm.hst.content.beans.standard.HippoGalleryImageSet;
import org.hippoecm.hst.content.beans.standard.HippoGalleryImageSetBean;

/**
 * @version "$Id: VendorDocument.java 157454 2013-03-08 13:14:30Z mmilicevic $"
 */
@Node(jcrType = "hippoplugins:vendor")
public class VendorDocument extends BaseDocument {

    public String getUrl() {
        return getProperty("hippoplugins:url");

    }

    public String getVendorName() {
        return getProperty("hippoplugins:name");
    }

    public HippoGalleryImageSetBean getLogo() {
        return getLinkedBean("hippoplugins:logo", HippoGalleryImageSet.class);
    }


}
