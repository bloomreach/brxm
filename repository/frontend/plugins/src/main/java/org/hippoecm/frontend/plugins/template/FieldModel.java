/*
 * Copyright 2007 Hippo
 *
 * Licensed under the Apache License, Version 2.0 (the  "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.frontend.plugins.template;

import javax.jcr.Item;

import org.hippoecm.frontend.model.ItemModelWrapper;

public class FieldModel extends ItemModelWrapper {
    private static final long serialVersionUID = 1L;

    private FieldDescriptor descriptor;

    //  Constructor
    public FieldModel(FieldDescriptor descriptor, Item item) {
        super(item);
        this.descriptor = descriptor;
    }

    public FieldModel(FieldDescriptor descriptor, String path) {
        super(path);
        this.descriptor = descriptor;
    }

    public FieldDescriptor getDescriptor() {
        return descriptor;
    }
}
