/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
 */

package org.onehippo.cms7.essentials.site.beans;

import java.util.List;

import org.hippoecm.hst.content.beans.Node;
import org.onehippo.cms7.essentials.site.beans.compounds.DependencyCompound;

/**
 * @version "$Id: VersionDocument.java 157469 2013-03-08 14:03:30Z mmilicevic $"
 */
@Node(jcrType = "hippoplugins:version")
public class VersionDocument extends BaseDocument {


    public String getVersion() {
        return getProperty("hippoplugins:version");
    }


    public List<DependencyCompound> getDependencies() {
        return getChildBeans(DependencyCompound.class);
    }

}
