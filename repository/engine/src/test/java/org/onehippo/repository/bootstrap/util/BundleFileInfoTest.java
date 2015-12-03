package org.onehippo.repository.bootstrap.util;

import java.io.InputStream;
import java.util.Locale;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class BundleFileInfoTest {

    @Test
    public void readBundleFileInfo() throws Exception {
        try (final InputStream in = getClass().getResourceAsStream("/bootstrap/resourcebundle.json")) {
            final BundleFileInfo bundleFileInfo = BundleFileInfo.readInfo(in);
            assertEquals(2, bundleFileInfo.getBundleInfos().size());
            final BundleInfo bundleInfo = bundleFileInfo.getBundleInfos().iterator().next();
            assertEquals(Locale.ENGLISH, bundleInfo.getLocale());
            assertEquals("foo.bar", bundleInfo.getName());
            assertEquals(1, bundleInfo.getTranslations().size());
            assertEquals("value", bundleInfo.getTranslations().get("key"));
        }
    }

}
