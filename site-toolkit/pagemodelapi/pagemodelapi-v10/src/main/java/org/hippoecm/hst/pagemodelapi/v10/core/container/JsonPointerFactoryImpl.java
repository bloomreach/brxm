/*
 *  Copyright 2020 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.pagemodelapi.v10.core.container;

import java.util.UUID;

import org.hippoecm.hst.content.beans.standard.IdentifiableContentBean;

// TODO make pluggable for downstream projects, for example such that a Product Ecommerce object can get a certain id
public class JsonPointerFactoryImpl implements JsonPointerFactory {
    /**
     * JSON property name prefix for a UUID-based identifier.
     */
    private static final String CONTENT_ID_JSON_NAME_PREFIX = "u";

    @Override
    public String createJsonPointerId(final Object object) {
        if (object instanceof IdentifiableContentBean) {
            final String id = ((IdentifiableContentBean)object).getRepresentationId();
            if (id == null) {
                return createJsonPointerId();
            } else {
                return createJsonPointerIdForString(id);
            }
        } else {
            return createJsonPointerId();
        }
    }

    @Override
    public String createJsonPointerId() {
        return createJsonPointerIdForString(UUID.randomUUID().toString()) ;
    }

    protected String createJsonPointerIdForString(final String string) {
        return new StringBuilder(string.length()).append(CONTENT_ID_JSON_NAME_PREFIX).append(string.replaceAll("-", ""))
                .toString();
    }
}
