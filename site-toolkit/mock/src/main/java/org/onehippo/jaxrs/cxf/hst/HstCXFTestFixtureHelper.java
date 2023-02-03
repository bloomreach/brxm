/*
 *  Copyright 2015-2023 Bloomreach
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
package org.onehippo.jaxrs.cxf.hst;

import java.io.IOException;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.ext.Provider;

import org.hippoecm.hst.container.ModifiableRequestContextProvider;
import org.hippoecm.hst.core.request.HstRequestContext;

/**
 * Helper class for setting up a CXFTest that uses the HST.
 * For example usage, see the tests in {@code org.onehippo.jaxrs.cxf.hst.TestHstCXFTestFixtureHelper}.
 */
@Provider
public class HstCXFTestFixtureHelper implements ContainerRequestFilter {

    private HstRequestContext hstRequestContext;

    public HstCXFTestFixtureHelper(final HstRequestContext hstRequestContext) {
        this.hstRequestContext= hstRequestContext;
    }

    @Override
    public void filter(final ContainerRequestContext containerRequestContext) throws IOException {
        ModifiableRequestContextProvider.set(hstRequestContext);
    }
}
