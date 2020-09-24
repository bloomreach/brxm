/*
 * Copyright 2019-2020 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *      http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.hippoecm.frontend.plugins.standards.perspective;

import org.apache.wicket.core.request.handler.IPartialPageRequestHandler;
import org.easymock.EasyMockRunner;
import org.easymock.Mock;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.onehippo.cms.json.Json;

import com.fasterxml.jackson.core.JsonProcessingException;

import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

@RunWith(EasyMockRunner.class)
public class ParentApiCallerTest {

    @SuppressWarnings("unused")
    @Mock
    private IPartialPageRequestHandler handler;

    private final ParentApi parentApi = new ParentApiCaller().setTargetSupplier(() -> handler);

    @Test
    public void updateNavLocation() {
        final String javascript = String.format(ParentApiCaller.JAVA_SCRIPT_TEMPLATE, "{\"path\":\"path\",\"breadcrumbLabel\":\"some breadcrumb label\",\"addHistory\":false}");
        handler.appendJavaScript(javascript);
        replay(handler);
        parentApi.updateNavLocation("path", "some breadcrumb label", false);
        verify(handler);
    }


}
