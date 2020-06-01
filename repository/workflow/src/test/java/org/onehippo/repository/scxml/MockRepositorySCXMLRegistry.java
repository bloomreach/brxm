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

import org.onehippo.repository.mock.MockNode;

public class MockRepositorySCXMLRegistry extends RepositorySCXMLRegistry {

    public MockNode createConfigNode() throws Exception {
        MockNode scxmlConfigNode = MockNode.root().addNode("hippo:moduleconfig", "nt:unstructured");
        scxmlConfigNode.addNode(SCXML_DEFINITIONS, SCXML_DEFINITIONS);
        return scxmlConfigNode;
    }

    public MockNode addScxmlNode(MockNode configNode, String name, String source) throws Exception {
        MockNode scxmlDefsNode = (MockNode) configNode.getNode(SCXML_DEFINITIONS);
        MockNode scxmlDefNode = scxmlDefsNode.addNode(name, NT_SCXML);
        scxmlDefNode.setProperty(SCXML_SOURCE, source);
        return scxmlDefNode;
    }

    public MockNode addCustomAction(MockNode scxmlDefNode, String namespace, String actionName, String className) throws Exception {
        MockNode actionNode = scxmlDefNode.addNode(actionName, SCXML_ACTION);
        actionNode.setProperty(SCXML_ACTION_NAMESPACE, namespace);
        actionNode.setProperty(SCXML_ACTION_CLASSNAME, className);
        return actionNode;
    }

    public void setUp(MockNode configNode) throws Exception {
        reconfigure(configNode);
        initialize();
    }
}
