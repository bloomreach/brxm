/*
 * Copyright 2007 Hippo
 *
 * Licensed under the Apache License, Version 2.0 (the  "License");
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
package org.hippocms.repository.webapp;

import javax.jcr.RepositoryException;

import org.apache.wicket.markup.html.WebPage;
import org.hippocms.repository.webapp.node.NodePanel;
import org.hippocms.repository.webapp.tree.TreePanel;

public class Browser extends WebPage {
    private static final long serialVersionUID = 1L;

    private NodePanel nodePanel;
    private TreePanel treePanel;

    public Browser() throws RepositoryException {
        nodePanel = new NodePanel("nodePanel");
        add(nodePanel);

        treePanel = new TreePanel("treePanel", "/");
        treePanel.addTreeStateListener(nodePanel.getTreeStateListener());
        add(treePanel);
    }

}
