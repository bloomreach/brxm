/**
 * Copyright 2015 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.hst.core.container;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;

import org.hippoecm.hst.container.HstContainerRequest;
import org.hippoecm.hst.diagnosis.HDC;
import org.hippoecm.hst.diagnosis.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * BinariesDispatchingValve
 * <p/>
 * invokes the <code>ServletContext#getRequestDispatcher(binariesDispatchPath)</code> in order to let the binaries servlet
 * continue request processing with HST provided context attributes.
 * Note that the client request might start with /_cmsinternal/binaries/ but /_cmsinternal is removed from our wrapped
 * request hence we can also for  /_cmsinternal/binaries/ dispatch to the request.getPathInfo();
 * </P>
 */
public class BinariesDispatchingValve extends AbstractBaseOrderableValve {

    private static Logger log = LoggerFactory.getLogger(BinariesDispatchingValve.class);

    @Override
    public void invoke(ValveContext context) throws ContainerException {
        final HttpServletRequest request = context.getServletRequest();
        final HttpServletResponse response = context.getServletResponse();

        try {
            Task chainingTask = null;
            // the dispatchPath is normally just the normal pathInfo unless the client request starts with /_cmsinternal/binaries/xyz
            // or it did match some submount (/fr/binaries) its binaries location, which however in general should not happen
            String binariesDispatchPath = request.getPathInfo();
            if (binariesDispatchPath == null || !binariesDispatchPath.startsWith("/binaries/")) {
                final String msg = String.format("Illegal binariesDispatchPath '%s' for BinariesInvokingValve found for '%s'", binariesDispatchPath, request);
                throw new ContainerException(msg);
            }

            // to be able to correctly use RequestDispatcher#forward (and have the adjusted pathInfo) we need the unwrapped
            // servlet request.
            HttpServletRequest unwrappedRequest = request;
            while (unwrappedRequest instanceof HttpServletRequestWrapper) {
                if (unwrappedRequest instanceof HstContainerRequest) {
                    // Let's stop when the HST specific request wrapper has been unwrapped.
                    unwrappedRequest = (HttpServletRequest)((HttpServletRequestWrapper)unwrappedRequest).getRequest();
                    break;
                }
                unwrappedRequest = (HttpServletRequest)((HttpServletRequestWrapper)unwrappedRequest).getRequest();
            }
            try {
                if (HDC.isStarted()) {
                    chainingTask = HDC.getCurrentTask().startSubtask("Dispatching to Binaries Servlet from BinariesInvokingValve");
                }
                context.getRequestContext().getServletContext().getRequestDispatcher(binariesDispatchPath).forward(unwrappedRequest, response);
            } finally {
                if (chainingTask != null) {
                    chainingTask.stop();
                }
            }
        } catch (ContainerException e) {
            throw e;
        } catch (Exception e) {
            if (log.isDebugEnabled()) {
                log.warn("Failed to dispatch to binaries servlet.", e);
            } else {
                log.warn("Failed to dispatch to binaries servlet : {}", e.toString());
            }
        }
        context.invokeNext();
    }

}
