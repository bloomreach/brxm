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
package org.hippoecm.frontend.i18n.types;

import org.apache.wicket.Component;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.model.StringResourceModel;
import org.hippoecm.frontend.model.nodetypes.JcrNodeTypeModel;

public class TypeChoiceRenderer implements IChoiceRenderer {

    private static final long serialVersionUID = 1L;

    private Component component;

    public TypeChoiceRenderer(Component component) {
        this.component = component;
    }

    public Object getDisplayValue(Object object) {
        String type = (String) object;
        JcrNodeTypeModel nodeTypeModel = new JcrNodeTypeModel(type);
        if (nodeTypeModel.getObject() != null) {
            return new TypeTranslator(nodeTypeModel).getTypeName().getObject();
        } else {
            return new StringResourceModel(type, component, null, type).getString();
        }
    }

    public String getIdValue(Object object, int index) {
        return object.toString();
    }
}
