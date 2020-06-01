/*
 * Copyright 2010-2013 Hippo B.V. (http://www.onehippo.com)
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *  http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.hst.configuration.hosting;

public interface PortMount {

    /**
     * @return Returns the portnumber associated with this {@link PortMount} object. 
     */
    int getPortNumber();
    
    /**
     * A {@link PortMount} has to have at least a root {@link Mount}, otherwise it is not a valid PortNumber and cannot be used.
     * @return the root {@link Mount} for this PortNumber object. When this PortMount has an invalid configured {@link Mount} or no {@link Mount}, <code>null</code> will be returned
     */
    Mount getRootMount();
    
   
}
