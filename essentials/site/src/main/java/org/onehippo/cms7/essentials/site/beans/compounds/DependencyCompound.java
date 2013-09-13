/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
 */

package org.onehippo.cms7.essentials.site.beans.compounds;

import org.hippoecm.hst.content.beans.Node;
import org.onehippo.cms7.essentials.site.beans.BaseDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @version "$Id: DependencyCompound.java 157469 2013-03-08 14:03:30Z mmilicevic $"
 */
@Node(jcrType = "hippoplugins:dependency")
public class DependencyCompound extends BaseDocument {

    private static Logger log = LoggerFactory.getLogger(DependencyCompound.class);

    public String getArtifactId() {
        return getProperty("hippoplugins:artifactid");
    }

    public String getGroupId() {
        return getProperty("hippoplugins:groupid");
    }

    public String getScope() {
        return getProperty("hippoplugins:scope");
    }

    public String getProjectType() {
        return getProperty("hippoplugins:projecttype");
    }

    public String getDependencyType() {
        return getProperty("hippoplugins:dependencytype");
    }


    public String getVersion() {
        return getProperty("hippoplugins:version");
    }

}
