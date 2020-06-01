/*
 * Copyright 2016 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.hippoecm.frontend.model;

import java.io.Serializable;
import java.util.Map;

import org.apache.wicket.model.IModel;

/**
 * A model binding to a value object identified by the key object in the map
 *
 * @param <K> type of the key in the map
 * @param <V> type of the value object in the map
 */
public class KeyMapModel<K extends Serializable, V> implements IModel<V> {

    private final K key;
    private final IModel<? extends Map<K, V>> mapModel;

    public KeyMapModel(final IModel<? extends Map<K, V>> mapModel, final K key) {
        this.mapModel = mapModel;
        this.key = key;
    }

    @Override
    public V getObject() {
        return mapModel.getObject().get(key);
    }

    @Override
    public void setObject(final V object) {
        mapModel.getObject().put(key, object);
    }

    @Override
    public void detach() {
        mapModel.detach();
    }
}