/*
 * Copyright 2011-2020 Bloomreach
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.hst.core.parameters;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that the value of the annotated method should be selected from a drop-down list. The available options
 * in the drop-down list are specified as an array of Strings.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface DropDownList {

    /**
     * Static list of options to show in the drop-down list. The selected option value is converted from a
     * {@link String} to the return type of the annotated method.
     *
     * @return the options to show in the drop-down list.
     */
    String[] value() default {};

    /**
     * @deprecated use valueListProviderKey instead.
     * Dynamic value list provider class that can return a list of options to show in the drop-down list dynamically
     * from any data sources.  The selected option value is converted from a {@link String} to the return type
     *
     * @return dynamic value list provider class that can return a list of options to show in the drop-down list
     * dynamically from any data sources.
     */
    @Deprecated
    Class<? extends ValueListProvider> valueListProvider() default EmptyValueListProvider.class;

    /**
     * Dynamic value list provider key that can be used to get a {@link ValueListProvider} instance from the
     * ValueListProviderService. That instance will return a list of options to show in the drop-down list
     * dynamically from any data sources.  The selected option value is converted from a {@link String} to the return
     * type of the annotated method.
     *
     * @return dynamic value list provider class that can return a list of options to show in the drop-down list
     * dynamically from any data sources.
     */
    String valueListProviderKey() default "";
}
