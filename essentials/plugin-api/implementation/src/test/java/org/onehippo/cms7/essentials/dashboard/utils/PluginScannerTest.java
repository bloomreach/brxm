/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
 */

package org.onehippo.cms7.essentials.dashboard.utils;

import java.io.StringReader;
import java.io.StringWriter;
import java.net.URL;
import java.util.List;

import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import org.junit.Test;
import org.onehippo.cms7.essentials.dashboard.Plugin;
import org.onehippo.cms7.essentials.dashboard.config.Asset;
import org.onehippo.cms7.essentials.dashboard.config.Screenshot;
import org.onehippo.cms7.essentials.dashboard.model.EssentialsPlugin;
import org.onehippo.cms7.essentials.dashboard.model.PluginAsset;
import org.onehippo.cms7.essentials.dashboard.model.PluginScreenshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertEquals;


/**
 * @version "$Id: PluginScannerTest.java 167907 2013-06-17 08:34:55Z mmilicevic $"
 */
public class PluginScannerTest {

    public static final String FILE_NAME = "/plugin-example.jar";
    private static Logger log = LoggerFactory.getLogger(PluginScannerTest.class);

    @Test
    public void testScan() throws Exception {

        final PluginScanner scanner = new PluginScanner();
        final URL resource = getClass().getResource(FILE_NAME);
        final String file = resource.getFile();
        final String directory = file.substring(0, file.length() - FILE_NAME.length());
        final List<Plugin> scan = scanner.scan(directory);
        assertEquals(1, scan.size());
        final Plugin plugin = scan.get(0);
        assertEquals("Gallery Plugin", plugin.getName());
        assertEquals("org.onehippo.cms7.essentials.dashboard.gallery.GalleryPlugin", plugin.getPluginClass());
    }

    @Test
    public void testMarshalling() throws Exception {
        final Plugin plugin = new EssentialsPlugin();
        plugin.setName("Gallery EssentialsPlugin");
        plugin.setPluginClass("org.onehippo.plugins.dashboard.gallery.GalleryPlugin");
        plugin.setDocumentationLink("http://documentation.link.com");
        plugin.setDescription("EssentialsPlugin Description");
        plugin.setVendor("Onehippo");
        plugin.setVendorLink("http://www.onehippo.com");
        plugin.setIssuesLink("http://issues.onehippo.com");
        plugin.setType("assets");
        final Screenshot screenShot = new PluginScreenshot("test.jpg");
        plugin.addScreenShot(screenShot);
        final Asset asset = new PluginAsset("myId", "http://www.onehippo.org", EssentialConst.MIME_TEXT_PLAIN);
        asset.setData("<myXml></myXml>");
        plugin.addAsset(asset);

        final StringWriter writer = new StringWriter();
        final Marshaller marshaller = PluginScanner.createMarshaller();
        marshaller.marshal(plugin, writer);
        log.info("writer {}", writer);
        final Unmarshaller unmarshaller = PluginScanner.createUnmarshaller();
        Plugin myPlugin = (Plugin) unmarshaller.unmarshal(new StringReader(writer.toString()));
        assertEquals(1, myPlugin.getScreenshots().size());
        assertEquals(1, myPlugin.getAssets().size());
        final Asset myAsset = myPlugin.getAssets().iterator().next();
        assertEquals("http://www.onehippo.org", myAsset.getUrl());
        assertEquals("myId", myAsset.getId());
        assertEquals("<myXml></myXml>", myAsset.getData());

    }
}
