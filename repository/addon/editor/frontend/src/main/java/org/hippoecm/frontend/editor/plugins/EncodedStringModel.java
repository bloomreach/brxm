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
package org.hippoecm.frontend.editor.plugins;

import org.apache.wicket.model.IModel;
import org.apache.wicket.util.string.Strings;

public class EncodedStringModel implements IModel<String> {
    private static final long serialVersionUID = 1L;

    private IModel<String> decorated;
    
    public EncodedStringModel(IModel<String> decorated) {
        this.decorated = decorated;
    }

    public String getObject() {
        return decorated.getObject();
    }

    public void setObject(String object) {
        decorated.setObject(Strings.escapeMarkup(object).toString());
    }

    public void detach() {
        decorated.detach();
    }

}
