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
class TestSuite {
}
