/*
 *  Copyright 2008 Hippo.
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
package org.hippoecm.frontend;

import org.hippoecm.frontend.config.PluginConfigTest;
import org.hippoecm.frontend.editor.compare.NodeComparerTest;
import org.hippoecm.frontend.editor.compare.ValueComparerTest;
import org.hippoecm.frontend.i18n.model.NodeTranslatorTest;
import org.hippoecm.frontend.model.*;
import org.hippoecm.frontend.model.event.ObservationTest;
import org.hippoecm.frontend.model.map.JcrValueListTest;
import org.hippoecm.frontend.model.ocm.JcrObjectTest;
import org.hippoecm.frontend.plugin.config.impl.JcrConfigServiceFactoryTest;
import org.hippoecm.frontend.plugins.standards.browse.BrowseServiceTest;
import org.hippoecm.frontend.plugins.standards.list.SearchDocumentsProviderTest;
import org.hippoecm.frontend.plugins.standards.search.TextSearchTest;
import org.hippoecm.frontend.plugins.standards.tabs.TabsTest;
import org.hippoecm.frontend.session.UserSessionTest;
import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(PluginSuite.class)
@Suite.SuiteClasses({
    PluginConfigTest.class,
    JcrItemModelTest.class,
    JcrPropertyModelTest.class,
    JcrPropertyValueModelTest.class,
    JcrValueListTest.class,
    PropertyValueProviderTest.class,
    JcrHelperTest.class,
    JcrMultiPropertyValueModelTest.class,
//    ObservationTest.class,
    JcrObjectTest.class,
    JcrConfigServiceFactoryTest.class,

    NodeTranslatorTest.class,
    UserSessionTest.class,
    
    TabsTest.class,
    TextSearchTest.class,
    SearchDocumentsProviderTest.class,
    BrowseServiceTest.class,

    NodeComparerTest.class,
    ValueComparerTest.class
})
public class EmbeddedTest
{
}
