/*
 * Copyright 2019 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.forge.selection.repository.valuelist;

import java.util.Locale;
import java.util.concurrent.ExecutionException;

import javax.jcr.Session;

import org.onehippo.forge.selection.frontend.model.ValueList;
import org.onehippo.forge.selection.frontend.provider.ValueListProvider;
import org.onehippo.forge.selection.frontend.utils.JcrUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

public class ValueListServiceImpl implements ValueListProvider, ValueListService {

    private static final Logger log = LoggerFactory.getLogger(ValueListServiceImpl.class);
    private static final ValueListServiceImpl INSTANCE = new ValueListServiceImpl();

    private static final Cache<String, ValueList> VALUE_LISTS = CacheBuilder.newBuilder()
            .maximumSize(100)
            .build();

    private ValueListServiceImpl() {
    }

    public static ValueListServiceImpl getInstance() {
        return INSTANCE;
    }

    @Override
    public ValueList getValueList(final String source, final Locale locale, final Session session) {
        final String cacheKey = source + "-" + locale.toString();
        try {
            return VALUE_LISTS.get(cacheKey, () -> retrieveValueList(source, locale, session));
        } catch (ExecutionException e) {
            log.warn("Value list could not be loaded. Returning empty Value list");
            return new ValueList();
        }
    }

    private ValueList retrieveValueList(final String source, final Locale locale, final Session session) {
        return JcrUtils.getValueList(source, locale, session);
    }

    /**
     * Invalidate the value lists cache
     */
    public void invalidateCache() {
        VALUE_LISTS.invalidateAll();
    }
}
