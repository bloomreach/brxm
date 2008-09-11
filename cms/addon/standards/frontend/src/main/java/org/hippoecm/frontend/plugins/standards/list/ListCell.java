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

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.plugins.standards.list.datatable.ListDataTable;
import org.hippoecm.frontend.plugins.standards.list.resolvers.IListAttributeModifier;
import org.hippoecm.frontend.plugins.standards.list.resolvers.IListCellRenderer;
import org.hippoecm.frontend.plugins.standards.list.resolvers.NameRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class ListCell extends Panel {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";
    private static final long serialVersionUID = 1L;
    static final Logger log = LoggerFactory.getLogger(ListCell.class);

    public ListCell(String id, final IModel model, IListCellRenderer renderer, IListAttributeModifier attributeModifier) {
        super(id, model);

        add(new AjaxEventBehavior("onclick") {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onEvent(AjaxRequestTarget target) {
                ListDataTable dataTable = (ListDataTable) findParent(ListDataTable.class);
                dataTable.getSelectionListener().selectionChanged(model);
            }
        });

//        add(new AjaxEventBehavior("ondblclick") {
//            private static final long serialVersionUID = 1L;
//            @Override
//            protected void onEvent(AjaxRequestTarget target) {
//                String script = "var x = YAHOO.util.Dom.getElementsByClassName('edit_ico'); var y = x[0];y.onclick();this.blur()";
//                target.appendJavascript(script);
//            }
//        });

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
