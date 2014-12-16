/*
 * Copyright 2010-2014 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.forge.selection.hst.manager;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.onehippo.forge.selection.hst.contentbean.ValueList;
import org.onehippo.forge.selection.hst.contentbean.ValueListItem;

public interface ValueListManager {

    /**
     * Get the value list document bean based on a site content base bean and an identifier.
     *
     * @param siteContentBaseBean site content base bean, from which to get a value list with a relative path
     * @param identifier identifier of a value list
     * @return ValueList found value list or null if not found
     */
    ValueList getValueList(final HippoBean siteContentBaseBean, final String identifier);

    /**
     * Get the value list document bean based on a hippo bean, an identifier and a locale.
     *
     * @param siteContentBaseBean site content base bean, from which to get a value list with a relative path
     * @param identifier identifier of a value list
     * @param locale locale by which to try to find a linked translated value list
     *
     * @return ValueList found value list or null if not found
     */
    ValueList getValueList(final HippoBean siteContentBaseBean, final String identifier, final Locale locale);

    /**
     * Get a specific value list item bean based on a document bean, an identifier and a key value.
     *
     * @param siteContentBaseBean site content base bean, from which to get a value list with a relative path
     * @param identifier identifier of a value list
     * @param key key of the value list item of a found value list
     *
     * @return ValueListItem found value list item or null if not found
     */
    ValueListItem getValueListItem(final HippoBean siteContentBaseBean, final String identifier, final String key);

    /**
     * Get a specific value list item bean based on a document bean, an identifier, a key value and a locale.
     *
     * @param siteContentBaseBean site content base bean, from which to get a value list with a relative path
     * @param identifier identifier of a value list
     * @param locale locale by which to try to find a linked translated value list
     * @param key key of the value list item of a found value list
     *
     * @return ValueListItem found value list item or null if not found
     */
    ValueListItem getValueListItem(final HippoBean siteContentBaseBean, final String identifier, final Locale locale, final String key);

    /**
     * Get a list of identifiers of the known value lists, that are valid as argument in the #getValueList methods.
     *
     * @return a list of String identifiers
     */
    List<String> getValueListIdentifiers();

    /**
     * Returns a map containing option ID and Text pairs by the given <code>identifier</code>.
     * @param identifier
     * @return
     */
    Map<String, String> getStaticOptionsMap(final String identifier);
}
