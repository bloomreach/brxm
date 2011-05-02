/*
 *  Copyright 2011 Hippo.
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
package org.hippoecm.repository.export;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class LocationMapperTest {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id: ";

    @Test
    public void testLocationMapper() {
        // /hippo:namespaces
        String file = LocationMapper.fileForPath("/hippo:namespaces/test", true);
        assertEquals("namespaces/test.xml", file);
        String contextNode = LocationMapper.contextNodeForPath("/hippo:namespaces/test", true);
        assertEquals("/hippo:namespaces/test", contextNode);
        contextNode = LocationMapper.contextNodeForPath("/hippo:namespaces/test/prop", false);
        assertEquals("/hippo:namespaces/test", contextNode);
        contextNode = LocationMapper.contextNodeForPath("/hippo:namespaces/test/doctype", true);
        assertEquals("/hippo:namespaces/test/doctype", contextNode);
        contextNode = LocationMapper.contextNodeForPath("/hippo:namespaces/test/doctype/prop", false);
        assertEquals("/hippo:namespaces/test/doctype", contextNode);
        contextNode = LocationMapper.contextNodeForPath("/hippo:namespaces/test/doctype/subnode", true);
        assertEquals("/hippo:namespaces/test/doctype", contextNode);
        contextNode = LocationMapper.contextNodeForPath("/hippo:namespaces/test/doctype/subnode/prop", false);
        assertEquals("/hippo:namespaces/test/doctype", contextNode);

        // /hst:hst
        // /hst:hst/hst:sites
        file = LocationMapper.fileForPath("/hst:hst/hst:sites", true);
        assertEquals("hst/sites.xml", file);
        contextNode = LocationMapper.contextNodeForPath("/hst:hst/hst:sites", true);
        assertEquals("/hst:hst/hst:sites", contextNode);
        contextNode = LocationMapper.contextNodeForPath("/hst:hst/hst:sites/prop", false);
        assertEquals("/hst:hst/hst:sites", contextNode);
        contextNode = LocationMapper.contextNodeForPath("/hst:hst/hst:sites/subnode", true);
        assertEquals("/hst:hst/hst:sites", contextNode);
        contextNode = LocationMapper.contextNodeForPath("/hst:hst/hst:sites/subnode/prop", false);
        assertEquals("/hst:hst/hst:sites", contextNode);

        // /hst:hst/hst:hosts
        file = LocationMapper.fileForPath("/hst:hst/hst:hosts", true);
        assertEquals("hst/hosts.xml", file);
        contextNode = LocationMapper.contextNodeForPath("/hst:hst/hst:hosts", true);
        assertEquals("/hst:hst/hst:hosts", contextNode);
        contextNode = LocationMapper.contextNodeForPath("/hst:hst/hst:hosts/prop", false);
        assertEquals("/hst:hst/hst:hosts", contextNode);
        contextNode = LocationMapper.contextNodeForPath("/hst:hst/hst:hosts/subnode", true);
        assertEquals("/hst:hst/hst:hosts", contextNode);
        contextNode = LocationMapper.contextNodeForPath("/hst:hst/hst:hosts/subnode/prop", false);
        assertEquals("/hst:hst/hst:hosts", contextNode);

        // /hst:hst/hst:configurations
        file = LocationMapper.fileForPath("/hst:hst/hst:configurations", true);
        assertEquals("hst/configurations.xml", file);
        contextNode = LocationMapper.contextNodeForPath("/hst:hst/hst:configurations", true);
        assertEquals("/hst:hst/hst:configurations", contextNode);
        contextNode = LocationMapper.contextNodeForPath("/hst:hst/hst:configurations/prop", false);
        assertEquals("/hst:hst/hst:configurations", contextNode);
        contextNode = LocationMapper.contextNodeForPath("/hst:hst/hst:configurations/project", true);
        assertEquals("/hst:hst/hst:configurations", contextNode);
        contextNode = LocationMapper.contextNodeForPath("/hst:hst/hst:configurations/project/prop", false);
        assertEquals("/hst:hst/hst:configurations", contextNode);

        file = LocationMapper.fileForPath("/hst:hst/hst:configurations/project/hst:pages", true);
        assertEquals("hst/configurations/project/pages.xml", file);
        contextNode = LocationMapper.contextNodeForPath("/hst:hst/hst:configurations/project/hst:pages", true);
        assertEquals("/hst:hst/hst:configurations/project/hst:pages", contextNode);
        contextNode = LocationMapper.contextNodeForPath("/hst:hst/hst:configurations/project/hst:pages/prop", false);
        assertEquals("/hst:hst/hst:configurations/project/hst:pages", contextNode);
        contextNode = LocationMapper.contextNodeForPath("/hst:hst/hst:configurations/project/hst:pages/subnode", true);
        assertEquals("/hst:hst/hst:configurations/project/hst:pages", contextNode);
        contextNode = LocationMapper.contextNodeForPath("/hst:hst/hst:configurations/project/hst:pages/subnode/prop", true);
        assertEquals("/hst:hst/hst:configurations/project/hst:pages", contextNode);

        // hippo:configuration
        file = LocationMapper.fileForPath("/hippo:configuration", true);
        assertEquals("configuration.xml", file);
        contextNode = LocationMapper.contextNodeForPath("/hippo:configuration", true);
        assertEquals("/hippo:configuration", contextNode);
        contextNode = LocationMapper.contextNodeForPath("/hippo:configuration/prop", false);
        assertEquals("/hippo:configuration", contextNode);
        contextNode = LocationMapper.contextNodeForPath("/hippo:configuration/subnode", true);
        assertEquals("/hippo:configuration", contextNode);
        contextNode = LocationMapper.contextNodeForPath("/hippo:configuration/subnode/prop", false);
        assertEquals("/hippo:configuration", contextNode);
        
        file = LocationMapper.fileForPath("/hippo:configuration/subnode/subsubnode", true);
        assertEquals("configuration/subnode/subsubnode.xml", file);
        contextNode = LocationMapper.contextNodeForPath("/hippo:configuration/subnode/subsubnode", true);
        assertEquals("/hippo:configuration/subnode/subsubnode", contextNode);
        contextNode = LocationMapper.contextNodeForPath("/hippo:configuration/subnode/subsubnode/prop", false);
        assertEquals("/hippo:configuration/subnode/subsubnode", contextNode);
        contextNode = LocationMapper.contextNodeForPath("/hippo:configuration/subnode/subsubnode/subsubsubnode", true);
        assertEquals("/hippo:configuration/subnode/subsubnode", contextNode);
        contextNode = LocationMapper.contextNodeForPath("/hippo:configuration/subnode/subsubnode/subsubsubnode/prop", false);
        assertEquals("/hippo:configuration/subnode/subsubnode", contextNode);
        contextNode = LocationMapper.contextNodeForPath("/hippo:configuration/subnode/subsubnode/any/sub/node", true);
        assertEquals("/hippo:configuration/subnode/subsubnode", contextNode);
        contextNode = LocationMapper.contextNodeForPath("/hippo:configuration/subnode/subsubnode/any/sub/node/prop", false);
        assertEquals("/hippo:configuration/subnode/subsubnode", contextNode);

        // hippo:configuration/hippo:queries
        file = LocationMapper.fileForPath("/hippo:configuration/hippo:queries/hippo:templates", true);
        assertEquals("configuration/queries/templates.xml", file);
        contextNode = LocationMapper.contextNodeForPath("/hippo:configuration/hippo:queries/hippo:templates", true);
        assertEquals("/hippo:configuration/hippo:queries/hippo:templates", contextNode);
        contextNode = LocationMapper.contextNodeForPath("/hippo:configuration/hippo:queries/hippo:templates/prop", false);
        assertEquals("/hippo:configuration/hippo:queries/hippo:templates", contextNode);
        file = LocationMapper.fileForPath("/hippo:configuration/hippo:queries/hippo:templates/simple", true);
        assertEquals("configuration/queries/templates/simple.xml", file);
        contextNode = LocationMapper.contextNodeForPath("/hippo:configuration/hippo:queries/hippo:templates/simple", true);
        assertEquals("/hippo:configuration/hippo:queries/hippo:templates/simple", contextNode);
        contextNode = LocationMapper.contextNodeForPath("/hippo:configuration/hippo:queries/hippo:templates/simple/prop", false);
        assertEquals("/hippo:configuration/hippo:queries/hippo:templates/simple", contextNode);
        contextNode = LocationMapper.contextNodeForPath("/hippo:configuration/hippo:queries/hippo:templates/simple/subnode", true);
        assertEquals("/hippo:configuration/hippo:queries/hippo:templates/simple", contextNode);
        contextNode = LocationMapper.contextNodeForPath("/hippo:configuration/hippo:queries/hippo:templates/simple/subnode/prop", true);
        assertEquals("/hippo:configuration/hippo:queries/hippo:templates/simple", contextNode);
        
        // /content
        file = LocationMapper.fileForPath("/content", true);
        assertEquals("content.xml", file);
        contextNode = LocationMapper.contextNodeForPath("/content", true);
        assertEquals("/content", contextNode);
        contextNode = LocationMapper.contextNodeForPath("/content/prop", false);
        assertEquals("/content", contextNode);
        contextNode = LocationMapper.contextNodeForPath("/content/documents", true);
        assertEquals("/content", contextNode);
        contextNode = LocationMapper.contextNodeForPath("/content/documents/prop", false);
        assertEquals("/content", contextNode);

        file = LocationMapper.fileForPath("/content/documents/my-project", true);
        assertEquals("content/documents/my-project.xml", file);
        contextNode = LocationMapper.contextNodeForPath("/content/documents/myproject", true);
        assertEquals("/content/documents/myproject", contextNode);
        contextNode = LocationMapper.contextNodeForPath("/content/documents/myproject/prop", false);
        assertEquals("/content/documents/myproject", contextNode);

        file = LocationMapper.fileForPath("/content/documents/myproject/common", true);
        assertEquals("content/documents/myproject/common.xml", file);
        contextNode = LocationMapper.contextNodeForPath("/content/documents/myproject/common", true);
        assertEquals("/content/documents/myproject/common", contextNode);
        contextNode = LocationMapper.contextNodeForPath("/content/documents/myproject/common/prop", false);
        assertEquals("/content/documents/myproject/common", contextNode);
        contextNode = LocationMapper.contextNodeForPath("/content/documents/myproject/common/article", true);
        assertEquals("/content/documents/myproject/common", contextNode);
        contextNode = LocationMapper.contextNodeForPath("/content/documents/myproject/common/article/prop", false);
        assertEquals("/content/documents/myproject/common", contextNode);
        contextNode = LocationMapper.contextNodeForPath("/content/documents/myproject/common/article/subnode", true);
        assertEquals("/content/documents/myproject/common", contextNode);
        contextNode = LocationMapper.contextNodeForPath("/content/documents/myproject/common/article/subnode/prop", false);
        assertEquals("/content/documents/myproject/common", contextNode);
        contextNode = LocationMapper.contextNodeForPath("/content/documents/myproject/common/article/any/sub/node", true);
        assertEquals("/content/documents/myproject/common", contextNode);
        contextNode = LocationMapper.contextNodeForPath("/content/documents/myproject/common/article/any/sub/node/prop", false);
        assertEquals("/content/documents/myproject/common", contextNode);


        // catch all: /node
        file = LocationMapper.fileForPath("/node", true);
        assertEquals("node.xml", file);
        contextNode = LocationMapper.contextNodeForPath("/node", true);
        assertEquals("/node", contextNode);
        contextNode = LocationMapper.contextNodeForPath("/node/prop", false);
        assertEquals("/node", contextNode);
        contextNode = LocationMapper.contextNodeForPath("/node/subnode", true);
        assertEquals("/node", contextNode);
        contextNode = LocationMapper.contextNodeForPath("/node/subnode/prop", false);
        assertEquals("/node", contextNode);

        file = LocationMapper.fileForPath("/node/subnode/subsubnode", true);
        assertEquals("node/subnode/subsubnode.xml", file);
        contextNode = LocationMapper.contextNodeForPath("/node/subnode/subsubnode", true);
        assertEquals("/node/subnode/subsubnode", contextNode);
        contextNode = LocationMapper.contextNodeForPath("/node/subnode/subsubnode/prop", false);
        assertEquals("/node/subnode/subsubnode", contextNode);
        contextNode = LocationMapper.contextNodeForPath("/node/subnode/subsubnode/any/sub/node", true);
        assertEquals("/node/subnode/subsubnode", contextNode);
        contextNode = LocationMapper.contextNodeForPath("/node/subnode/subsubnode/any/sub/node/prop", false);
        assertEquals("/node/subnode/subsubnode", contextNode);

    }

}
