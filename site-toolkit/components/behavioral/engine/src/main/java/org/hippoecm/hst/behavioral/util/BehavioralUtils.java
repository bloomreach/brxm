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
package org.hippoecm.hst.behavioral.util;

import javax.servlet.http.HttpServletRequest;

import org.hippoecm.hst.behavioral.BehavioralProfile;
import org.hippoecm.hst.behavioral.BehavioralService;
import org.hippoecm.hst.site.HstServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class BehavioralUtils {

    public static final Logger log = LoggerFactory.getLogger(BehavioralUtils.class);

    
    public static BehavioralService getBehavioralService() {
        return HstServices.getComponentManager().getComponent(BehavioralService.class.getName());
    }
    
    /**
     * @param request
     * @return the <code>BehavioralProfile</code> for the current {@link HttpServletRequest} and <code>null<code> if there cannot be returned a profile
     */
    public static BehavioralProfile getBehavioralProfile(HttpServletRequest request) {
         BehavioralService bs = getBehavioralService();
         if(bs == null) {
             log.warn("Cannot get BehavioralProfile because there is no BehavioralService component available");
             return null;
         }
         return getBehavioralService().getBehavioralProfile(request);
    }
    
}
