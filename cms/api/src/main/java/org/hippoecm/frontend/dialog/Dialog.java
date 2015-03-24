/*
 *  Copyright 2015 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.dialog;

import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.value.IValueMap;
import org.apache.wicket.util.value.ValueMap;

public class Dialog<T> extends AbstractDialog<T> {

    private IValueMap properties;
    private Component bottomLeft;

    public Dialog() {
        this(null);
    }

    public Dialog(final IModel<T> model) {
        super(model);

        add(bottomLeft = createBottomLeft("bottom-left"));
    }

    @Override
    public IModel<String> getTitle() {
        final String titleKey = getTitleKey();
        final IModel<?> titleModel = getTitleModel();
        final String title = getString(titleKey, titleModel, "Dialog title");
        return Model.of(title);
    }

    protected String getTitleKey() {
        return "dialog.title";
    }

    protected IModel<?> getTitleModel() {
        return null;
    }

    protected Component createBottomLeft(final String id) {
        return new EmptyPanel(id).setVisible(false);
    }

    protected Component getBottomLeft() {
        return bottomLeft;
    }

    @Override
    public IValueMap getProperties() {
        if (properties == null) {
            final String cssClass = getCssClass();
            if (StringUtils.isNotEmpty(cssClass)) {
                Map<String, Object> map = new LinkedHashMap<>(getSize());
                map.put("css-class-name", cssClass);
                properties = new ValueMap(map).makeImmutable();
            } else {
                properties = getSize();
            }
        }
        return properties;
    }

    protected String getCssClass() {
        return null;
    }

    protected IValueMap getSize() {
        return DialogConstants.LARGE;
    }
}
