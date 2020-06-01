/*
 * Copyright 2015-2018 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.plugins.standards.list.resolvers;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.model.IObservableModel;
import org.hippoecm.frontend.model.ReadOnlyModel;
import org.hippoecm.frontend.model.SerializableSupplier;

/**
 * Utility to set, add and remove CSS classes of Wicket components.
 */
public class CssClass {

    private static final String CLASS_ATTRIBUTE = "class";

    private CssClass() {
    }

    public static AttributeModifier append(final String cssClass) {
        return AttributeModifier.append(CLASS_ATTRIBUTE, cssClass);
    }

    public static AttributeModifier append(final IModel<String> cssClassModel) {
        return AttributeModifier.append(CLASS_ATTRIBUTE, cssClassModel);
    }

    public static AttributeModifier append(final SerializableSupplier<String> supplier) {
        return append(ReadOnlyModel.of(supplier));
    }

    public static CssClassAppender appendAndObserve(final IObservableModel<String> cssClassModel) {
        return new CssClassAppender(cssClassModel);
    }

    public static AttributeModifier set(final String cssClass) {
        return AttributeModifier.replace(CLASS_ATTRIBUTE, cssClass);
    }

    public static AttributeModifier set(final IModel<String> cssClass) {
        return AttributeModifier.replace(CLASS_ATTRIBUTE, cssClass);
    }

    public static AttributeModifier clear() {
        return AttributeModifier.remove(CLASS_ATTRIBUTE);
    }

}
