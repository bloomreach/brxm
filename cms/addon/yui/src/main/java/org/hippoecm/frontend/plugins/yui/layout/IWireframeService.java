/*
 *  Copyright 2008 Hippo.
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

import org.apache.wicket.behavior.IBehavior;
import org.hippoecm.frontend.plugins.yui.javascript.YuiId;

/**
 * Behaviors implementing this service can be queried by nested component {@link WireframeBehavior}'s for their id 
 * value. This is because a nested wireframe needs to know it's parent id value during the render phase.   
 */
public interface IWireframeService extends IBehavior {
    final static String SVN_ID = "$Id$";

    /**
     * Return the {@link YuiId} value that represents the wireframe managed by the {@link IBehavior} that is 
     * implementing this interface.
     * 
     * @return a {@link YuiId} value representing the wireframe 
     */
    YuiId getParentId();
}
