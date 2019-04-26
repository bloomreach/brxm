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
package org.onehippo.cms.services.validation.util;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.cms.services.validation.api.internal.ValidationService;
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
public class ServiceUtilsTest {

    @Before
    public void setUp() {
        mockStaticPartial(HippoServiceRegistry.class, "getService");
    }

    @Test(expected = IllegalStateException.class)
    public void throwsExceptionWhenServiceIsNotFound() {
        expect(HippoServiceRegistry.getService(ValidationService.class)).andReturn(null);
        replayAll();

        ServiceUtils.getValidationService();
    }

    @Test
    public void getValidationService() {
        final ValidationService validationService = createMock(ValidationService.class);
        expect(HippoServiceRegistry.getService(ValidationService.class)).andReturn(validationService);
        replayAll();

        assertEquals(validationService, ServiceUtils.getValidationService());
        verifyAll();
    }

}
