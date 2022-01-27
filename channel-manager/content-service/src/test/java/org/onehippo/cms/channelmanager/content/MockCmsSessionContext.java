/*
 *  Copyright 2021-2022 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms.channelmanager.content;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import javax.jcr.SimpleCredentials;

import org.onehippo.cms7.services.cmscontext.CmsSessionContext;

public class MockCmsSessionContext implements CmsSessionContext {

    private final Map<String, Serializable> contextPayload = new HashMap<>();
    private final Locale locale;
    private final TimeZone timeZone;

    public MockCmsSessionContext(final Locale locale, final TimeZone timeZone) {
        this.locale = locale;
        this.timeZone = timeZone;
    }

    public MockCmsSessionContext() {
        locale = null;
        timeZone = null;
    }

    @Override
    public String getId() {
       throw new UnsupportedOperationException();
    }

    @Override
    public String getCmsContextServiceId() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object get(final String key) {
        throw new UnsupportedOperationException();
    }

    @Override
    public SimpleCredentials getRepositoryCredentials() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Locale getLocale() {
        return locale;
    }

    @Override
    public TimeZone getTimeZone() {
        return timeZone;
    }

    @Override
    public Map<String, Serializable> getContextPayload() {
        return contextPayload;
    }
}
