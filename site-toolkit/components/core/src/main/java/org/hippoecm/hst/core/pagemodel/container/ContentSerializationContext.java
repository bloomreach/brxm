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

import org.hippoecm.hst.container.RequestContextProvider;
import org.hippoecm.hst.core.request.HstRequestContext;

/**
 * Content Serialization Context.
 */
class ContentSerializationContext {

    /**
     * <code>HstRequestContext</code> specific {@link Phase} attribute name.
     */
    private static final String PHASE_ATTR = ContentSerializationContext.class.getName() + ".phase";

    /**
     * <code>HstRequestContext</code> specific {@link AggregatedPageModel} attribute name.
     */
    private static final String AGGREGATED_PAGE_MODEL_ATTR = ContentSerializationContext.class.getName()
            + ".aggregatedPageModel";

    /**
     * Content Serialization Phase Enumeration.
     */
    public enum Phase {
        REFERENCING_CONTENT_IN_COMPONENT, // The phase referencing content item from a component model.
        REFERENCING_CONTENT_IN_CONTENT,   // The phase referencing content item from another content item.
        SERIALIZING_CONTENT               // The phase serializing the content item fully.
    }

    private ContentSerializationContext() {
    }

    /**
     * Return the current Content Serialization phase.
     * @return the current Content Serialization phase
     */
    public static Phase getCurrentPhase() {
        Phase phase = (Phase) RequestContextProvider.get().getAttribute(PHASE_ATTR);
        return phase;
    }

    /**
     * Set the current Content Serialization phase.
     * @param phase the current Content Serialization phase
     */
    public static void setCurrentPhase(Phase phase) {
        RequestContextProvider.get().setAttribute(PHASE_ATTR, phase);
    }

    /**
     * Return the current {@link AggregatedPageModel} object.
     * @return the current {@link AggregatedPageModel} object
     */
    public static AggregatedPageModel getCurrentAggregatedPageModel() {
        AggregatedPageModel aggregatedPageModel = (AggregatedPageModel) RequestContextProvider.get()
                .getAttribute(AGGREGATED_PAGE_MODEL_ATTR);
        return aggregatedPageModel;
    }

    /**
     * Set the current {@link AggregatedPageModel} object.
     * @param aggregatedPageModel the current {@link AggregatedPageModel} object
     */
    public static void setCurrentAggregatedPageModel(AggregatedPageModel aggregatedPageModel) {
        RequestContextProvider.get().setAttribute(AGGREGATED_PAGE_MODEL_ATTR, aggregatedPageModel);
    }

    /**
     * Clear all the <code>HstRequestContext} specific attributes regarding Content Serialization Context.
     */
    public static void clear() {
        final HstRequestContext requestContext = RequestContextProvider.get();
        requestContext.removeAttribute(PHASE_ATTR);
        requestContext.removeAttribute(AGGREGATED_PAGE_MODEL_ATTR);
    }
}
