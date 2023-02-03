/*
 * Copyright 2011-2023 Bloomreach
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
package org.hippoecm.frontend.plugins.cms;

import org.hippoecm.frontend.PluginSuite;
import org.hippoecm.frontend.plugins.cms.browse.service.BrowseServiceTest;
import org.hippoecm.frontend.plugins.cms.dashboard.BrowseLinkTargetTest;
import org.hippoecm.frontend.plugins.cms.dashboard.EventLabelTest;
import org.hippoecm.frontend.plugins.cms.edit.EditorManagerTest;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(PluginSuite.class)
@Suite.SuiteClasses({
    EditorManagerTest.class,
    BrowseServiceTest.class,
    EventLabelTest.class,
    BrowseLinkTargetTest.class
})
public class TestSuite {
}
