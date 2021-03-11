/*
 *  Copyright 2015-2021 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.core.linking;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.hippoecm.hst.configuration.ConfigurationUtils;
import org.hippoecm.hst.configuration.components.DynamicParameterConfig;
import org.hippoecm.hst.configuration.components.HstComponentConfiguration;
import org.hippoecm.hst.core.parameters.JcrPath;
import org.hippoecm.hst.core.parameters.Parameter;
import org.hippoecm.hst.core.parameters.ParametersInfo;
import org.hippoecm.hst.util.ParametersInfoAnnotationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DocumentParamsScanner {

    private static final Logger log = LoggerFactory.getLogger(DocumentParamsScanner.class);

    // simple cache to avoid class method and annotation scanning over and over. Needs to be
    // synchronized since LocationMapTreeImpl construction can be invoked concurrent
    private final static ConcurrentMap<String, Set<String>> componentDocumentParamNamesCache = new ConcurrentHashMap<>();

    /**
     * Returns the document paths for <code>componentConfiguration</code> including its descendant
     * {@link HstComponentConfiguration}s. A document path is an method from the {@link ParametersInfo} that is
     * annotated with {@link JcrPath}. The <code>componentConfiguration</code> java
     * class will be loaded by the provided <code>classLoader</code>
     *
     * @param componentConfiguration the root {@link HstComponentConfiguration} for which all document paths will be
     *                               returned. <strong>All</strong> as in that also all descendant
     *                               {@link HstComponentConfiguration}s are scanned.
     * @return {@link List} of document paths parameters for the <code>componentConfiguration</code> including all its
     * descendant {@link HstComponentConfiguration}s.
     */
    public static List<String> findDocumentPathsRecursive(final HstComponentConfiguration componentConfiguration,
                                                          final ClassLoader classLoader) {
        return findDocumentPathsRecursive(componentConfiguration, classLoader, config -> true);
    }

    /**
     * Returns the same as {@link #findDocumentPathsRecursive(org.hippoecm.hst.configuration.components.HstComponentConfiguration, ClassLoader)}
     * only this time during recursive scanning of the {@code componentConfiguration} and its descendants, the scanning
     * can be stopped when the {@link java.util.function.Predicate} returns false for a {@link org.hippoecm.hst.configuration.components.HstComponentConfiguration}
     * @see #findDocumentPathsRecursive(org.hippoecm.hst.configuration.components.HstComponentConfiguration, ClassLoader)
     */
    public static List<String> findDocumentPathsRecursive(final HstComponentConfiguration componentConfiguration,
                                                          final ClassLoader classLoader,
                                                          final Predicate<HstComponentConfiguration> predicate) {
        final ArrayList<String> populate = new ArrayList<>();
        findDocumentPathsRecursive(componentConfiguration, populate, classLoader, predicate);
        return populate;
    }

    private static void findDocumentPathsRecursive(final HstComponentConfiguration config,
                                                   final List<String> populate,
                                                   final ClassLoader classLoader,
                                                   final Predicate<HstComponentConfiguration> predicate) {
        if (!predicate.test(config)) {
            log.debug("Skip '{}' plus descendants because of predicate '{}'", config, predicate);
            return;
        }
        Set<String> parameterNames = getNames(config, classLoader);

        for (String param : parameterNames) {
            String documentPath = config.getParameter(param);
            if (!isEmpty(documentPath)) {
                populate.add(documentPath);
            }
            // add variants as well
            for (String prefix : config.getParameterPrefixes()) {
                final String prefixedParam = ConfigurationUtils.createPrefixedParameterName(prefix, param);
                String variantDocumentPath = config.getParameter(prefixedParam);
                if (!isEmpty(variantDocumentPath)) {
                    populate.add(variantDocumentPath);
                }
            }
        }

        for (HstComponentConfiguration child : config.getChildren().values()) {
            findDocumentPathsRecursive(child, populate, classLoader, predicate);
        }
    }

    private static boolean isEmpty(final String str) {
        return str == null || str.length() == 0;
    }

    /**
     * @param config the {@link HstComponentConfiguration} for which we need to return the parameter names
     * @return {@link java.util.Set} of parameter names that have a {@link JcrPath} annotation
     * present. Returns empty set if an exception occurs (for example <code>componentClassName</code> cannot be
     * instantiated) or no
     * {@link ParametersInfo} is present on <code>componentClassName</code>.
     */
    public static Set<String> getNames(final HstComponentConfiguration config,
                                       final ClassLoader classLoader) {
        final String componentClassName = config.getComponentClassName();
        final String parametersInfoClassName = config.getParametersInfoClassName();
        Set<String> parameterNames = getAnnotationBasedParameterNames(componentClassName, parametersInfoClassName, classLoader);
        parameterNames.addAll(getDynamicJcrPathParameterNames(config));
        return parameterNames;
    }

    private static Set<String> getDynamicJcrPathParameterNames(final HstComponentConfiguration config) {
        return config.getDynamicComponentParameters().stream().filter(param -> param.getComponentParameterConfig().getType() == DynamicParameterConfig.Type.JCR_PATH)
                .map(param -> param.getName())
                .collect(Collectors.toSet());
    }

    private static Set<String> getAnnotationBasedParameterNames(final String componentClassName, final String parametersInfoClassName,
                                                                final ClassLoader classLoader) {

        if (isEmpty(componentClassName) && isEmpty(parametersInfoClassName)) {
            return Collections.emptySet();
        }

        final String cacheKey = new StringBuilder(128).append(componentClassName).append(':')
                .append(parametersInfoClassName).toString();
        Set<String> parameterNames = componentDocumentParamNamesCache.get(cacheKey);

        if (parameterNames != null) {
            return parameterNames;
        }

        ParametersInfo parametersInfo = ParametersInfoAnnotationUtils.getParametersInfoAnnotation(componentClassName,
                parametersInfoClassName, classLoader);

        if (parametersInfo == null) {
            return Collections.emptySet();
        }

        Class<?> parametersInfoType = parametersInfo.type();

        if (!parametersInfoType.isInterface()) {
            throw new IllegalArgumentException("The ParametersInfo annotation type must be an interface.");
        }

        parameterNames = new HashSet<>();

        for (Method method : parametersInfoType.getMethods()) {
            if (method.isAnnotationPresent(Parameter.class) && method.isAnnotationPresent(JcrPath.class)) {
                Parameter parameter = method.getAnnotation(Parameter.class);
                parameterNames.add(parameter.name());
            }
        }

        componentDocumentParamNamesCache.putIfAbsent(cacheKey, parameterNames);

        return parameterNames;
    }

}
