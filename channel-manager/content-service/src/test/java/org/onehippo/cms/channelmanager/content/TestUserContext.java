/*
 *  Copyright 2019-2022 Hippo B.V. (http://www.onehippo.com)
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

import java.util.Locale;
import java.util.TimeZone;

import javax.jcr.Session;

import static org.powermock.api.easymock.PowerMock.createMock;

public class TestUserContext extends UserContext {

    public static final Locale TEST_LOCALE = Locale.ENGLISH;
    public static final TimeZone TEST_TIME_ZONE = TimeZone.getTimeZone("Europe/Amsterdam");

    public TestUserContext() {
        super(createMock(Session.class), new MockCmsSessionContext(TEST_LOCALE, TEST_TIME_ZONE));
    }

    public TestUserContext(final Locale locale) {
        super(createMock(Session.class), new MockCmsSessionContext(locale, TEST_TIME_ZONE));
    }

    public TestUserContext(final Session session) {
        super(session, new MockCmsSessionContext(TEST_LOCALE, TEST_TIME_ZONE));
    }

}
