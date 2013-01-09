/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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

package org.hippoecm.frontend.plugins.yui.dragdrop;

import org.apache.wicket.Component;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.i18n.model.NodeTranslator;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NodeDragBehavior extends DragBehavior {

    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(NodeDragBehavior.class);

    final JcrNodeModel nodeModel;

    public NodeDragBehavior(DragSettings settings, JcrNodeModel nodeModel) {
        super(settings);
        this.nodeModel = nodeModel;
    }

    @Override
    protected void updateAjaxSettings() {
        super.updateAjaxSettings();
        dragSettings.setLabel(getLabel());
    }

    @Override
    protected IModel getDragModel() {
        return nodeModel;
    }

    protected String getLabel() {
        return (String) new NodeTranslator(nodeModel).getNodeName().getObject();
    }

    @Override
    public void detach(Component component) {
        super.detach(component);
        this.nodeModel.detach();
    }

}
