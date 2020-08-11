/*
 *  Copyright 2011-2015 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.core.parameters;

import java.lang.String;import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that the annotated class contains a set of channel properties that have to be rendered as a group in a
 * certain order. This annotation should only be used on interfaces that map to channel information.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface FieldGroup {

    /**
     * Returns the array of channel property names in this field group. The properties should be rendered as a group
     * in the array order.
     *
     * @return the array of channel property names in this field group.
     */
    String[] value();

    /**
     * Returns the title key of this field group. If the title key is present in the resource bundle of the channel
     * properties class, that translated title will be rendered above the properties in this field group. By default,
     * the title key of a field group is an empty string.
     *
     * @return the title key of this field group.
     */
    String titleKey() default "";

}
