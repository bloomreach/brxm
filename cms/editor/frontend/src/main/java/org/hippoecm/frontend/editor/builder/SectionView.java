/*
 *  Copyright 2012-2015 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.editor.builder;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.IDataProvider;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.plugins.standards.icon.HippoIcon;
import org.hippoecm.frontend.skin.Icon;
import org.hippoecm.frontend.widgets.AbstractView;

public abstract class SectionView<T> extends AbstractView<T> {

    public SectionView(String id, IDataProvider<T> provider) {
        super(id, provider);
    }

    abstract void onClickItem(T object);

    abstract IModel<String> getNameModel(T object);

    @Override
    public void populateItem(Item<T> item) {
        final T type = item.getModelObject();
        AjaxLink<Void> link = new AjaxLink<Void>("template") {
            @Override
            public void onClick(AjaxRequestTarget target) {
                onClickItem(type);
            }
        };
        link.add(new Label("template-name", getNameModel(type)));
        link.add(HippoIcon.fromSprite("icon", Icon.GEAR));
        item.add(link);
    }

    @Override
    public void destroyItem(Item<T> item) {
        // nothing
    }
}
