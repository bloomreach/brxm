/*
 *  Copyright 2015-2022 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.attributes;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.model.IModel;

/**
 * Utility to set, add and remove title attributes of Wicket components.
 */
public class TitleAttribute {

    private static final String TITLE_ATTRIBUTE = "title";

    private TitleAttribute() {
    }

    public static AttributeAppender append(final String title) {
        return AttributeModifier.append(TITLE_ATTRIBUTE, title);
    }

    public static AttributeAppender append(final IModel<String> titleModel) {
        return AttributeModifier.append(TITLE_ATTRIBUTE, titleModel);
    }

    public static AttributeModifier set(final String title) {
        return AttributeModifier.replace(TITLE_ATTRIBUTE, title);
    }

    public static AttributeModifier set(final IModel<String> titleModel) {
        return AttributeModifier.replace(TITLE_ATTRIBUTE, titleModel);
    }

    public static AttributeModifier clear() {
        return AttributeModifier.remove(TITLE_ATTRIBUTE);
    }
}
