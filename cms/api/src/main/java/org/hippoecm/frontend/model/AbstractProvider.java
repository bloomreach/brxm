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
package org.hippoecm.frontend.model;

import java.util.Iterator;
import java.util.LinkedList;

import javax.jcr.Item;

import org.apache.wicket.markup.repeater.data.IDataProvider;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.validation.ModelPathElement;

/**
 * Provider that can turn a {@link JcrItemModel} into a {@link IDataProvider}.
 */
public abstract class AbstractProvider<T extends Item, M extends IModel> extends ItemModelWrapper<T> {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    protected transient LinkedList<M> elements = null;

    // Constructor

    public AbstractProvider(JcrItemModel itemModel) {
        super(itemModel);
    }

    public void refresh() {
        elements = null;
    }

    @Override
    public void setChainedModel(IModel model) {
        detach();
        super.setChainedModel(model);
    }

    @Override
    public void detach() {
        if (elements != null) {
            Iterator<M> iterator = elements.iterator();
            while (iterator.hasNext()) {
                iterator.next().detach();
            }
        }
        super.detach();
    }

    // IDataProvider implementation, provides the fields of the chained itemModel

    public Iterator<M> iterator(int first, int count) {
        load();
        return elements.subList(first, first + count).iterator();
    }

    public int size() {
        load();
        return elements.size();
    }

    public abstract void addNew();

    public abstract void remove(M model);

    public abstract void moveUp(M model);

    public abstract ModelPathElement getFieldElement(M model);

    protected abstract void load();
}
