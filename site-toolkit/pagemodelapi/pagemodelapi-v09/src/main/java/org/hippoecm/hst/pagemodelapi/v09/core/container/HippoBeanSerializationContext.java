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
package org.hippoecm.hst.pagemodelapi.v09.core.container;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import org.hippoecm.hst.container.RequestContextProvider;
import org.hippoecm.hst.core.request.HstRequestContext;

/**
 * HippoBean Serialization Context.
 * <p>
 * This provides a serialization sub-context per a top level content bean's representation ID.
 * When serialization a top level content bean, it should start with {@link #beginTopLevelContentBean(String)}
 * and it should stop with {@link #endTopLevelContentBean()}.
 * </p>
 */
class HippoBeanSerializationContext {

    /**
     * <code>HstRequestContext</code> specific attribute name to store a <code>Stack</code> that contains the
     * representation IDs of the top level content bean items as direct children of the "content" section.
     */
    private static final String TOP_LEVEL_BEAN_IDS_STACK_ATTR_NAME = HippoBeanSerializationContext.class.getName()
            + ".topLevelBeanIdsStack";

    /**
     * <code>HstRequestContext</code> specific attribute name to store a <code>Set</code> that contains the
     * <code>HippoBeanWrapperModel</code> objects which are newly discovered and accumulated from the references
     * under the top level content bean items.
     */
    private static final String BEAN_MODEL_SET_MAP_ATTR_NAME = HippoBeanSerializationContext.class.getName()
            + ".beanModelSetMap";

    private HippoBeanSerializationContext() {
    }

    /**
     * Enter into a specific top level bean's (referred to by {@code representationId}) content bean serialization sub-context.
     * @param representationId the top level bean's {@code representationId}
     */
    public static void beginTopLevelContentBean(final String representationId) {
        final HstRequestContext requestContext = RequestContextProvider.get();
        Stack<String> baseModelIdStack = (Stack<String>) requestContext
                .getAttribute(TOP_LEVEL_BEAN_IDS_STACK_ATTR_NAME);

        if (baseModelIdStack == null) {
            baseModelIdStack = new Stack<>();
            requestContext.setAttribute(TOP_LEVEL_BEAN_IDS_STACK_ATTR_NAME, baseModelIdStack);
        }

        baseModelIdStack.push(representationId);
    }

    /**
     * Exit from the last top level bean's serialization sub-context.
     */
    public static String endTopLevelContentBean() {
        final HstRequestContext requestContext = RequestContextProvider.get();
        Stack<String> baseModelIdStack = (Stack<String>) requestContext
                .getAttribute(TOP_LEVEL_BEAN_IDS_STACK_ATTR_NAME);

        if (baseModelIdStack == null || baseModelIdStack.empty()) {
            throw new IllegalStateException("No current top level content bean representation ID in the internal stack.");
        }

        return baseModelIdStack.pop();
    }

    /**
     * Add the {@code model} to the serialization sub-context.
     * @param model the content bean model to add
     */
    @SuppressWarnings("unchecked")
    public static void addSerializableContentBeanModel(HippoBeanWrapperModel model) {
        final HstRequestContext requestContext = RequestContextProvider.get();

        Map<String, Set<HippoBeanWrapperModel>> modelSetMap = (Map<String, Set<HippoBeanWrapperModel>>) requestContext
                .getAttribute(BEAN_MODEL_SET_MAP_ATTR_NAME);

        if (modelSetMap == null) {
            modelSetMap = new HashMap<>();
            requestContext.setAttribute(BEAN_MODEL_SET_MAP_ATTR_NAME, modelSetMap);
        }

        final String topLevelBeanRepresentationId = getCurrentTopLevelContentBeanRepresentationId();
        Set<HippoBeanWrapperModel> modelSet = modelSetMap.get(topLevelBeanRepresentationId);

        if (modelSet == null) {
            modelSet = new LinkedHashSet<>();
            modelSetMap.put(topLevelBeanRepresentationId, modelSet);
        }

        modelSet.add(model);
    }

    /**
     * Get the <code>Set</code> for the current top level content bean's serialization sub-context, specified by
     * the {@code topLevelContentBeanRepresentationId}.
     * @param topLevelContentBeanRepresentationId the top level content bean's representation ID
     * @return the <code>Set</code> for the current top level content bean's serialization sub-context,
     * specified by the {@code topLevelContentBeanRepresentationId}
     */
    @SuppressWarnings("unchecked")
    public static Set<HippoBeanWrapperModel> getContentBeanModelSet(
            final String topLevelContentBeanRepresentationId) {
        final HstRequestContext requestContext = RequestContextProvider.get();
        Map<String, Set<HippoBeanWrapperModel>> modelSetMap = (Map<String, Set<HippoBeanWrapperModel>>) requestContext
                .getAttribute(BEAN_MODEL_SET_MAP_ATTR_NAME);

        if (modelSetMap == null) {
            return null;
        }

        return modelSetMap.get(topLevelContentBeanRepresentationId);
    }

    /**
     * Clear all the HippoBeanSerializationContext related attributes.
     */
    public static void clear() {
        final HstRequestContext requestContext = RequestContextProvider.get();
        requestContext.removeAttribute(TOP_LEVEL_BEAN_IDS_STACK_ATTR_NAME);
        requestContext.removeAttribute(BEAN_MODEL_SET_MAP_ATTR_NAME);
    }

    /**
     * Get the current top level bean's {@code representationId} from the content bean serialization sub-context.
     * @return the current top level bean's {@code representationId} from the internal stack
     */
    private static String getCurrentTopLevelContentBeanRepresentationId() {
        final HstRequestContext requestContext = RequestContextProvider.get();
        final Stack<String> topLevelBeanModelIdStack = (Stack<String>) requestContext
                .getAttribute(TOP_LEVEL_BEAN_IDS_STACK_ATTR_NAME);

        if (topLevelBeanModelIdStack == null || topLevelBeanModelIdStack.empty()) {
            throw new IllegalStateException("No current top level content bean representation ID in the internal stack.");
        }

        return topLevelBeanModelIdStack.peek();
    }

}
