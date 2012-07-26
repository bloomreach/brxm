/*
 *  Copyright 2010 Hippo.
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
package org.hippoecm.frontend.plugins.standards.list.resolvers;

import java.io.Serializable;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.model.IModel;

/**
 * Attribute modifier for listing plugins.  These attributes will be added to each cell in the grid.
 * Column attribute modifiers alter the attributes independently of the row, cell attributes take
 * the model for the row into account.
 * 
 * @param <T> model type of the grid
 */
public abstract class AbstractListAttributeModifier<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Attribute modifiers for an individual cells in the grid.
     * The array may be null and each of the modifiers in the array can be null, too.
     * 
     * @param model for the row being shown
     * @return an array of attribute modifiers
     */
    public AttributeModifier[] getCellAttributeModifiers(IModel<T> model) {
        return null;
    }

    /**
     * Attribute modifiers for a column in the grid.
     * The array may be null and each of the modifiers in the array can be null, too.
     * 
     * @return an array of attribute modifiers
     */
    public AttributeModifier[] getColumnAttributeModifiers() {
        return null;
    }

}
