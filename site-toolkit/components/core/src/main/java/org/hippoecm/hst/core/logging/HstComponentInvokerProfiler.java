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
package org.hippoecm.hst.core.logging;

import org.aspectj.lang.ProceedingJoinPoint;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstRequestImpl;
import org.hippoecm.hst.core.container.HstComponentWindow;
import org.hippoecm.hst.core.container.HstContainerURL;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.logging.Logger;
import org.hippoecm.hst.logging.LoggerFactory;

public class HstComponentInvokerProfiler {

    private static final String LOGGER_NAME = HstComponentInvokerProfiler.class.getName();
    
    private LoggerFactory loggerFactory;

    public HstComponentInvokerProfiler(LoggerFactory loggerFactory) {
        this.loggerFactory = loggerFactory;
    }
    
    public Object profile(ProceedingJoinPoint call) throws Throwable {
        Logger logger = this.loggerFactory.getLogger(LOGGER_NAME);
        
        if (logger.isInfoEnabled()) {
            long start = System.currentTimeMillis();
            Object [] args = call.getArgs();
            String method = call.toShortString();
            String pathInfo = "";
            String windowName = "";
            String refNamespace = "";
            
            try {
                
                if (args.length > 1 && args[1] instanceof HstRequest) {
                    HstRequest hstRequest = (HstRequest) args[1];
                    HstComponentWindow window = ((HstRequestImpl) hstRequest).getComponentWindow();
                    windowName = window.getName();
                    HstRequestContext hstRequestContext = hstRequest.getRequestContext();
                    HstContainerURL url = hstRequestContext.getBaseURL();
                    pathInfo = url.getPathInfo();
                    refNamespace = hstRequest.getReferenceNamespace();
                    if ("".equals(refNamespace)) {
                        refNamespace = "root";
                    }
                }
                
                return call.proceed();
                
            } finally {
                long laps = System.currentTimeMillis() - start;
                logger.info("Profiling: {} of {} ({}) on {} took {}ms.", new Object [] { method, windowName, refNamespace, pathInfo, Long.toString(laps) });
            }
        } else {
            return call.proceed();
        }
    }
    
}
