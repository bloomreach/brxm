/*
 * Copyright 2010-2019 Hippo B.V. (http://www.onehippo.com)
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

import java.util.List;
import java.util.Locale;

import javax.jcr.Session;

import org.apache.wicket.util.io.IClusterable;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.onehippo.forge.selection.frontend.model.ValueList;

/**
 * Interface for providers that generate value lists for an Apache Wicket application.
 *
 * A value list is a list of key/label pairs that can be used to populate lists, dropdowns etc.
 */
public interface IValueListProvider extends ValueListProvider, IClusterable {

    /**
     * The name of the plugin configuration parameter that holds
     * the id of the service provider that generates a list of
     * values.
     */
    String SERVICE = "valuelist.provider";

    /**
     * The id of the default valuelist provider
     */
    String SERVICE_VALUELIST_DEFAULT = "service.valuelist.default";

    /**
     * Returns a list of values
     *
     * @param config a plugin configuration that can help the
     *               provider to generate the list
     * @return a list of value items
     * @deprecated please use {@link #getValueList(String)} instead
     */
    @Deprecated
    ValueList getValueList(IPluginConfig config);

    /**
     * Returns an immutable list of values.
     *
     * @param name the name of a value list
     * @return a list of value items
     * @deprecated please use {@link #getValueList(String, Locale)} instead
     */
    @Deprecated
    ValueList getValueList(String name);

    /**
     * Returns an immutable list of values.
     *
     * @param name the name of a value list
     * @param locale the locale by which to get a preferred version of the valuelist
     * @return a list of value items
     */
    ValueList getValueList(String name, Locale locale);

    default ValueList getValueList(String name, Locale locale, Session session) {
        return getValueList(name, locale);    
    }
   
    /**
     * Returns the names of available value lists
     *
     * @return an immutable list of names of value lists
     */
    List<String> getValueListNames();

}
