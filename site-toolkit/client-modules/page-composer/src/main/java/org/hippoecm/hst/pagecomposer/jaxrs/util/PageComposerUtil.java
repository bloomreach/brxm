/*
 * Copyright 2016-2017 Hippo B.V. (http://www.onehippo.com)
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
 *
 */

package org.hippoecm.hst.pagecomposer.jaxrs.util;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.hippoecm.hst.configuration.HstNodeTypes;
import org.hippoecm.hst.container.RequestContextProvider;
import org.hippoecm.hst.core.parameters.Parameter;
import org.hippoecm.hst.core.parameters.ParametersInfo;
import org.hippoecm.hst.pagecomposer.jaxrs.services.PageComposerContextService;
import org.hippoecm.hst.platform.model.HstModel;
import org.hippoecm.hst.util.ParametersInfoAnnotationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hippoecm.hst.configuration.HstNodeTypes.NODETYPE_HST_CONTAINERITEMCOMPONENT;

public class PageComposerUtil {

    private PageComposerUtil() {}

    /**
     * Returns the {@link Map} of annotated parameter name as key and annotated default value as value. Parameters with
     * empty default value are also represented in the returned map.
     *
     * @param componentItemNode the current container item componentItemNode
     * @return the Map of all {@link Parameter} names and their default value
     */
    public static Map<String, String> getAnnotatedDefaultValues(final Node componentItemNode) throws RepositoryException, IllegalArgumentException {
        final ParametersInfo parametersInfo = executeWithWebsiteClassLoader(node -> {
            try {
                return ParametersInfoAnnotationUtils.getParametersInfoAnnotation(componentItemNode);
            } catch (RepositoryException e) {
                throw new RuntimeException(e);
            }
        }, componentItemNode);

        // Parameter is part of shared class loader so below doesn't need to be done with the 'website classloader'
        if (parametersInfo != null) {
            Class<?> classType = parametersInfo.type();
            if (classType == null) {
                return Collections.emptyMap();
            }
            Map<String, String> result = new HashMap<String, String>();
            for (Method method : classType.getMethods()) {
                if (method.isAnnotationPresent(Parameter.class)) {
                    Parameter annotation = method.getAnnotation(Parameter.class);
                    result.put(annotation.name(), annotation.defaultValue());
                }
            }
            return result;
        }
        return Collections.emptyMap();
    }

    /**
     * @param function the function to be executed with the website classloader
     * @param input the input of the {@code function}
     * @param <R> the type of the output of the {@code function}
     * @param <T> the type of the input of the {@code function}
     * @return
     */
    public static <T, R> R executeWithWebsiteClassLoader(final Function<T,R> function, final T input) {
        final ClassLoader currentClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            final HstModel websiteHstModel = (HstModel) RequestContextProvider.get().getAttribute(PageComposerContextService.PREVIEW_EDITING_HST_MODEL_ATTR);
            if (websiteHstModel != null) {
                Thread.currentThread().setContextClassLoader(websiteHstModel.getWebsiteClassLoader());
            }
            return function.apply(input);
        } finally {
            Thread.currentThread().setContextClassLoader(currentClassLoader);
        }
    }

}
