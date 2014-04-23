/*
 * Copyright 2014 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
