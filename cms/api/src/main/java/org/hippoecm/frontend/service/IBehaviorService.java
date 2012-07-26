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
package org.hippoecm.frontend.service;

import org.apache.wicket.IClusterable;
import org.apache.wicket.behavior.IBehavior;

public interface IBehaviorService extends IClusterable {

    String ID = "behavior.id";
    String PATH = "behavior.path";

    /**
     * The behavior that should be added to the component at relative path {@link #getComponentPath()}.
     * Returned behaviors should implement {@link #equals(Object)} and {@link #hashCode()} such that
     * multiple requests yield equivalent results.
     * 
     * @return the {@link IBehavior} to be attached to the component.
     */
    IBehavior getBehavior();

    String getComponentPath();
}
