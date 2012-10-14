package org.hippoecm.repository.util;

import java.util.jar.Manifest;

import org.hippoecm.repository.HippoRepository;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class RepoUtilsTest {

    @Test
    public void testGetManifest() throws Exception {
        final Manifest manifest = RepoUtils.getManifest(HippoRepository.class);
        assertNotNull(manifest);
        assertEquals("Repository API", manifest.getMainAttributes().getValue("Implementation-Title"));
    }
}
