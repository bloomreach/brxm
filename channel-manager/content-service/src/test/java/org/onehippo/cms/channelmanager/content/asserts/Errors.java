/*
 *  Copyright 2021 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms.channelmanager.content.asserts;

import java.io.Serializable;
import java.util.Map;

import javax.ws.rs.core.Response;

import org.onehippo.cms.channelmanager.content.error.ErrorInfo;
import org.onehippo.cms.channelmanager.content.error.ErrorWithPayloadException;

import static java.util.Collections.singletonMap;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public final class Errors {

    private Errors() {
    }

    public static void assertErrorStatusAndReason(final ErrorWithPayloadException e,
                                                  final Response.Status status,
                                                  final ErrorInfo.Reason reason) {
        assertErrorStatusAndReason(e, status, reason, null);
    }

    public static void assertErrorStatusAndReason(final ErrorWithPayloadException e,
                                                  final Response.Status status,
                                                  final ErrorInfo.Reason reason,
                                                  final String key,
                                                  final String value) {
        assertErrorStatusAndReason(e, status, reason, singletonMap(key, value));
    }

    public static void assertErrorStatusAndReason(final ErrorWithPayloadException e,
                                                  final Response.Status status,
                                                  final ErrorInfo.Reason reason,
                                                  final Map<String, Serializable> params) {
        assertTrue(e.getPayload() instanceof ErrorInfo);

        final ErrorInfo errorInfo = (ErrorInfo) e.getPayload();
        assertThat(e.getStatus(), is(status));
        assertThat(errorInfo.getReason(), is(reason));

        final Map<String, Serializable> exceptionParams = errorInfo.getParams();
        if (params == null) {
            assertNull(exceptionParams);
        } else {
            params.forEach((name, value) -> {
                assertThat(exceptionParams.get(name), is(value));
            });
        }
    }

}
