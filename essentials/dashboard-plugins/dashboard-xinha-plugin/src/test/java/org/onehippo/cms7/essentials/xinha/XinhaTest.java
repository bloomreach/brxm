package org.onehippo.cms7.essentials.xinha;

import java.io.File;
import java.net.URL;
import java.nio.file.Path;
import java.util.Collection;

import org.junit.Test;
import org.onehippo.cms7.essentials.dashboard.utils.XmlUtils;
import org.onehippo.cms7.essentials.dashboard.utils.xml.XmlNode;
import org.onehippo.cms7.essentials.dashboard.utils.xml.XmlProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertTrue;

/**
 * @version "$Id$"
 */
public class XinhaTest {

    private static Logger log = LoggerFactory.getLogger(XinhaTest.class);

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
