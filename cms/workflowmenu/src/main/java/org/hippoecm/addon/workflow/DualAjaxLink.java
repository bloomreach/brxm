/*
 *  Copyright 2009-2018 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.addon.workflow;

import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.attributes.AjaxRequestAttributes;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.model.IModel;

public abstract class DualAjaxLink<T> extends AjaxLink<T> {

    public DualAjaxLink(final String id) {
        this(id, null);
    }

    public DualAjaxLink(final String id, final IModel<T> model) {
        super(id, model);

        add(new AjaxEventBehavior("contextmenu") {

            protected void onEvent(final AjaxRequestTarget target) {
                onRightClick(target);
            }

            @Override
            protected void updateAjaxAttributes(final AjaxRequestAttributes attributes) {
                super.updateAjaxAttributes(attributes);
                attributes.setEventPropagation(AjaxRequestAttributes.EventPropagation.STOP_IMMEDIATE);
            }
        });
    }

    @Override
    protected void updateAjaxAttributes(final AjaxRequestAttributes attributes) {
        super.updateAjaxAttributes(attributes);

        attributes.setEventPropagation(AjaxRequestAttributes.EventPropagation.STOP_IMMEDIATE);
    }

    public void onRightClick(final AjaxRequestTarget target) {
        onClick(target);
    }

    public abstract void onClick(final AjaxRequestTarget target);
}
