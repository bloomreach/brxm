/*
 *  Copyright 2011 Hippo.
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
package org.hippoecm.hst.behavioral;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public interface BehavioralService {

    /**
     * Updates the behavioral data with any new information found in the current request.
     * The response is passed in order to make it possible to identify a user across requests.
     * 
     * @param request
     * @param response
     */
    void updateBehavioralData(HttpServletRequest request, HttpServletResponse response);
    
    /**
     * Get the {@link BehavioralProfile} of the current user.
     * 
     * @param request
     * @return the {@link BehavioralProfile} of the current user. 
     */
    BehavioralProfile getBehavioralProfile(HttpServletRequest request);
    
    /**
     * Invalidates the current {@link BehavioralService} implementation
     */
    void invalidate();
    
}
