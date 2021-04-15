/*
 *  Copyright 2020-2021 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.errors;

import java.nio.charset.StandardCharsets;

import javax.servlet.http.HttpServletResponse;

import org.apache.wicket.WicketRuntimeException;
import org.apache.wicket.request.IRequestCycle;
import org.apache.wicket.request.IRequestHandler;
import org.apache.wicket.request.http.WebResponse;

public class UnexpectedErrorRequestHandler implements IRequestHandler {

    private final String errorMessage;

    public UnexpectedErrorRequestHandler(final Exception e) {
        errorMessage = "<error type=\"lenient\">" + getMessage(e) + "</error>";
    }

    private static String getMessage(final Throwable throwable) {
        Throwable cause = throwable;
        while (cause.getCause() != null && cause != cause.getCause()) {
            cause = cause.getCause();
        }
        if (cause instanceof WicketRuntimeException) {
            return cause.getClass().getSimpleName();
        }
        return cause.getMessage();
    }

    @Override
    public void respond(final IRequestCycle requestCycle) {
        final WebResponse response = (WebResponse) requestCycle.getResponse();
        response.setContentType("text/xml;charset=UTF8");
        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);

        final byte[] bytes = errorMessage.getBytes(StandardCharsets.UTF_8);
        response.setContentLength(bytes.length);
        response.write(bytes);
    }

    @Override
    public void detach(final IRequestCycle requestCycle) {
        // There is only immutable state in this handler so there is nothing to detach.
    }
}
