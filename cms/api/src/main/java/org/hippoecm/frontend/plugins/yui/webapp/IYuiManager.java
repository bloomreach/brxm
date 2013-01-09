/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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

package org.hippoecm.frontend.plugins.yui.webapp;

import org.apache.wicket.IClusterable;
import org.hippoecm.frontend.plugins.yui.header.IYuiContext;

/**
 * {@link IYuiManager} implementations will be used as a service for creating {@link IYuiContext} instances. These 
 * should all use the same {@link org.hippoecm.frontend.plugins.yui.header.YuiHeaderCache} as their backing model. 
 */
public interface IYuiManager extends IClusterable {


    /**
     * Create a new {@link IYuiContext} that is backed up by a centrally managed
     * {@link org.hippoecm.frontend.plugins.yui.header.YuiHeaderCache}
     * 
     * @return A new {@link IYuiContext} to add header contributions to
     */
    IYuiContext newContext();
}
