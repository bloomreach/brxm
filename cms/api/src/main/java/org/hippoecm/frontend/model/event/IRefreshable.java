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
package org.hippoecm.frontend.model.event;

import org.apache.wicket.IClusterable;

/**
 * This interface defines the contract for a service that can update its internal state.
 * To participate in the default refresh strategy, part of each (ajax) update of a page,
 * register the service at classname of this interface, IRefreshable.class.getName().
 */
public interface IRefreshable extends IClusterable {

    void refresh();
}
