/*
 *  Copyright 2017-2018 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.plugins.richtext.htmlprocessor;

import org.apache.wicket.model.IModel;
import org.onehippo.cms7.services.htmlprocessor.model.Model;

public class WicketModel<T> implements Model<T> {

    private final IModel<T> model;

    public WicketModel(final IModel<T> model) {
        this.model = model == null ? new org.apache.wicket.model.Model() : model;
    }

    @Override
    public T get() {
        return model.getObject();
    }

    @Override
    public void set(final T value) {
        model.setObject(value);
    }

    public static <T> Model<T> of(final IModel<T> wicketModel) {
        return new WicketModel<>(wicketModel);
    }
}
