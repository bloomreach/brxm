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
package org.hippoecm.frontend.model;

import java.util.Iterator;
import java.util.LinkedList;

import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.validation.ModelPathElement;

/**
 * Provider that enumerates a list of models based on a {@link JcrItemModel}.
 * It can be used to uniformly manipulate such lists.
 */
public abstract class AbstractProvider<T, M extends IModel> extends ItemModelWrapper<T> {

    private static final long serialVersionUID = 1L;

    protected transient LinkedList<M> elements = null;
    private transient boolean attached = true;

    // Constructor

    public AbstractProvider(JcrItemModel itemModel) {
        super(itemModel);
    }

    /**
     * Force a reloading of items from the session.
     */
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
        if (elements != null && attached) {
            Iterator<M> iterator = elements.iterator();
            while (iterator.hasNext()) {
                iterator.next().detach();
            }
            attached = false;
        }
        super.detach();
    }

    /**
     * Iterate over the provided elements.
     *
     * @param first
     * @param count
     *
     * @return iterator over the elements
     */
    public Iterator<M> iterator(int first, int count) {
        load();
        return elements.subList(first, first + count).iterator();
    }

    /**
     * Count the number of elements
     *
     * @return the number of elements provided
     */
    public int size() {
        load();
        return elements.size();
    }

    /**
     * Add a new element.
     */
    public abstract void addNew();

    /**
     * Remove a model from the list.
     *
     * @param model the model to remove
     */
    public abstract void remove(M model);

    /**
     * Move a model up one position.
     *
     * @param model the model to move
     */
    public abstract void moveUp(M model);

    /**
     * Retrieve the {@link ModelPathElement} to access an element.
     *
     * @param model the element to be made accessible
     *
     * @return the ModelPathElement for the element
     */
    public abstract ModelPathElement getFieldElement(M model);

    protected final void load() {
        loadElements();
        attached = true;
    }

    /**
     * Method to be overridden by subclasses, to populate the list of elements.
     */
    protected abstract void loadElements();
}
