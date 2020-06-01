/*
 *  Copyright 2015 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.skin;

import org.apache.cxf.common.util.StringUtils;
import org.apache.wicket.util.tester.WicketTester;
import org.hippoecm.frontend.service.IconSize;
import org.junit.Test;

import static org.junit.Assert.assertFalse;

public class CmsIconTest extends WicketTester {

    @Test
    public void cms_contains_all_icons() {
        for (CmsIcon icon : CmsIcon.values()) {
            assertFalse("CMS does not contain icon '" + icon + "'",
                    StringUtils.isEmpty(icon.getInlineSvg(IconSize.M)));
        }
    }
}
