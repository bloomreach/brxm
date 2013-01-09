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
package org.hippoecm.hst.core.logging;

import org.apache.commons.lang.StringUtils;
import org.hippoecm.hst.logging.LogEventBuffer;
import org.hippoecm.hst.logging.Logger;
import org.hippoecm.hst.logging.LoggerFactory;
import org.hippoecm.hst.site.HstServices;

public class Slf4jLoggerFactory implements LoggerFactory {
    
    enum RuntimeMode {
        UNKNOWN_MODE,
        DEVELOPMENT_MODE,
        PRODUCTION_MODE
    }
    
    private RuntimeMode runtimeMode = RuntimeMode.UNKNOWN_MODE;
    
    private LogEventBuffer traceToolLogEventBuffer;
    
    public void setTraceToolLogEventBuffer(LogEventBuffer traceToolLogEventBuffer) {
        this.traceToolLogEventBuffer = traceToolLogEventBuffer;
    }
    
    public Logger getLogger(String name) {
        return getLogger(name, null);
    }
    
    public Logger getLogger(String name, String fqcn) {
        Logger logger = null;

        if (runtimeMode == RuntimeMode.UNKNOWN_MODE) {
            if (HstServices.isAvailable()) {
                if (HstServices.getComponentManager().getContainerConfiguration().isDevelopmentMode()) {
                    runtimeMode = RuntimeMode.DEVELOPMENT_MODE;
                } else {
                    runtimeMode = RuntimeMode.PRODUCTION_MODE;
                }
            }
        }
        
        if (runtimeMode == RuntimeMode.DEVELOPMENT_MODE) {
            logger = new TraceToolSlf4jLogger(traceToolLogEventBuffer, org.slf4j.LoggerFactory.getLogger(name));
        } else if (StringUtils.isBlank(fqcn)) {
            logger = new Slf4jLogger(org.slf4j.LoggerFactory.getLogger(name));
        } else {
            logger = new Slf4jLogger(org.slf4j.LoggerFactory.getLogger(name), fqcn);
        }
        
        return logger;
    }

}
