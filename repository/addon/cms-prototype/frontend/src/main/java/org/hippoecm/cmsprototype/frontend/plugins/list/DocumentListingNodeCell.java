/*
 * Copyright 2008 Hippo
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
package org.hippoecm.cmsprototype.frontend.plugins.list;

import javax.jcr.RepositoryException;

import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.hippoecm.cmsprototype.frontend.model.content.Folder;
import org.hippoecm.cmsprototype.frontend.plugins.generic.list.NodeCell;
import org.hippoecm.frontend.model.NodeModelWrapper;
import org.hippoecm.frontend.plugin.channel.Channel;
import org.hippoecm.repository.api.HippoNode;

public class DocumentListingNodeCell extends NodeCell {
    private static final long serialVersionUID = 1L;

    public DocumentListingNodeCell(String id, NodeModelWrapper model, Channel channel, String nodePropertyName) {
        super(id, model, channel, nodePropertyName);
    }

    @Override
    protected void addLabel(NodeModelWrapper model, String nodePropertyName, AjaxLink link) {
        if (model instanceof DocumentListingParentFolder && nodePropertyName.equals("name")) {
            addLabel(link, "[..]");
        }
        else if (model instanceof Folder) {
            if (nodePropertyName.equals("name")) {
                HippoNode n = (HippoNode) model.getObject();
                try {
                    addLabel(link, "["+n.getName()+"]");
                } catch (RepositoryException e) {
                    addEmptyLabel(link);
                }
            }
            else if (nodePropertyName.equals("jcr:primaryType")) {
                addLabel(link, "Folder");
            }
            else {
                addEmptyLabel(link);
            }
        }
        else {
            super.addLabel(model, nodePropertyName, link);
        }
    }

    
    
}
