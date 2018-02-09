/*
 * Copyright 2018 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onehippo.cm.model.mapper;

import org.junit.Test;
import org.onehippo.cm.model.impl.ModelTestUtils;
import org.onehippo.cm.model.impl.definition.ContentDefinitionImpl;
import org.onehippo.cm.model.impl.tree.DefinitionNodeImpl;

import static org.junit.Assert.assertEquals;

public class HippoResourceFileMapperTest {

    @Test
    public void test_hippo_namespace_prototype() throws Exception {
        final String yaml =
                "/hipposysedit:prototype:\n" +
                "  jcr:primaryType: hippo:resource\n" +
                "  hippo:filename: hippo:resource\n" +
                "  jcr:data:\n" +
                "    type: binary\n" +
                "    value: !!binary data\n" +
                "  jcr:encoding: UTF-8\n" +
                "  jcr:lastModified: 2008-03-26T12:03:00+01:00\n" +
                "  jcr:mimeType: application/vnd.hippo.blank";

        DefinitionNodeImpl resourceNode =
                ((ContentDefinitionImpl) ModelTestUtils.parseNoSort(yaml, false, false).get(0))
                        .getNode();

        final String filename = new HippoResourceFileMapper().apply(resourceNode.getProperty("jcr:data").getValue());
        assertEquals("/prototype/hippo-resource", filename);
    }

    @Test
    public void test_naughty_windows_chars() throws Exception {
        final String yaml =
                "/hipposysedit:prototype:\n" +
                "  jcr:primaryType: hippo:resource\n" +
                "  hippo:filename: \"hippo:<resource>*sss. . . \"\n" +
                "  jcr:data:\n" +
                "    type: binary\n" +
                "    value: !!binary data\n" +
                "  jcr:encoding: UTF-8\n" +
                "  jcr:lastModified: 2008-03-26T12:03:00+01:00\n" +
                "  jcr:mimeType: application/vnd.hippo.blank";

        DefinitionNodeImpl resourceNode =
                ((ContentDefinitionImpl) ModelTestUtils.parseNoSort(yaml, false, false).get(0))
                        .getNode();

        final String filename = new HippoResourceFileMapper().apply(resourceNode.getProperty("jcr:data").getValue());
        assertEquals("/prototype/hippo-resource-sss", filename);
    }

    @Test
    public void test_naughty_windows_names() throws Exception {
        final String yaml =
                "/COM1:\n" +
                        "  jcr:primaryType: hippo:resource\n" +
                        "  jcr:data:\n" +
                        "    type: binary\n" +
                        "    value: !!binary data\n" +
                        "  jcr:encoding: UTF-8\n" +
                        "  jcr:lastModified: 2008-03-26T12:03:00+01:00\n" +
                        "  jcr:mimeType: application/vnd.hippo.blank";

        DefinitionNodeImpl resourceNode =
                ((ContentDefinitionImpl) ModelTestUtils.parseNoSort(yaml, false, false).get(0))
                        .getNode();

        final String filename = new HippoResourceFileMapper().apply(resourceNode.getProperty("jcr:data").getValue());
        assertEquals("/data/data.bin", filename);
    }

}
