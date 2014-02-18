package org.onehippo.cms7.essentials.xinha;

import java.io.File;
import java.net.URL;
import java.nio.file.Path;

import org.junit.Test;
import org.onehippo.cms7.essentials.dashboard.utils.XmlUtils;
import org.onehippo.cms7.essentials.dashboard.utils.xml.XmlNode;
import org.onehippo.cms7.essentials.dashboard.utils.xml.XmlProperty;

import static org.junit.Assert.assertTrue;

/**
 * @version "$Id$"
 */
public class XinhaTest {


    @Test
    public void testXML() throws Exception {
        final URL resource = getClass().getResource("/root.xml");
        final File file = new File(resource.getFile());
        final Path path = file.toPath();
        final XmlNode xmlNode = XmlUtils.parseXml(path);
        final XmlProperty toolbar = xmlNode.getXmlPropertyByName("Xinha.config.toolbar");
        assertTrue(toolbar.getValues().size() == 36);
    }
}
