/*
 *  Copyright 2008-2018 Hippo B.V. (http://www.onehippo.com)
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

import java.util.List;

import org.apache.wicket.Component;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.StringResourceModel;
import org.hippoecm.frontend.model.nodetypes.JcrNodeTypeModel;

public class TypeChoiceRenderer implements IChoiceRenderer<String> {

    private static final long serialVersionUID = 1L;

    private Component component;

    public TypeChoiceRenderer(Component component) {
        this.component = component;
    }

    @Override
    public Object getDisplayValue(final String type) {
        JcrNodeTypeModel nodeTypeModel = new JcrNodeTypeModel(type);
        if (nodeTypeModel.getObject() != null) {
            return new TypeTranslator(nodeTypeModel).getTypeName().getObject();
        } else {
            return new StringResourceModel(type, component, null, type).getString();
        }
    }

    @Override
    public String getIdValue(final String type, final int index) {
        return type;
    }

    @Override
    public String getObject(final String id, final IModel<? extends List<? extends String>> choicesModel) {
        final List<? extends String> choices = choicesModel.getObject();
        return choices.stream().filter(choice -> choice.equals(id)).findFirst().orElse(null);
    }
}
