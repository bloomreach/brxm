/*
 *  Copyright 2010-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.plugins.standards.diff;

import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;

public class TextDiffModel extends LoadableDetachableModel<String> {
    private static final long serialVersionUID = 1L;


    TextDiffer differ = new TextDiffer();

    IModel<String> original;
    IModel<String> current;

    public TextDiffModel(IModel<String> original, IModel<String> current) {
        this.original = original;
        this.current = current;
    }

    @Override
    protected String load() {
        if (original == null || current == null) {
            return null;
        }
        return differ.diffText(original.getObject(), current.getObject());
    }

    @Override
    public void detach() {
        if (original != null) {
            original.detach();
        }
        if (current != null) {
            current.detach();
        }
        super.detach();
    }
}
