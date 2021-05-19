/*
 *  Copyright 2019-2021 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms7.crisp.core.profile;

import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.hippoecm.hst.diagnosis.DefaultTaskImpl;
import org.hippoecm.hst.diagnosis.HDC;
import org.hippoecm.hst.diagnosis.Task;
import org.onehippo.cms7.crisp.api.broker.ResourceServiceBrokerRequestContext;
import org.onehippo.cms7.crisp.api.exchange.ExchangeHint;
import org.onehippo.cms7.crisp.api.resource.ResourceResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ResourceResolver profiling AOP Around Advice.
 * <P>
 * This advice leaves logs on each execution of {@link ResourceResolver} retrieving backend data in the the format:
 * <P>
 * <CODE>space:{}, op:{}, t:{}ms, verb:{}, sc:{}, path:{}, params:{}</CODE>
 * <P>
 * Each attribute represents the following:
 * <UL>
 * <LI><CODE>space</CODE>: The resource space name.
 * <LI><CODE>op</CODE>: The invoked operation name of the {@link ResourceResolver}.
 * <LI><CODE>t</CODE>: The response time in milliseconds on the invocation.
 * <LI><CODE>verb</CODE>: The HTTP verb (e.g, "GET", "POST", etc.) suggested by the given {@link ExchangeHint} if available.
 * <LI><CODE>sc</CODE>: The response status code from the backend, retrieved through the given {@link ExchangeHint} if available.
 * <LI><CODE>path</CODE>: The resource path.
 * <LI><CODE>params</CODE>: The path parameters interpolated at runtime.
 * </UL>
 */
public class ResourceResolverProfiler {

    private static Logger log = LoggerFactory.getLogger(ResourceResolverProfiler.class);

    private static final String LOG_FORMAT = "space:{}, op:{}, t:{}ms, verb:{}, sc:{}, path:{}, params:{}";

    private static final String TASK_NAME = "crispResResolver";

    public Object profile(ProceedingJoinPoint call) throws Throwable {
        final String resourceSpace = ResourceServiceBrokerRequestContext.getCurrentResourceSpace();

        final String opName = call.getSignature().getName();
        final Object[] args = call.getArgs();
        final int lastArgIndex = args.length - 1;

        final ExchangeHint exchangeHint = (lastArgIndex != -1 && args[lastArgIndex] instanceof ExchangeHint)
                ? (ExchangeHint) args[lastArgIndex]
                : null;
        final String httpVerb = (exchangeHint != null)
                ? StringUtils.defaultString(exchangeHint.getMethodName(), "GET")
                : "GET";

        final String resPath = (lastArgIndex != -1) ? args[0].toString() : null;

        @SuppressWarnings("unchecked")
        final Map<String, Object> pathParams = (lastArgIndex > 0 && (args[1] instanceof Map))
                ? ((Map<String, Object>) args[1])
                : null;

        Task task = null;
        final boolean hdcStarted = HDC.isStarted();

        Object retValue = null;

        try(IsolatedTaskImpl taskImpl = new IsolatedTaskImpl(TASK_NAME)) {
            task = (hdcStarted) ? HDC.getCurrentTask().startSubtask(TASK_NAME) : taskImpl;
            retValue = call.proceed();
        } finally {
            if (task != null) {
                task.stop();

                final int statusCode = (exchangeHint != null) ? exchangeHint.getResponseStatusCode() : 0;
                log.info(LOG_FORMAT, resourceSpace, opName, task.getDurationTimeMillis(), httpVerb, statusCode,
                        resPath, pathParams);

                if (hdcStarted) {
                    task.setAttribute("space", resourceSpace);
                    task.setAttribute("op", opName);
                    task.setAttribute("verb", httpVerb);
                    task.setAttribute("sc", statusCode);
                    task.setAttribute("path", resPath);
                    task.setAttribute("params", pathParams);
                }
            }
        }

        return retValue;
    }

    private static class IsolatedTaskImpl extends DefaultTaskImpl {

        IsolatedTaskImpl(final String name) {
            super(null, name);
        }
    }
}
