/*
 *  Copyright 2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.plugins.yui.layout;

import org.apache.wicket.Component;

public final class WireframeUtils {

    private WireframeUtils() {}


    public static IWireframe getParentWireframe(Component component) {
        //If linkedWithParent, look for an ancestor Component that implements IWireframeService and retrieve it's id
        Component parent = component.getParent();
        while (parent != null) {
            for (Object parentBehavior : parent.getBehaviors()) {
                if (parentBehavior instanceof IWireframe) {
                    return (IWireframe) parentBehavior;
                }
            }
            parent = parent.getParent();
        }
        return null;
    }

}
