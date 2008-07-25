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

import java.util.Comparator;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.hippoecm.frontend.plugins.standards.list.ListCell.IListCellAction;
import org.hippoecm.frontend.plugins.standards.list.resolvers.IListCellAttributeModifier;
import org.hippoecm.frontend.plugins.standards.list.resolvers.IListCellRenderer;

public class ListColumn extends AbstractColumn {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    private Comparator comparator;
    private IListCellRenderer renderer;
    private IListCellAttributeModifier attributeModifier;
    private IListCellAction action;

    public ListColumn(IModel displayModel, String sortProperty) {
        super(displayModel, sortProperty);
    }

    public void setComparator(Comparator comparator) {
        this.comparator = comparator;
    }

    public Comparator getComparator() {
        return comparator;
    }

    public void setRenderer(IListCellRenderer renderer) {
        this.renderer = renderer;
    }

    public IListCellRenderer getRenderer() {
        return renderer;
    }

    public void setAttributeModifier(IListCellAttributeModifier attributeModifier) {
        this.attributeModifier = attributeModifier;
    }

    public IListCellAttributeModifier getAttributeModifier() {
        return attributeModifier;
    }

    public void setAction(IListCellAction action) {
        this.action = action;
    }

    public IListCellAction getAction() {
        return action;
    }

    public void populateItem(Item item, String componentId, IModel model) {
        if (getSortProperty().equals("icon")) {
            item.add(new AttributeModifier("class", true, new Model("icon-16")));
        }
        item.add(new ListCell(componentId, model, renderer, attributeModifier, action));
    }

}
