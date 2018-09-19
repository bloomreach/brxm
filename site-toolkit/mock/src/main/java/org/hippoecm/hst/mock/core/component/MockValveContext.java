/*
 * Copyright 2014-2018 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.hst.mock.core.component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.hst.core.container.ContainerException;
import org.hippoecm.hst.core.container.HstComponentWindow;
import org.hippoecm.hst.core.container.HstContainerConfig;
import org.hippoecm.hst.core.container.PageCacheContext;
import org.hippoecm.hst.core.container.ValveContext;
import org.hippoecm.hst.core.request.HstRequestContext;

public class MockValveContext implements ValveContext {

    private final HstRequest request;
    private final HstResponse response;
    private boolean nextValveInvoked = false;
    private HstComponentWindow rootComponentWindow;
    private HstComponentWindow rootComponentRenderingWindow;

    public MockValveContext(final HstRequest request, final HstResponse response) {
        this.request = request;
        this.response = response;
    }

    @Override
    public void invokeNext() throws ContainerException {
        nextValveInvoked = true;
    }

    public boolean isNextValveInvoked() {
        return nextValveInvoked;
    }

    @Override
    public HstContainerConfig getRequestContainerConfig() {
        throw new UnsupportedOperationException();
    }

    @Override
    public HstRequestContext getRequestContext() {
        return request.getRequestContext();
    }

    @Override
    public HttpServletRequest getServletRequest() {
        return request;
    }

    @Override
    public HttpServletResponse getServletResponse() {
        return response;
    }

    @Override
    public void setRootComponentWindow(final HstComponentWindow rootComponentWindow) {
        this.rootComponentWindow = rootComponentWindow;
    }

    @Override
    public HstComponentWindow getRootComponentWindow() {
        return rootComponentWindow;
    }

    @Override
    public void setRootComponentRenderingWindow(final HstComponentWindow rootComponentRenderingWindow) {
        this.rootComponentRenderingWindow = rootComponentRenderingWindow;
    }

    @Override
    public HstComponentWindow getRootComponentRenderingWindow() {
        return rootComponentRenderingWindow;
    }

    @Override
    public PageCacheContext getPageCacheContext() {
        throw new UnsupportedOperationException();
    }
}
