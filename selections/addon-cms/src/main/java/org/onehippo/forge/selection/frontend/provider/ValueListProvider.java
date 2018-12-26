/*
 * Copyright 2018 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onehippo.forge.selection.frontend.provider;

import java.util.Locale;

import javax.jcr.Session;

import org.onehippo.forge.selection.frontend.model.ValueList;

public interface ValueListProvider {

    /**
     * Returns an immutable list of values.
     *
     * @param name the name of a value list
     * @param locale the locale by which to get a preferred version of the valuelist
     * @return a list of value items
     */
    ValueList getValueList(String name, Locale locale, Session session);

}
