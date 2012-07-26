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

import javax.jcr.Item;

import org.apache.wicket.model.IChainingModel;
import org.apache.wicket.model.IModel;

/**
 * Base class for models that can provide objects based on an JCR {@link Item}.
 *
 * @param <T> the model object type
 */
public abstract class ItemModelWrapper<T> implements IChainingModel<T> {
    private static final long serialVersionUID = 1L;


    private JcrItemModel itemModel;

    public ItemModelWrapper(JcrItemModel model) {
        itemModel = model;
    }

    public ItemModelWrapper(Item item) {
        itemModel = new JcrItemModel(item);
    }

    public ItemModelWrapper(String path) {
        itemModel = new JcrItemModel(path);
    }

    public JcrItemModel getItemModel() {
        return itemModel;
    }

    // Implement IChainingModel

    @Override
    public IModel<Item> getChainedModel() {
        return itemModel;
    }

    @Override
    public void setChainedModel(IModel<?> model) {
        if (model instanceof JcrItemModel) {
            itemModel = (JcrItemModel) model;
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public T getObject() {
        return (T) itemModel.getObject();
    }

    @Override
    public void setObject(T object) {
        throw new UnsupportedOperationException("Cannot alter the item of an " + getClass());
    }

    @Override
    public void detach() {
        itemModel.detach();
    }

}
