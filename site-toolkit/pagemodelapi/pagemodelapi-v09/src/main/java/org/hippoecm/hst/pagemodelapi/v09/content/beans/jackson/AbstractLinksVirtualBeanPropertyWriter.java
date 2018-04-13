/*
 *  Copyright 2018 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.pagemodelapi.v09.content.beans.jackson;

import java.util.Map;

import org.hippoecm.hst.core.request.HstRequestContext;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.introspect.BeanPropertyDefinition;
import com.fasterxml.jackson.databind.util.Annotations;

/**
 * Abstract base class to add extra <code>_links</code> properties dynamically.
 * @param <S> the type of the original source bean type
 */
abstract public class AbstractLinksVirtualBeanPropertyWriter<S> extends AbstractVirtualBeanPropertyWriter<S, Map<String, LinkModel>> {

    private static final long serialVersionUID = 1L;

    protected AbstractLinksVirtualBeanPropertyWriter() {
        super();
    }

    protected AbstractLinksVirtualBeanPropertyWriter(BeanPropertyDefinition propDef, Annotations contextAnnotations,
                                                    JavaType type) {
        super(propDef, contextAnnotations, type);
    }

    @Override
    protected final Map<String, LinkModel> createValue(final HstRequestContext requestContext, final S item) throws Exception {
        if (requestContext == null) {
            return null;
        }

        return createLinksMap(requestContext, item);
    }

    /**
     * Create a links map or return null if no extra property is not needed to add.
     * @param requestContext request context
     * @param item bean item
     * @return a links map or null
     * @throws Exception if any exception occurs
     */
    abstract protected Map<String, LinkModel> createLinksMap(final HstRequestContext requestContext, final S item) throws Exception;

}
