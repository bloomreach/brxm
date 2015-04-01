/*
 *  Copyright 2015 Hippo B.V. (http://www.onehippo.com)
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.commons.lang.StringUtils;
import org.hippoecm.hst.configuration.ConfigurationUtils;
import org.hippoecm.hst.configuration.components.HstComponentConfiguration;
import org.hippoecm.hst.core.component.HstComponent;
import org.hippoecm.hst.core.parameters.DocumentLink;
import org.hippoecm.hst.core.parameters.JcrPath;
import org.hippoecm.hst.core.parameters.Parameter;
import org.hippoecm.hst.core.parameters.ParametersInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DocumentParamsScanner {

    private static final Logger log = LoggerFactory.getLogger(DocumentParamsScanner.class);

    // simple cache to avoid class method and annotation scanning over and over. Needs to be
    // synchronized since LocationMapTreeImpl construction can be invoked concurrent
    private final static ConcurrentMap<String, Set<String>> componentClassToDocumentParameterNamesCache = new ConcurrentHashMap<>();


    /**
     * Returns the document paths for <code>componentConfiguration</code> including its descendant
     * {@link HstComponentConfiguration}s. A document path is an method from the {@link ParametersInfo} that is
     * either annotated with {@link JcrPath} or {@link DocumentLink}.
     * @param componentConfiguration the root {@link HstComponentConfiguration} for which all document paths will be
     *                               returned. <strong>All</strong> as in that also all descendant
     *                               {@link HstComponentConfiguration}s are scanned.
     * @return {@link List} of document paths parameters for the <code>componentConfiguration</code> including all its
     * descendant {@link HstComponentConfiguration}s.
     */
    public static List<String> findDocumentPathsRecursive(final HstComponentConfiguration componentConfiguration) {
        final ArrayList<String> populate = new ArrayList<>();
        findDocumentPathsRecursive(componentConfiguration, populate);
        return populate;

    }

    private static void findDocumentPathsRecursive(final HstComponentConfiguration config,
                                            final List<String> populate) {

        final String componentClassName = config.getComponentClassName();
        if (StringUtils.isNotEmpty(componentClassName)) {
            Set<String> parameterNames = DocumentParamsScanner.getNames(componentClassName);

            for (String param : parameterNames) {
                String documentPath = config.getParameter(param);
                if (StringUtils.isNotEmpty(documentPath)) {
                    populate.add(documentPath);
                }
                // add variants as well
                for (String prefix : config.getParameterPrefixes()) {
                    final String prefixedParam = ConfigurationUtils.createPrefixedParameterName(prefix, param);
                    String variantDocumentPath = config.getParameter(prefixedParam);
                    if (StringUtils.isNotEmpty(variantDocumentPath)) {
                        populate.add(variantDocumentPath);
                    }
                }
            }
        }

        for (HstComponentConfiguration child : config.getChildren().values()) {
            findDocumentPathsRecursive(child, populate);
        }
    }

    /**
     * @param componentClassName the class name for which the {@link ParametersInfo} is scanned
     * @return {@link java.util.Set} of parameter names that have either {@link JcrPath} or {@link DocumentLink} annotation
     * present. Returns empty set if and exception occurs (for example <code>componentClassName</code> cannot be instantiated) or no
     * {@link ParametersInfo} is present on <code>componentClassName</code>
     */
    public static Set<String> getNames(final String componentClassName) {

        Set<String> parameterNames = componentClassToDocumentParameterNamesCache.get(componentClassName);
        if (parameterNames != null) {
            return parameterNames;
        }

        try {
            parameterNames = new HashSet<>();
            final Class<?> compClass = Class.forName(componentClassName);
            HstComponent component = (HstComponent) compClass.newInstance();
            ParametersInfo parametersInfo = component.getClass().getAnnotation(ParametersInfo.class);

            if (parametersInfo != null) {
                Class<?> parametersInfoType = parametersInfo.type();

                if (!parametersInfoType.isInterface()) {
                    throw new IllegalArgumentException("The ParametersInfo annotation type must be an interface.");
                }
                for (Method method : parametersInfoType.getMethods()) {
                    if (method.isAnnotationPresent(Parameter.class) &&
                            (method.isAnnotationPresent(JcrPath.class) || method.isAnnotationPresent(DocumentLink.class))) {
                        Parameter parameter = method.getAnnotation(Parameter.class);
                        parameterNames.add(parameter.name());
                    }
                }
            }
        }  catch (Exception e) {
            log.warn("Exception while finding documentLink or JcrPath annotations for {}. Return empty: {}",
                    componentClassName, e.toString());
        }
        componentClassToDocumentParameterNamesCache.putIfAbsent(componentClassName, parameterNames);
        return parameterNames;
    }

}
