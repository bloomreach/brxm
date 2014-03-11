/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
 */

package org.onehippo.cms7.essentials.dashboard.utils;

import java.net.URL;
import java.util.List;

import org.apache.maven.model.Dependency;
import org.junit.Test;
import org.onehippo.cms7.essentials.BaseResourceTest;
import org.onehippo.cms7.essentials.dashboard.model.DependencyType;

import static org.junit.Assert.assertTrue;

/**
 * @version "$Id$"
 */
public class ProjectUtilsTest extends BaseResourceTest {


    @Test
    public void testSitePackages() throws Exception {
        final List<String> sitePackages = ProjectUtils.getSitePackages(getContext());
        assertTrue(sitePackages.size() > 8);
        assertTrue(sitePackages.contains("org"));
        assertTrue(sitePackages.contains("org.dummy"));

    }

    @Test
    public void testDependencies() throws Exception {
        final URL resource = getClass().getResource("/project");
        Dependency dependency = new Dependency();
        dependency.setGroupId("org.onehippo.cms7.essentials");
        dependency.setArtifactId("hippo-components-plugin-components");
        System.setProperty(EssentialConst.PROJECT_BASEDIR_PROPERTY, resource.getPath());
        boolean installed = ProjectUtils.isInstalled(DependencyType.SITE, dependency);
        assertTrue(!installed);
        dependency.setGroupId("org.onehippo.cms7.hst.dependencies");
        dependency.setArtifactId("hst-server-dependencies");
        installed = ProjectUtils.isInstalled(DependencyType.SITE, dependency);
        assertTrue(installed);


    }


}
