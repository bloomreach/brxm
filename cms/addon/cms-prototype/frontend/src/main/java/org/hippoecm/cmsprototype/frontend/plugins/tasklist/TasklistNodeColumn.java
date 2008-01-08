/*
 * Copyright 2007-2008 Hippo
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
 
package org.hippoecm.cmsprototype.frontend.plugins.tasklist;

import org.apache.wicket.model.IModel;
import org.hippoecm.cmsprototype.frontend.plugins.list.NodeCell;
import org.hippoecm.cmsprototype.frontend.plugins.list.NodeColumn;
import org.hippoecm.frontend.model.NodeModelWrapper;
import org.hippoecm.frontend.plugin.channel.Channel;

public class TasklistNodeColumn extends NodeColumn{

    private static final long serialVersionUID = 1L;
    
     public TasklistNodeColumn(IModel displayModel, String nodePropertyName ,Channel channel) {
        super(displayModel, nodePropertyName, channel);
    }

    @Override
    protected NodeCell getNodeCell(String componentId, IModel model, String nodePropertyName) {
        return new TasklistNodeCell(componentId, (NodeModelWrapper) model, getChannel(), nodePropertyName);
    }


}
