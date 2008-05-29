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
package org.hippoecm.frontend.plugins.standards.sa.list;

import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.model.JcrNodeModel;

public class NodeColumn extends AbstractColumn {
    private static final long serialVersionUID = 1L;

    private String nodePropertyName;

    public NodeColumn(IModel displayModel, String nodePropertyName) {
        super(displayModel, nodePropertyName);
        this.nodePropertyName = nodePropertyName;
    }

    public void populateItem(Item item, String componentId, IModel model) {
        item.add(getNodeCell(componentId, model, nodePropertyName));
    }

    /**
     * Override this method to allow for a custom node cell implementation
     * @param componentId
     * @param model
     * @return NodeCell
     */
    protected NodeCell getNodeCell(String componentId, IModel model, String nodePropertyName) {
        return new NodeCell(componentId, (JcrNodeModel)model, nodePropertyName);
    }

    public String getNodePropertyName() {
        return nodePropertyName;
    }
    
}
