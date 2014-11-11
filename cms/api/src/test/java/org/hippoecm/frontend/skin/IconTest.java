/*
 * Copyright 2014 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.skin;

import org.apache.wicket.request.resource.PackageResource;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.apache.wicket.util.tester.WicketTester;
import org.easymock.classextension.EasyMock;
import org.hippoecm.frontend.service.IconSize;
import org.junit.Test;

import static org.easymock.EasyMock.expect;
import static org.easymock.classextension.EasyMock.replay;
import static org.junit.Assert.*;

public class IconTest extends WicketTester {

    @Test
    public void all_icons_exist() {
        for (Icon icon : Icon.values()) {
            final PackageResourceReference reference = icon.getReference();
            assertTrue("Icon does not exist: " + icon.name(), PackageResource.exists(reference.getKey()));
        }
    }

    @Test
    public void referenceByName_finds_icon() {
        final PackageResourceReference reference = Icon.referenceByName("bullet", IconSize.XLARGE, Icon.FOLDER_TINY);
        assertEquals("Icon.BULLET_XLARGE should have been returned", Icon.BULLET_XLARGE.getReference().getResource(), reference.getResource());
    }

    @Test
    public void referenceByName_returns_default_value_when_icon_does_not_exist() {
        final PackageResourceReference reference = Icon.referenceByName("no-such-icon", IconSize.MEDIUM, Icon.FOLDER_TINY);
        assertEquals("Default value should have been returned", Icon.FOLDER_TINY.getReference().getResource(), reference.getResource());
    }

    @Test
    public void referenceByName_can_return_default_value_null_when_icon_does_not_exist() {
        final PackageResourceReference reference = Icon.referenceByName("no-such-icon", IconSize.MEDIUM, null);
        assertNull("Default value null should not have been returned", reference);
    }

}