/*
 *  Copyright 2009-2016 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.demo.components;

import org.apache.commons.lang.time.StopWatch;
import org.aspectj.lang.ProceedingJoinPoint;
import org.hippoecm.hst.core.component.HstResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Example profiling Aspect implementation which simply profiles the execution time in milliseconds
 * for demonstration purpose.
 */
public class ExampleProfilingHstComponentAspect {

    private static Logger log = LoggerFactory.getLogger(ExampleProfilingHstComponentAspect.class);

    public Object profile(ProceedingJoinPoint pjp) throws Throwable {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        final HstResponse hstResponse = (HstResponse) pjp.getArgs()[1];
        final String compNamespace = hstResponse.getNamespace();

        Object retVal = pjp.proceed();

        stopWatch.stop();
        log.info("Example profiling for '{}': {}ms.", compNamespace, stopWatch.getTime());

        return retVal;
    }

}
