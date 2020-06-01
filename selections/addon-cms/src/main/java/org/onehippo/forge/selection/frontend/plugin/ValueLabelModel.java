/*
 * Copyright 2010-2013 Hippo B.V. (http://www.onehippo.com)
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/

package org.onehippo.forge.selection.frontend.plugin;

import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.onehippo.forge.selection.frontend.model.ValueList;

final class ValueLabelModel extends LoadableDetachableModel<String> {
    private static final long serialVersionUID = 1L;

    private final IModel<String> value;
    private final ValueList valueList;

    ValueLabelModel(ValueList valueList, IModel<String> value) {
        this.valueList = valueList;
        this.value = value;
    }

    @Override
    protected String load() {
        return valueList.getLabel(value.getObject());
    }

    @Override
    public void detach() {
        value.detach();
        super.detach();
    }
}
