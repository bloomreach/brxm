/*
 *  Copyright 2019-2020 Hippo B.V. (http://www.onehippo.com)
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

import java.util.Collections;
import java.util.Locale;
import java.util.Map;

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
@PowerMockIgnore({"org.apache.logging.log4j.*", "javax.management.*"})
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

        final TranslatedViolation violation = new TranslatedViolation(Locale.getDefault(), "my-key");
        assertEquals("???my-key???", violation.getMessage());
        verifyAll();
    }

    @Test
    public void returnsMissingValueWhenBundleIsNotFound() {
        final LocalizationService localizationService = createMock(LocalizationService.class);
        expect(HippoServiceRegistry.getService(LocalizationService.class)).andReturn(localizationService);
        expect(localizationService.getResourceBundle("hippo:cms.validators", Locale.getDefault())).andReturn(null);
        replayAll();

        final TranslatedViolation violation = new TranslatedViolation(Locale.getDefault(), "my-key");
        assertEquals("???my-key???", violation.getMessage());
        verifyAll();
    }

    @Test
    public void returnsMissingValueWhenMessageIsNotFound() {
        final LocalizationService localizationService = createMock(LocalizationService.class);
        final ResourceBundle bundle = createMock(ResourceBundle.class);
        expect(HippoServiceRegistry.getService(LocalizationService.class)).andReturn(localizationService);
        expect(localizationService.getResourceBundle("hippo:cms.validators", Locale.getDefault())).andReturn(bundle);
        expect(bundle.getString("my-key", null)).andReturn(null);
        replayAll();

        final TranslatedViolation violation = new TranslatedViolation(Locale.getDefault(), "my-key");
        assertEquals("???my-key???", violation.getMessage());
        verifyAll();
    }

    @Test
    public void returnsMissingValueWhenFallbackMessageIsNotFound() {
        final LocalizationService localizationService = createMock(LocalizationService.class);
        final ResourceBundle bundle = createMock(ResourceBundle.class);
        expect(HippoServiceRegistry.getService(LocalizationService.class)).andReturn(localizationService);
        expect(localizationService.getResourceBundle("hippo:cms.validators", Locale.getDefault())).andReturn(bundle);
        expect(bundle.getString("my-key", null)).andReturn(null);
        expect(bundle.getString("fallback-key", null)).andReturn(null);
        replayAll();

        final TranslatedViolation violation = new TranslatedViolation(Locale.getDefault(), "my-key", "fallback-key");
        assertEquals("???my-key???", violation.getMessage());
        verifyAll();
    }

    @Test
    public void returnsTranslatedMessage() {
        final LocalizationService localizationService = createMock(LocalizationService.class);
        final ResourceBundle bundle = createMock(ResourceBundle.class);
        expect(HippoServiceRegistry.getService(LocalizationService.class)).andReturn(localizationService);
        expect(localizationService.getResourceBundle("hippo:cms.validators", Locale.getDefault())).andReturn(bundle);
        expect(bundle.getString("my-key", null)).andReturn("my-message");
        replayAll();

        final TranslatedViolation violation = new TranslatedViolation(Locale.getDefault(), "my-key");
        assertEquals("my-message", violation.getMessage());
        verifyAll();
    }

    @Test
    public void returnsTranslatedFallbackMessage() {
        final LocalizationService localizationService = createMock(LocalizationService.class);
        final ResourceBundle bundle = createMock(ResourceBundle.class);
        expect(HippoServiceRegistry.getService(LocalizationService.class)).andReturn(localizationService);
        expect(localizationService.getResourceBundle("hippo:cms.validators", Locale.getDefault())).andReturn(bundle);
        expect(bundle.getString("my-key", null)).andReturn(null);
        expect(bundle.getString("fallback-key", null)).andReturn("Fallback message");
        replayAll();

        final TranslatedViolation violation = new TranslatedViolation(Locale.getDefault(), "my-key", "fallback-key");
        assertEquals("Fallback message", violation.getMessage());
        verifyAll();
    }
    
    @Test
    public void returnsTranslatedMessageWithParameters() {
        final LocalizationService localizationService = createMock(LocalizationService.class);
        final ResourceBundle bundle = createMock(ResourceBundle.class);
        expect(HippoServiceRegistry.getService(LocalizationService.class)).andReturn(localizationService);
        expect(localizationService.getResourceBundle("hippo:cms.validators", Locale.getDefault())).andReturn(bundle);
        final Map<String, String> parameters = Collections.singletonMap("variable", "replacement");
        expect(bundle.getString("my-key", parameters)).andReturn("my-message replacement");
        replayAll();

        final TranslatedViolation violation = new TranslatedViolation(Locale.getDefault(), "my-key");
        violation.setParameters(parameters);
        assertEquals("my-message replacement", violation.getMessage());
        verifyAll();
    }
}
