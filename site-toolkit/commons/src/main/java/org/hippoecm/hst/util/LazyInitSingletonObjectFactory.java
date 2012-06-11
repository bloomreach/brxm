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
 * @version $Id$
 */
abstract public class LazyInitSingletonObjectFactory<T, U> implements ObjectFactory<T, U> {

    private volatile boolean created;
    private T singleton;

    /* (non-Javadoc)
     * @see org.hippoecm.hst.util.ObjectFactory#getInstance()
     */
    @Override
    public T getInstance(U ... args) {
        // seamingly unused read but needed for shared memory flushing
        boolean created = this.created;
        if (!created) {
            synchronized (this) {
                created = this.created;
                if (!created) {
                    singleton = createInstance(args);
                    // flush the flag to shared memory
                    this.created = created = (singleton != null);
                }
            }
        }

        return singleton;
    }

    abstract protected T createInstance(U ... args);

}
