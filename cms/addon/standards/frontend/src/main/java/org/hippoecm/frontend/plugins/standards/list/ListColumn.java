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

import org.hippoecm.frontend.plugins.standards.list.resolvers.IListAttributeModifier;
import org.hippoecm.frontend.plugins.standards.list.resolvers.IListCellRenderer;

/**
 * Definition of a column in a {@link ListDataTable}.  Can be used to define sorting,
 * cell renderers and attribute modifiers that will be applied to the repeater {@link Item}.
 * By default, the renderer used is the {@link NameRenderer}, that renders the (translated)
 * name of a JCR node.
 */
public class ListColumn extends AbstractColumn {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    private Comparator<IModel> comparator;
    private IListCellRenderer renderer;
    private IListAttributeModifier attributeModifier;

    public ListColumn(IModel displayModel, String sortProperty) {
        super(displayModel, sortProperty);
    }

    public void setComparator(Comparator<IModel> comparator) {
        this.comparator = comparator;
    }

    public Comparator<IModel> getComparator() {
        return comparator;
    }

    public void setRenderer(IListCellRenderer renderer) {
        this.renderer = renderer;
    }

    public IListCellRenderer getRenderer() {
        return renderer;
    }

    public void setAttributeModifier(IListAttributeModifier attributeModifier) {
        this.attributeModifier = attributeModifier;
    }

    public IListAttributeModifier getAttributeModifier() {
        return attributeModifier;
    }

    public void populateItem(Item item, String componentId, IModel model) {
        if (attributeModifier != null) {
            AttributeModifier[] columnModifiers = attributeModifier.getColumnAttributeModifiers(model);
            if (columnModifiers != null) {
                for(AttributeModifier columnModifier : columnModifiers) {
                    item.add(columnModifier);
                }
            }
        }
        item.add(new ListCell(componentId, model, renderer, attributeModifier));
    }
}
