/*
 * Copyright 2019 Hippo B.V. (http://www.onehippo.com)
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

package org.hippoecm.frontend.service;

import java.util.function.Supplier;

import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.resource.ResourceReference;
import org.apache.wicket.util.tester.WicketTester;
import org.easymock.EasyMockRunner;
import org.easymock.Mock;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;

@RunWith(EasyMockRunner.class)
public class WicketFaviconServiceImplTest {

    public static final String WICKET_FAVICON_PREFIX = "./wicket/resource/org.hippoecm.frontend.service.WicketFaviconServiceImpl/";

    // Needed for RequestCycle.get()
    @SuppressWarnings("FieldCanBeLocal")
    private WicketTester wicketTester;

    @Mock
    private Supplier<String> pluginApplicationNameSupplier;

    private WicketFaviconServiceImpl faviconService;

    @Before
    public void setUp() throws Exception {
        wicketTester = new WicketTester();
        faviconService = new WicketFaviconServiceImpl(pluginApplicationNameSupplier);
    }

    @Test
    public void getFaviconResourceReferenceCms() {
        expect(pluginApplicationNameSupplier.get()).andReturn("cms");
        replay(pluginApplicationNameSupplier);
        final ResourceReference faviconResourceReference = faviconService.getFaviconResourceReference();
        assertEquals(WICKET_FAVICON_PREFIX +
                "cms-icon.png", getResourceReferenceUrl(faviconResourceReference));
    }

    @Test
    public void getFaviconResourceReferenceConsole() {
        expect(pluginApplicationNameSupplier.get()).andReturn("console");
        replay(pluginApplicationNameSupplier);
        final ResourceReference faviconResourceReference = faviconService.getFaviconResourceReference();
        assertEquals(WICKET_FAVICON_PREFIX +
                "console-icon.png", getResourceReferenceUrl(faviconResourceReference));
    }

    @Test
    public void getFaviconResourceReferenceResourceDoesNotExist() {
        expect(pluginApplicationNameSupplier.get()).andReturn("authoring");
        replay(pluginApplicationNameSupplier);
        final ResourceReference faviconResourceReference = faviconService.getFaviconResourceReference();
        assertEquals("skin/images/cms-icon.png", getResourceReferenceUrl(faviconResourceReference));
    }

    protected String getResourceReferenceUrl(final ResourceReference faviconResourceReference) {
        return RequestCycle.get().urlFor(faviconResourceReference, null).toString();
    }
}
