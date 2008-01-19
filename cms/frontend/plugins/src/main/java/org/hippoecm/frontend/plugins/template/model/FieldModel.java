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
package org.hippoecm.frontend.plugins.template.model;

import org.hippoecm.frontend.model.ItemModelWrapper;
import org.hippoecm.frontend.model.JcrItemModel;
import org.hippoecm.frontend.plugins.template.config.FieldDescriptor;

public class FieldModel extends ItemModelWrapper {
    private static final long serialVersionUID = 1L;

    private FieldDescriptor descriptor;
    private int index;

    //  Constructor
    public FieldModel(FieldDescriptor descriptor, JcrItemModel model) {
        super(model);
        this.descriptor = descriptor;
        this.index = 0;
    }

    public FieldDescriptor getDescriptor() {
        return descriptor;
    }
    
    public void setIndex(int index) {
        this.index = index;
    }

    public JcrItemModel getChildModel() {
        String path = itemModel.getPath() + "/" + descriptor.getPath() + "[" + index + "]";
        return new JcrItemModel(path);
    }
}
