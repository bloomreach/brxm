package org.onehippo.repository.l10n;

import java.util.Map;

import org.apache.commons.lang.LocaleUtils;
import org.junit.Test;
import org.onehippo.repository.testutils.RepositoryTestCase;

import static javax.jcr.ImportUUIDBehavior.IMPORT_UUID_COLLISION_THROW;
import static org.hippoecm.repository.api.HippoNodeType.NT_RESOURCEBUNDLES;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class ResourceBundleLoaderTest extends RepositoryTestCase {

    @Override
    public void setUp() throws Exception {
        super.setUp();
        session.getRootNode().addNode("test", NT_RESOURCEBUNDLES);
        session.importXML("/test", getClass().getResourceAsStream("test-translations.xml"), IMPORT_UUID_COLLISION_THROW);
        session.save();
    }

    @Test
    public void testLoadBundles() throws Exception {
        final Map<ResourceBundleKey, ResourceBundle> bundles = ResourceBundleLoader.load(session.getNode("/test"));
        assertEquals("Expected two bundles to be present", 2, bundles.size());
        final ResourceBundle en = bundles.get(new ResourceBundleKey("foo.bar", LocaleUtils.toLocale("en")));
        assertNotNull(en);
        final ResourceBundle nl = bundles.get(new ResourceBundleKey("foo.bar", LocaleUtils.toLocale("nl")));
        assertNotNull(nl);
        assertEquals("value", en.getString("key"));
        assertEquals("waarde", nl.getString("key"));
    }
}
