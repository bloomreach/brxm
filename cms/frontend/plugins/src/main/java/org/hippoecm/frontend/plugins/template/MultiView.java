/*
 * Copyright 2008 Hippo
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

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.repeater.Item;
import org.hippoecm.frontend.plugins.template.model.AbstractProvider;
import org.hippoecm.frontend.plugins.template.model.FieldModel;

public class MultiView extends FieldView {
    private static final long serialVersionUID = 1L;

    private MultiTemplate template;
    
    public MultiView(String wicketId, AbstractProvider provider, TemplateEngine engine, MultiTemplate template) {
        super(wicketId, provider, engine);
        this.template = template;
    }

    @Override
    protected void populateItem(Item item) {
        final FieldModel fieldModel = (FieldModel) item.getModel();
        item.add(engine.createTemplate("sub", fieldModel));
        item.add(new AjaxLink("remove") {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                template.onRemoveNode(fieldModel, target);
            }
        });
    }
}
