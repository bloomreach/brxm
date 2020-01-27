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
package org.hippoecm.hst.content.beans;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyName;
import com.fasterxml.jackson.databind.introspect.Annotated;
import com.fasterxml.jackson.databind.introspect.AnnotatedClass;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;

import org.hippoecm.hst.content.annotations.PageModelIgnore;
import org.hippoecm.hst.content.annotations.PageModelIgnoreType;
import org.hippoecm.hst.content.annotations.PageModelProperty;
import org.hippoecm.hst.content.beans.standard.HippoBean;

/**
 * <p>
 *     This provides a {@link com.fasterxml.jackson.databind.ObjectMapper} that can serialize {@link HippoBean}s. A
 *     plain {@link com.fasterxml.jackson.databind.ObjectMapper} cannot serialize {@link HippoBean}s because of
 *     self-referencing and recursion in the bean methods: For example {@link HippoBean#getParentBean()} where the
 *     parent bean has again a getter that results in the current instance. The hippo beans are in the shared lib and
 *     therefor cannot use jackson annotations, but instead they have support for HST annotations for serialization,
 *     namely
 *     <ol>
 *         <li>{@link PageModelIgnore}</li>
 *         <li>{@link PageModelIgnoreType}</li>
 *         <li>{@link PageModelProperty}</li>
 *     </ol>
 * </p>
 */
public class PageModelObjectMapperFactory {

    public ObjectMapper createPageModelObjectMapper() {
        final ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setAnnotationIntrospector(new PageModelAnnotationIntrospector());
        return objectMapper;
    }

    private static class PageModelAnnotationIntrospector extends JacksonAnnotationIntrospector {
        @Override
        protected boolean _isIgnorable(Annotated a)
        {
            PageModelIgnore ann = _findAnnotation(a, PageModelIgnore.class);
            if (ann != null) {
                return ann.value();
            }

            return super._isIgnorable(a);
        }

        @Override
        public PropertyName findNameForSerialization(Annotated a)
        {
            PageModelProperty pann = _findAnnotation(a, PageModelProperty.class);
            if (pann != null) {
                return PropertyName.construct(pann.value());
            }
            return super.findNameForSerialization(a);
        }

        @Override
        public Boolean isIgnorableType(AnnotatedClass ac) {
            PageModelIgnoreType ignore = _findAnnotation(ac, PageModelIgnoreType.class);
            if (ignore != null) {
                return ignore.value();
            }
            return super.isIgnorableType(ac);
        }
    }
}
