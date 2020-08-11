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
package org.hippoecm.hst.core.container;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.hippoecm.hst.configuration.components.HstComponentConfiguration;
import org.hippoecm.hst.core.request.HstRequestContext;

/**
 * Selective Rendering ContextResolvingValve.
 * <p>
 * Filter out all the descendant windows from the root component window.
 * This can be improved in the future to select some descendant components which have specific configuration properties.
 */
public class SelectiveRenderingContextResolvingValve extends ContextResolvingValve {

    /**
     * {@inheritDoc}
     * <p>
     * Simply filter out all the children of the root component window returned by the super class.
     */
    @Override
    protected HstComponentWindow createRootComponentWindow(ValveContext context,
            HstComponentConfiguration rootComponentConfig) {
        HstComponentWindow rootComponentWindow = super.createRootComponentWindow(context, rootComponentConfig);

        Map<String, HstComponentWindow> childMap = rootComponentWindow.getChildWindowMap();

        if (childMap != null) {
            List<HstComponentWindow> children = new ArrayList<>(childMap.values());

            for (HstComponentWindow child : children) {
                rootComponentWindow.removeChildWindow(child);
            }
        }

        return rootComponentWindow;
    }

}
