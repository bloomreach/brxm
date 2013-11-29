/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onehippo.repository.scxml;

import java.util.HashMap;
import java.util.Map;

import org.onehippo.repository.mock.MockNode;

public class MockRepositorySCXMLRegistry extends RepositorySCXMLRegistry {

    public void setup(String scxmlId, String scxmlDefinition) throws Exception {
        HashMap<String, String> definitions = new HashMap<>();
        definitions.put(scxmlId, scxmlDefinition);
        setup(definitions);
    }

    public void setup(Map<String, String> definitions) throws Exception {
        MockNode scxmlConfigNode = MockNode.root().addMockNode("hippo:moduleconfig", "nt:unstructured");
        MockNode scxmlDefsNode = scxmlConfigNode.addMockNode("hipposcxml:definitions", "hipposcxml:definitions");
        for (Map.Entry<String, String> entry : definitions.entrySet()) {
            MockNode scxmlDefNode = scxmlDefsNode.addMockNode(entry.getKey(), "hipposcxml:scxml");
            scxmlDefNode.setProperty("hipposcxml:source", entry.getValue());
        }
        reconfigure(scxmlConfigNode);
        initialize();
    }
}
