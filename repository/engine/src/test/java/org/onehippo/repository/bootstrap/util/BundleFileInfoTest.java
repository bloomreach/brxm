package org.onehippo.repository.bootstrap.util;

import java.util.Locale;

import javax.jcr.Node;

import org.junit.Test;
import org.onehippo.repository.bootstrap.InitializeItem;
import org.onehippo.repository.mock.MockNode;

import static org.hippoecm.repository.api.HippoNodeType.HIPPO_RESOURCEBUNDLES;
import static org.hippoecm.repository.api.HippoNodeType.NT_INITIALIZEITEM;
import static org.junit.Assert.assertEquals;

public class BundleFileInfoTest {

    @Test
    public void readBundleFileInfo() throws Exception {
        final Node itemNode = MockNode.root().addNode("item", NT_INITIALIZEITEM);
        itemNode.setProperty(HIPPO_RESOURCEBUNDLES, getClass().getResource("/bootstrap/resourcebundle.json").toString());
        final InitializeItem initializeItem = new InitializeItem(itemNode);
        final BundleFileInfo bundleFileInfo = BundleFileInfo.readInfo(initializeItem);
        assertEquals(2, bundleFileInfo.getBundleInfos().size());
        final BundleInfo bundleInfo = bundleFileInfo.getBundleInfos().iterator().next();
        assertEquals(Locale.ENGLISH, bundleInfo.getLocale());
        assertEquals("foo.bar", bundleInfo.getName());
        assertEquals(1, bundleInfo.getTranslations().size());
        assertEquals("value", bundleInfo.getTranslations().get("key"));
    }

}
