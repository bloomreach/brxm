/*
 *  Copyright 2015-2020 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.plugins.cms.browse;

import org.hamcrest.core.IsNull;
import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class BrowseStateTest {

    public static final boolean WITHOUT_TABS = false;
    public static final boolean WITH_TABS = true;

    @Test
    public void testNotDirtyIfNoChanges() throws Exception {
        BrowseState state = new BrowseState();
        assertFalse(state.processChanges(WITHOUT_TABS));
        assertFalse(state.processChanges(WITH_TABS));
    }

    @Test
    public void testDirty() throws Exception {
        BrowseState state = new BrowseState();
        state.onSectionChanged("section1");
        assertTrue(state.isDirty());

        state = new BrowseState();
        state.onTabChanged("tab1");
        assertTrue(state.isDirty());

        state = new BrowseState();
        state.onListingChanged();
        assertTrue(state.isDirty());

        state = new BrowseState();
        state.onExpand();
        assertTrue(state.isDirty());

        state = new BrowseState();
        state.onCollapse();
        assertTrue(state.isDirty());

        state = new BrowseState();
        state.onNavLocationChanged(new NavLocation("path", "label"));
        assertTrue(state.isDirty());
    }

    @Test
    public void testNotDirtyAfterReset() throws Exception {
        BrowseState state = new BrowseState();
        state.onSectionChanged("section1");
        state.reset();
        assertFalse(state.isDirty());

        state = new BrowseState();
        state.onTabChanged("tab1");
        state.reset();
        assertFalse(state.isDirty());

        state = new BrowseState();
        state.onListingChanged();
        state.reset();
        assertFalse(state.isDirty());

        state = new BrowseState();
        state.onExpand();
        state.reset();
        assertFalse(state.isDirty());

        state = new BrowseState();
        state.onCollapse();
        state.reset();
        assertFalse(state.isDirty());

        state = new BrowseState();
        state.onNavLocationChanged(new NavLocation("path", "label"));
        state.reset();
        assertFalse(state.isDirty());
    }

    @Test
    public void testOnListingChanged() throws Exception {
        BrowseState state = new BrowseState();

        // default state is collapsed
        state.onListingChanged();
        assertTrue(state.processChanges(WITHOUT_TABS));
        assertTrue(state.isCollapseListing());

        state.onExpand();
        state.reset();
        state.onListingChanged();
        assertTrue(state.processChanges(WITHOUT_TABS));
        assertTrue(state.isExpandListing());

        state.onCollapse();
        state.reset();
        state.onListingChanged();
        assertTrue(state.processChanges(WITHOUT_TABS));
        assertTrue(state.isCollapseListing());
    }

    @Test
    public void testExpandedStateIsSetAndPreservedAfterReset() throws Exception {
        BrowseState state = new BrowseState();
        assertFalse(state.isExpanded());

        state.onExpand();
        assertTrue(state.isExpanded());
        state.reset();
        assertTrue(state.isExpanded());

        state.onCollapse();
        assertFalse(state.isExpanded());
        state.reset();
        assertFalse(state.isExpanded());
    }

    @Test
    public void testExpandCollapseWithoutTabs() throws Exception {
        BrowseState state = new BrowseState();

        state.onExpand();
        assertTrue(state.processChanges(WITHOUT_TABS));
        assertTrue(state.isExpandListing());
        assertFalse(state.isShelveSelection());
        assertFalse(state.isBlurTabs());

        state.reset();
        state.onCollapse();
        assertTrue(state.processChanges(WITHOUT_TABS));
        assertTrue(state.isCollapseListing());
        assertFalse(state.isRestoreSelection());
        assertFalse(state.isFocusTabs());
    }

    @Test
    public void testExpandCollapseWithTabs() throws Exception {
        BrowseState state = new BrowseState();
        // a section is required to store the last selected tab
        state.onSectionChanged("section1");
        state.onTabChanged("tab1");
        state.processChanges(WITH_TABS);

        state.reset();
        state.onExpand();
        assertTrue(state.processChanges(WITH_TABS));
        assertTrue(state.isExpandListing());
        assertTrue(state.isBlurTabs());
        assertTrue(state.isShelveSelection());

        state.reset();
        state.onCollapse();
        assertTrue(state.processChanges(WITH_TABS));
        assertTrue(state.isCollapseListing());
        assertTrue(state.isRestoreSelection());
        assertFalse(state.isFocusTabs());
    }

    @Test
    public void testCollapseOnTabChange() throws Exception {
        BrowseState state = new BrowseState();
        // a section is required to store the last selected tab
        state.onSectionChanged("section1");
        state.onTabChanged("tab1");
        state.processChanges(WITH_TABS);

        state.reset();
        state.onExpand();
        state.onTabChanged("tab1");
        state.processChanges(WITH_TABS);
        assertTrue(state.isCollapseAll());
        assertTrue(state.isCollapseListing());

        state.reset();
        state.onExpand();
        state.processChanges(WITH_TABS);

        state.reset();
        state.onTabChanged("tab2");
        state.processChanges(WITH_TABS);
        assertTrue(state.isCollapseAll());
        assertTrue(state.isCollapseListing());
    }

    @Test
    public void testFocusBlurWithoutTabs() throws Exception {
        BrowseState state = new BrowseState();
        state.onExpand();
        assertTrue(state.processChanges(WITHOUT_TABS));
        assertFalse(state.isFocusTabs());
        assertFalse(state.isBlurTabs());
        assertFalse(state.isRestoreSelection());
        assertFalse(state.isShelveSelection());

        state.reset();
        state.onCollapse();
        assertTrue(state.processChanges(WITHOUT_TABS));
        assertFalse(state.isFocusTabs());
        assertFalse(state.isBlurTabs());
        assertFalse(state.isRestoreSelection());
        assertFalse(state.isShelveSelection());
    }

    @Test
    public void testFocusBlurWithTabs() throws Exception {
        BrowseState state = new BrowseState();
        state.onSectionChanged("section1");
        state.onTabChanged("tab1");
        state.processChanges(WITH_TABS);

        state.reset();
        state.onExpand();
        assertTrue(state.processChanges(WITH_TABS));
        assertTrue(state.isBlurTabs());
        assertTrue(state.isShelveSelection());
        assertFalse(state.isFocusTabs());
        assertFalse(state.isRestoreSelection());

        state.reset();
        state.onCollapse();
        assertTrue(state.processChanges(WITH_TABS));
        assertTrue(state.isRestoreSelection());
        assertFalse(state.isFocusTabs());
        assertFalse(state.isBlurTabs());
        assertFalse(state.isShelveSelection());

        state.reset();
        state.onExpand();
        state.processChanges(WITH_TABS);

        // should not restore last-selected-tab
        state.reset();
        state.onTabChanged("tab2");
        assertTrue(state.processChanges(WITH_TABS));
        assertFalse(state.isFocusTabs());
        assertFalse(state.isBlurTabs());
        assertFalse(state.isRestoreSelection());
        assertFalse(state.isShelveSelection());
    }

    @Test
    public void testRestoreLastSelectedTab() {
        BrowseState state = new BrowseState();

        // open a tab
        state.onSectionChanged("section1");
        state.onTabChanged("tab1");
        state.processChanges(WITH_TABS);
        state.reset();

        // expand listing
        state.onExpand();
        state.processChanges(WITH_TABS);
        state.reset();

        // change to new section
        state.onSectionChanged("section2");
        state.processChanges(WITH_TABS);
        assertFalse(state.isFocusTabs());
        assertFalse(state.isBlurTabs());
        assertFalse(state.isShelveSelection());
        state.reset();

        // collapse listing
        state.onCollapse();
        state.processChanges(WITH_TABS);
        assertTrue(state.isFocusTabs());
        assertFalse(state.isBlurTabs());
        assertFalse(state.isRestoreSelection());
        assertFalse(state.isShelveSelection());
        state.reset();

        // change to initial section
        state.onSectionChanged("section1");
        state.processChanges(WITH_TABS);
        assertTrue(state.isRestoreSelection());
        assertFalse(state.isFocusTabs());
        assertFalse(state.isBlurTabs());
        assertFalse(state.isShelveSelection());
    }

    @Test
    public void testShelveSelectedTab() {
        BrowseState state = new BrowseState();

        // open a tab
        state.onSectionChanged("section1");
        state.onTabChanged("tab1");
        state.processChanges(WITH_TABS);
        state.reset();

        // change to new section
        state.onSectionChanged("section2");
        state.processChanges(WITH_TABS);
        assertFalse(state.isBlurTabs());
        assertFalse(state.isShelveSelection());
        assertFalse(state.isFocusTabs());
        assertFalse(state.isRestoreSelection());
        state.reset();

        // change to initial section
        state.onSectionChanged("section1");
        state.processChanges(WITH_TABS);
        assertFalse(state.isRestoreSelection());
        assertFalse(state.isFocusTabs());
        assertFalse(state.isBlurTabs());
        assertFalse(state.isShelveSelection());
    }

    @Test
    public void testOnTabChangedToNull() throws Exception {
        BrowseState state = new BrowseState();
        // a section is required to store the last selected tab
        state.onSectionChanged("section1");
        state.reset();

        state.onTabChanged(null);
        assertTrue(state.processChanges(WITHOUT_TABS));
        assertFalse(state.isCollapseAll());
        assertFalse(state.isCollapseListing());
        assertTrue(state.isExpandDefault());
        assertTrue(state.isExpandListing());
        assertThat(state.getTab(), IsNull.nullValue());
        state.reset();

        state.onExpand();
        state.reset();

        state.onTabChanged(null);
        assertFalse(state.processChanges(WITHOUT_TABS));
        assertThat(state.getTab(), IsNull.nullValue());
        state.reset();

        state.onTabChanged("tab1");
        assertTrue(state.processChanges(WITH_TABS));
        state.reset();

        state.onCollapse();
        state.reset();

        state.onTabChanged(null);
        assertTrue(state.processChanges(WITHOUT_TABS));
        assertFalse(state.isCollapseAll());
        assertFalse(state.isCollapseListing());
        assertTrue(state.isExpandDefault());
        assertTrue(state.isExpandListing());
        assertThat(state.getTab(), IsNull.nullValue());
    }

    @Test
    public void testOnTabChanged() throws Exception {
        BrowseState state = new BrowseState();
        // a section is required to store the last selected tab
        state.onSectionChanged("section1");
        state.onExpand();
        state.reset();

        state.onTabChanged("tab1");
        assertTrue(state.processChanges(WITH_TABS));
        assertTrue(state.isCollapseAll());
        assertTrue(state.isCollapseListing());
        assertFalse(state.isExpandDefault());
        assertFalse(state.isExpandListing());
        assertThat(state.getTab(), is("tab1"));

        state.onCollapse();
        state.reset();

        state.onTabChanged("tab2");
        assertFalse(state.processChanges(WITH_TABS));
        assertThat(state.getTab(), is("tab2"));
    }
}
