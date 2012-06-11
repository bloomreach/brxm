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
 * ObjectFactory interface.
 * <P>
 * Implementation can create object as a singleton or per instance,
 * depending on the purpose of each implementation.
 * </P>
 * @version $Id$
 */
public interface ObjectFactory<T, U> {

    /**
     * Returns the object instance which might be a new instance or
     * a singleton instance, depending on implementations.
     * @param args the arguments which could be used during the object creation
     * @return
     */
    public T getInstance(U ... args);

}
