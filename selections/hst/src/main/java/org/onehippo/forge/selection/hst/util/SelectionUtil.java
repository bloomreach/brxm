/*
 * Copyright 2010-2015 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.forge.selection.hst.util;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.lang.LocaleUtils;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.onehippo.forge.selection.hst.contentbean.ValueList;
import org.onehippo.forge.selection.hst.contentbean.ValueListItem;
import org.onehippo.forge.selection.hst.manager.ValueListManager;
import org.hippoecm.hst.site.HstServices;

public class SelectionUtil {

    /* Prevent instantiation */
    private SelectionUtil() {
    }

    /**
     * Map converter method, for easy access by JSP expression language.
     */
    public static Map<String, String> valueListAsMap(final ValueList valueList) {

        if ((valueList == null) || (valueList.getItems() == null)) {
            return null;
        }

        final List<ValueListItem> items = valueList.getItems();
        final Map<String, String> map = new LinkedHashMap<>(items.size());

        for (ValueListItem listItem : items) {
            map.put(listItem.getKey(), listItem.getLabel());
        }

        return map;
    }

    /**
     * Helper function for retrieving a value list by identifier, using the current context's locale.
     *
     * @param identifier of the (set of) value lists
     * @param context    current request context
     * @return           requested value list, or null if none was found
     */
    public static ValueList getValueListByIdentifier(final String identifier, final HstRequestContext context) {
        final ValueListManager valueListManager = HstServices.getComponentManager().getComponent(ValueListManager.class.getName());
        final HippoBean baseBean = context.getSiteContentBaseBean();
        final Locale locale = LocaleUtils.toLocale(context.getResolvedMount().getMount().getLocale());

        return valueListManager.getValueList(baseBean, identifier, locale);
    }
}
