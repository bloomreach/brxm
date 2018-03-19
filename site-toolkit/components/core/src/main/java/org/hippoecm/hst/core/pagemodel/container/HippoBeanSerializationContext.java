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
package org.hippoecm.hst.core.pagemodel.container;

import java.util.HashMap;
import java.util.HashSet;
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
 * <p>
 * To get the current top level content bean's representation ID in the sub-contexts, {@link #getCurrentTopLevelContentBeanRepresentationId()}
 * should be used.
 * </p>
 * <p>
 * And, to get the current stack of newly contributed content bean models in the sub-context, {@link #getContentBeanModelStack(String)}
 * should be used.
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
     * <code>HstRequestContext</code> specific attribute name to store a <code>Stack</code> that contains the
     * <code>HippoBeanWrapperModel</code> objects which are newly discovered and accumulated from the references
     * under the top level content bean items.
     */
    private static final String BEAN_MODEL_STACKS_MAP_ATTR_NAME = HippoBeanSerializationContext.class.getName()
            + ".beanModelStacksMap";

    /**
     * <code>HstRequestContext</code> specific attribute name to store a <code>Set</code> that stores all the processed
     * content beans' representation IDs for caching purpose, in order not to process multiple times for the same
     * content bean item.
     */
    private static final String BEAN_MODEL_ID_CACHE_SET_ATTR_NAME = HippoBeanSerializationContext.class.getName()
            + ".beanModelIdCacheSet";

    private HippoBeanSerializationContext() {
    }

    /**
     * Enter into a specific top level bean's (referred to by {@code representationId}) content bean serialization sub-context.
     * @param topLevelBeanRepresentationId the top level bean's {@code representationId}
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
     * Get the current top level bean's {@code representationId} from the content bean serialization sub-context.
     * @return the current top level bean's {@code representationId} from the internal stack
     */
    public static String getCurrentTopLevelContentBeanRepresentationId() {
        final HstRequestContext requestContext = RequestContextProvider.get();
        final Stack<String> baseModelIdStack = (Stack<String>) requestContext
                .getAttribute(TOP_LEVEL_BEAN_IDS_STACK_ATTR_NAME);

        if (baseModelIdStack == null || baseModelIdStack.empty()) {
            throw new IllegalStateException("No current top level content bean representation ID in the internal stack.");
        }

        return baseModelIdStack.peek();
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
     * Push the {@code model} to the serialization sub-context (specified by {@code representationId}).
     * @param representationId the {@code model}'s representation ID
     * @param model the content bean model to push
     */
    @SuppressWarnings("unchecked")
    public static void pushContentBeanModel(final String representationId, HippoBeanWrapperModel model) {
        final HstRequestContext requestContext = RequestContextProvider.get();

        Set<String> modelIdCacheSet = (Set<String>) requestContext.getAttribute(BEAN_MODEL_ID_CACHE_SET_ATTR_NAME);

        if (modelIdCacheSet == null) {
            modelIdCacheSet = new HashSet<>();
            requestContext.setAttribute(BEAN_MODEL_ID_CACHE_SET_ATTR_NAME, modelIdCacheSet);
        }

        if (modelIdCacheSet.contains(representationId)) {
            return;
        }

        Map<String, Stack<HippoBeanWrapperModel>> modelStacksMap = (Map<String, Stack<HippoBeanWrapperModel>>) requestContext
                .getAttribute(BEAN_MODEL_STACKS_MAP_ATTR_NAME);

        if (modelStacksMap == null) {
            modelStacksMap = new HashMap<>();
            requestContext.setAttribute(BEAN_MODEL_STACKS_MAP_ATTR_NAME, modelStacksMap);
        }

        Stack<HippoBeanWrapperModel> modelStack = (Stack<HippoBeanWrapperModel>) modelStacksMap.get(representationId);

        if (modelStack == null) {
            modelStack = new Stack<>();
            modelStacksMap.put(representationId, modelStack);
        }

        modelStack.push(model);
        modelIdCacheSet.add(model.getBean().getRepresentationId());
    }

    /**
     * Get the <code>Stack</code> for the current top level content bean's serialization sub-context, specified by
     * the {@code topLevelContentBeanRepresentationId}.
     * @param topLevelContentBeanRepresentationId the top level content bean's representation ID
     * @return the <code>Stack</code> for the current top level content bean's serialization sub-context,
     * specified by the {@code topLevelContentBeanRepresentationId}
     */
    @SuppressWarnings("unchecked")
    public static Stack<HippoBeanWrapperModel> getContentBeanModelStack(
            final String topLevelContentBeanRepresentationId) {
        final HstRequestContext requestContext = RequestContextProvider.get();
        Map<String, Stack<HippoBeanWrapperModel>> modelStacksMap = (Map<String, Stack<HippoBeanWrapperModel>>) requestContext
                .getAttribute(BEAN_MODEL_STACKS_MAP_ATTR_NAME);

        if (modelStacksMap == null) {
            return null;
        }

        return (Stack<HippoBeanWrapperModel>) modelStacksMap.get(topLevelContentBeanRepresentationId);
    }

    /**
     * Clear all the HippoBeanSerializationContext related attributes.
     */
    public static void clear() {
        final HstRequestContext requestContext = RequestContextProvider.get();
        requestContext.removeAttribute(TOP_LEVEL_BEAN_IDS_STACK_ATTR_NAME);
        requestContext.removeAttribute(BEAN_MODEL_STACKS_MAP_ATTR_NAME);
        requestContext.removeAttribute(BEAN_MODEL_ID_CACHE_SET_ATTR_NAME);
    }
}
