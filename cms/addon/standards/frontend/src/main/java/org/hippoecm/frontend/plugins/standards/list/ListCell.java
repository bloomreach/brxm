/*
 *  Copyright 2008 Hippo.
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
package org.hippoecm.frontend.plugins.standards.list;

import javax.jcr.RepositoryException;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugins.standards.list.resolvers.IListAttributeModifier;
import org.hippoecm.frontend.plugins.standards.list.resolvers.IListCellRenderer;
import org.hippoecm.frontend.plugins.standards.list.resolvers.NameRenderer;
import org.hippoecm.frontend.service.render.RenderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ListCell extends Panel {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(ListCell.class);

    /**
     * A ListCell constructed with a ListCellAction performs a custom operation when clicked on.
     * Implement this interface if you want a ListCell that does something else then the
     * default behavior. 
     */
    public interface IListCellAction {
        void onClick(IModel model, AjaxRequestTarget target);
    }

    public ListCell(String id, final IModel model, IListCellRenderer renderer,
            IListAttributeModifier attributeModifier, final IListCellAction action) {
        super(id, model);

        add(new AjaxEventBehavior("onclick") {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onEvent(AjaxRequestTarget target) {
                RenderService plugin = (RenderService) findParent(RenderService.class);
                try {
                    if (action == null && model instanceof JcrNodeModel) {
                        JcrNodeModel nodeModel = (JcrNodeModel) model;
                        if (nodeModel.getNode().getParent() != null) {
                            plugin.setModel(model);
                            return;
                        }
                    } else {
                        action.onClick(model, target);
                    }
                } catch (RepositoryException ex) {
                }
            }
        });

        if (renderer == null) {
            add(new NameRenderer().getRenderer("renderer", model));
        } else {
            add(renderer.getRenderer("renderer", model));
        }

        if (attributeModifier != null) {
            AttributeModifier cellModifier = attributeModifier.getCellAttributeModifier(model);
            if (cellModifier != null) {
                add(cellModifier);
            }
        }
    }

}
