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
package org.hippoecm.hst.service;

import org.hippoecm.hst.provider.ValueProvider;

public interface Service {
    
    /**
     * @return ValueProvider giving access to the underlying object providing the values
     */
    ValueProvider getValueProvider();
    
    /**
     * 
     * @return an array of child Services. If there are no child services, an empty array is returned
     */
    Service[] getChildServices();
    
    /**
     * Closing a valueprovider means that the underlying value provider might be closed after calling this method, though 
     * this is up to the implementation.
     * @param closeChildServices
     */
    void closeValueProvider(boolean closeChildServices);
    
}
