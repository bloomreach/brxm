/*
 *  Copyright 2009 Hippo.
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
package org.hippoecm.frontend.editor.layout;

import org.hippoecm.frontend.editor.builder.IBuilderContext;

/**
 * Context for layout editing plugins.  It makes it possible for render services 
 * to relocate themselves.
 */
public interface ILayoutContext extends IBuilderContext {
    
    /**
     * @return the current location of the render service
     */
    ILayoutPad getLocation();
    
    /**
     * Apply the transition.  The render service will move to the target
     * location of the transition.
     * 
     * @param transition
     */
    void apply(ILayoutTransition transition);

}
