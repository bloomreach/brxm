package org.onehippo.cms.l10n;

import java.io.IOException;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.junit.Assert.assertEquals;

public abstract class ResourceBundleTest {

    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test(expected = IOException.class)
    public void testEmptyBundleCannotBeSaved() throws Exception {
        createResourceBundle().save();
    }

    @Test
    public void testResourceBundleSerialization() throws Exception {
        final ResourceBundle resourceBundle = createResourceBundle();
        resourceBundle.getEntries().put("key", "value");
        resourceBundle.save();
        resourceBundle.load();
        assertEquals(resourceBundle.getEntries().get("key"), "value");
    }

    protected abstract ResourceBundle createResourceBundle(); 

}
