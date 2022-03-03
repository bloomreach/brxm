/*
 * Copyright 2019-2022 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.frontend.attributes;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.model.IModel;

/**
 * Utility to set, add and remove style attributes of Wicket components.
 */
public class StyleAttribute {

    private static final String STYLE_ATTRIBUTE = "style";
    private static final String SEPARATOR = ";";

    private StyleAttribute() {
    }

    public static AttributeModifier append(final String style) {
        return AttributeModifier.append(STYLE_ATTRIBUTE, style).setSeparator(SEPARATOR);
    }

    public static AttributeModifier append(final IModel<String> styleModel) {
        return AttributeModifier.append(STYLE_ATTRIBUTE, styleModel).setSeparator(SEPARATOR);
    }

    public static AttributeModifier set(final String style) {
        return AttributeModifier.replace(STYLE_ATTRIBUTE, style);
    }

    public static AttributeModifier set(final IModel<String> styleModel) {
        return AttributeModifier.replace(STYLE_ATTRIBUTE, styleModel);
    }

    public static AttributeModifier clear() {
        return AttributeModifier.remove(STYLE_ATTRIBUTE);
    }

}
