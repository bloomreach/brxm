/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
 */

package org.onehippo.cms7.essentials.dashboard.utils;

import java.net.URL;
import java.util.List;

import org.apache.maven.model.Dependency;
import org.junit.Before;
import org.junit.Test;
import org.onehippo.cms7.essentials.BaseTest;
import org.onehippo.cms7.essentials.dashboard.DependencyType;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @version "$Id: ProjectUtilsTest.java 164012 2013-05-11 14:05:23Z mmilicevic $"
 */
public class ProjectUtilsTest extends BaseTest {


    @Test
    public void testSitePackages() throws Exception {
        final List<String> sitePackages = ProjectUtils.getSitePackages(getContext());
        assertEquals(9, sitePackages.size());
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
