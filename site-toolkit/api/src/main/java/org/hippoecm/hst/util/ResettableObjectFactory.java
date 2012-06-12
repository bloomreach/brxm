/**
 * Copyright 2012 Hippo.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *         http://www.apache.org/licenses/LICENSE-2.
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.hst.util;

/**
 * ResettableObjectFactory interface.
 * @version $Id$
 */
public interface ResettableObjectFactory<T, U> extends ObjectFactory<T, U> {

    /**
     * Resets the status to the initial status, depending on implementations.
     * Usually singleton object factory can remove its singleton object held by itself.
     */
    public void reset();

}
