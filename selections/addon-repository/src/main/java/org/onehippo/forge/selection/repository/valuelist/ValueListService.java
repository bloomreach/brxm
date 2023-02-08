/*
 *  Copyright 2019-2023 Bloomreach
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

package org.onehippo.forge.selection.repository.valuelist;

import java.util.Locale;

import javax.jcr.Session;

import org.onehippo.forge.selection.frontend.model.ValueList;

public interface ValueListService {

    static ValueListService get() {
        return ValueListServiceImpl.getInstance();
    }

    ValueList getValueList(String source, Locale locale, Session session);

    /**
     * Invalidate the value list cache
     */
    void invalidateCache();
}
