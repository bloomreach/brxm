/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
 */

package org.onehippo.cms7.essentials.dashboard.utils.common;

import java.nio.file.Files;
import java.util.Collection;

import org.junit.Test;
import org.onehippo.cms7.essentials.BaseTest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertTrue;

/**
 * @version "$Id: PackageVisitorTest.java 174288 2013-08-19 16:21:19Z mmilicevic $"
 */
public class PackageVisitorTest extends BaseTest{

    private static Logger log = LoggerFactory.getLogger(PackageVisitorTest.class);

    @Test
    public void testGetPackages() throws Exception {
        final PackageVisitor visitor = new PackageVisitor();
        Files.walkFileTree(getContext().getSiteDirectory().toPath(), visitor);
        final Collection<String> packages = visitor.getPackages();
        assertTrue(packages.size() >= 8);

    }
}
