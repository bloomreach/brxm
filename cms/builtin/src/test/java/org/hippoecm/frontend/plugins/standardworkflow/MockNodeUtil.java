/*
 * Copyright 2015 Hippo B.V. (http://www.onehippo.com)
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

package org.hippoecm.frontend.plugins.standardworkflow;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.hippoecm.repository.HippoStdNodeType;
import org.hippoecm.repository.api.HippoNodeType;
import org.onehippo.repository.mock.MockNode;

public class MockNodeUtil {

    /**
     * Create a subfolder {@link HippoStdNodeType#NT_FOLDER} of <code>parentNode</code> with the same node name and localized name
     *
     */
    public static MockNode addFolder(final MockNode parentNode, final String nodeName) throws RepositoryException {
        return addFolder(parentNode, nodeName, nodeName);
    }

    /**
     * Create a subfolder {@link HippoStdNodeType#NT_FOLDER} of <code>parentNode</code> with specified node name and localized name
     */
    public static MockNode addFolder(final MockNode parentNode, final String nodeName, final String localizedName) throws RepositoryException {
        final MockNode node = parentNode.addNode(nodeName, HippoStdNodeType.NT_FOLDER);
        node.addMixin(HippoNodeType.NT_TRANSLATED);
        final Node translationNode = node.addNode(HippoNodeType.HIPPO_TRANSLATION, HippoNodeType.HIPPO_TRANSLATION);
        translationNode.setProperty(HippoNodeType.HIPPO_MESSAGE, localizedName);
        translationNode.setProperty(HippoNodeType.HIPPO_LANGUAGE, ""); // neutral language
        return node;
    }
}
