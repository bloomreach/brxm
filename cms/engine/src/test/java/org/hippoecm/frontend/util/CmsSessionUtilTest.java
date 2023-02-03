/*
 * Copyright 2016-2023 Bloomreach
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
package org.hippoecm.frontend.util;

import java.util.Locale;
import java.util.TimeZone;

import javax.jcr.Credentials;
import javax.jcr.SimpleCredentials;

import org.hippoecm.frontend.session.PluginUserSession;
import org.junit.Test;
import org.onehippo.cms7.services.cmscontext.CmsInternalCmsContextService;
import org.onehippo.cms7.services.cmscontext.CmsSessionContext;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

public class CmsSessionUtilTest {
    @Test
    public void populate_cms_session_context() {
        final CmsInternalCmsContextService cmsContextService = createMock(CmsInternalCmsContextService.class);
        final PluginUserSession pluginUserSession = createMock(PluginUserSession.class);
        final CmsSessionContext cmsSessionContext = createMock(CmsSessionContext.class);
        final Credentials credentials = new SimpleCredentials("test", new char[]{});
        final Locale locale = new Locale("nl");
        final TimeZone timeZone = TimeZone.getTimeZone("Europe/Amsterdam");

        expect(pluginUserSession.getCredentials()).andReturn(credentials);
        expect(pluginUserSession.getLocale()).andReturn(locale);
        expect(pluginUserSession.getTimeZone()).andReturn(timeZone);
        cmsContextService.setData(cmsSessionContext, CmsSessionContext.REPOSITORY_CREDENTIALS, credentials);
        expectLastCall();
        cmsContextService.setData(cmsSessionContext, CmsSessionContext.LOCALE, locale);
        expectLastCall();
        cmsContextService.setData(cmsSessionContext, CmsSessionContext.TIME_ZONE, timeZone);
        expectLastCall();
        replay(cmsContextService, pluginUserSession);

        CmsSessionUtil.populateCmsSessionContext(cmsContextService, cmsSessionContext, pluginUserSession);

        verify();
    }
}
