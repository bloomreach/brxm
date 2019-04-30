/*
 *  Copyright 2019 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms.services.validation;

import java.util.Locale;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.repository.l10n.LocalizationService;
import org.onehippo.repository.l10n.ResourceBundle;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;
import static org.powermock.api.easymock.PowerMock.createMock;
import static org.powermock.api.easymock.PowerMock.mockStaticPartial;
import static org.powermock.api.easymock.PowerMock.replayAll;
import static org.powermock.api.easymock.PowerMock.verifyAll;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore("javax.management.*")
@PrepareForTest(HippoServiceRegistry.class)
public class TranslatedViolationTest {

    @Before
    public void setUp() {
        mockStaticPartial(HippoServiceRegistry.class, "getService");
    }

    @Test
    public void returnsMissingValueWhenLocalizationServiceIsNotFound() {
        expect(HippoServiceRegistry.getService(LocalizationService.class)).andReturn(null);
        replayAll();

        final TranslatedViolation violation = new TranslatedViolation("my-key", Locale.getDefault());
        assertEquals("???my-key???", violation.getMessage());
        verifyAll();
    }

    @Test
    public void returnsMissingValueWhenBundleIsNotFound() {
        final LocalizationService localizationService = createMock(LocalizationService.class);
        expect(HippoServiceRegistry.getService(LocalizationService.class)).andReturn(localizationService);
        expect(localizationService.getResourceBundle("hippo:cms.validators", Locale.getDefault())).andReturn(null);
        replayAll();

        final TranslatedViolation violation = new TranslatedViolation("my-key", Locale.getDefault());
        assertEquals("???my-key???", violation.getMessage());
        verifyAll();
    }

    @Test
    public void returnsMissingValueWhenMessageIsNotFound() {
        final LocalizationService localizationService = createMock(LocalizationService.class);
        final ResourceBundle bundle = createMock(ResourceBundle.class);
        expect(HippoServiceRegistry.getService(LocalizationService.class)).andReturn(localizationService);
        expect(localizationService.getResourceBundle("hippo:cms.validators", Locale.getDefault())).andReturn(bundle);
        expect(bundle.getString("my-key")).andReturn(null);
        replayAll();

        final TranslatedViolation violation = new TranslatedViolation("my-key", Locale.getDefault());
        assertEquals("???my-key???", violation.getMessage());
        verifyAll();
    }

    @Test
    public void returnsTranslatedMessage() {
        final LocalizationService localizationService = createMock(LocalizationService.class);
        final ResourceBundle bundle = createMock(ResourceBundle.class);
        expect(HippoServiceRegistry.getService(LocalizationService.class)).andReturn(localizationService);
        expect(localizationService.getResourceBundle("hippo:cms.validators", Locale.getDefault())).andReturn(bundle);
        expect(bundle.getString("my-key")).andReturn("my-message");
        replayAll();

        final TranslatedViolation violation = new TranslatedViolation("my-key", Locale.getDefault());
        assertEquals("my-message", violation.getMessage());
        verifyAll();
    }
}
