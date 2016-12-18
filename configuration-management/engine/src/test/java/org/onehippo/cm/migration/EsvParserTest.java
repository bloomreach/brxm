/*
 *  Copyright 2016-2017 Hippo B.V. (http://www.onehippo.com)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.onehippo.cm.migration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Base64;

import javax.jcr.PropertyType;

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class EsvParserTest {

    @Test
    public void test_esv_kitchen_sink() throws IOException {
        File file = new File("src/test/resources/migration/hippoecm-extension.xml");
        File baseDir = file.getParentFile();
        EsvParser esvParser = new EsvParser(baseDir);
        EsvNode rootNode = esvParser.parse(Esv2Yaml.createReader(file), file.getCanonicalPath());
        assertNotNull(rootNode);
        EsvNode treeImagesNode = rootNode.getChildren().get(9);
        EsvNode assetNode = treeImagesNode.getChildren().get(0);
        EsvNode resourceNode = assetNode.getChildren().get(0);
        EsvProperty dataProperty = resourceNode.getProperties().get(0);
        assertEquals("jcr:data",dataProperty.getName());
        assertEquals(PropertyType.BINARY,dataProperty.getType());
        String dataValue = dataProperty.getValues().get(0).getValue();
        byte[] data = Base64.getDecoder().decode(dataValue);
        byte[] src = Files.readAllBytes(new File("src/test/resources/migration/tree-images.png").toPath());
        assertArrayEquals(data,src);
    }
}
