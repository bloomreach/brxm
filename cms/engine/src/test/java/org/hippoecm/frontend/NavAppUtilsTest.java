/*
 * Copyright 2019 Hippo B.V. (http://www.onehippo.com)
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
 *
 */

package org.hippoecm.frontend;

import java.net.URI;
import java.util.function.Function;

import org.apache.wicket.request.resource.ResourceReference;
import org.easymock.EasyMock;
import org.hippoecm.frontend.service.AppSettings;
import org.junit.Test;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.hamcrest.CoreMatchers.endsWith;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.junit.Assert.assertThat;

public class NavAppUtilsTest {

    @Test
    public void getMapperWithAntiCache() {

        final AppSettings appSettings = EasyMock.createNiceMock(AppSettings.class);

        final URI location = URI.create("http://cms.company-x.com/navapp");

        expect(appSettings.isCmsServingNavAppResources()).andStubReturn(true);
        expect(appSettings.getNavAppResourceLocation()).andStubReturn(location);
        replay(appSettings);

        final Function<String, ResourceReference> mapper = NavAppUtils.getMapper(appSettings);
        final ResourceReference reference = mapper.apply("test.js");
        assertThat(reference.getName(), startsWith("/navapp/test.js?antiCache="));
    }

    @Test
    public void getMapperWithoutAntiCache() {

        final AppSettings appSettings = EasyMock.createNiceMock(AppSettings.class);

        final URI brXmLocation = URI.create("http://cms.company-x.com");
        final URI navAppLocation = URI.create("http://cdn.bloomreach.com/navapp-cdn");

        expect(appSettings.isCmsServingNavAppResources()).andStubReturn(false);
        expect(appSettings.getNavAppResourceLocation()).andStubReturn(navAppLocation);
        replay(appSettings);

        final Function<String, ResourceReference> mapper = NavAppUtils.getMapper(appSettings);
        final ResourceReference reference = mapper.apply("test.js");
        assertThat(reference.getName(), endsWith("navapp-cdn/test.js"));
    }
}
