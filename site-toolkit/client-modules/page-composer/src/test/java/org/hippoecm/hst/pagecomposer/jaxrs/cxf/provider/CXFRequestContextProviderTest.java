/*
 * Copyright 2014 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.hippoecm.hst.pagecomposer.jaxrs.cxf.provider;

import javax.servlet.http.HttpServletRequest;

import org.apache.cxf.message.Message;
import org.hippoecm.hst.core.container.ContainerConstants;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.junit.Test;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class CXFRequestContextProviderTest {

    @Test
    public void testCreateContext() {

        final Message message = createMock(Message.class);
        final HttpServletRequest request = createMock(HttpServletRequest.class);
        final HstRequestContext context = createMock(HstRequestContext.class);

        expect(message.get("HTTP.REQUEST")).andReturn(request);
        expect(request.getAttribute(ContainerConstants.HST_REQUEST_CONTEXT)).andReturn(context);
        replay(message, request);

        final CXFRequestContextProvider provider = new CXFRequestContextProvider();
        assertThat(provider.createContext(message), is(context));
    }
}
