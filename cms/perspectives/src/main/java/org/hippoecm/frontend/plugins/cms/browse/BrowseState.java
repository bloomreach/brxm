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
package org.hippoecm.frontend.plugins.cms.browse;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.wicket.util.io.IClusterable;

public class BrowseState implements IClusterable {

    // change state
    private boolean sectionChanged;
    private boolean tabChanged;
    private boolean listingChanged;
    private boolean expandChanged;

    // render state
    private boolean expandDefault;
    private boolean expandListing;
    private boolean collapseAll;
    private boolean collapseListing;
    private boolean focusTabs;
    private boolean blurTabs;
    private boolean shelveSelection;
    private boolean restoreSelection;

    // value state
    private String tab;
    private String section;
    private boolean expanded;
    private Selection last;

    public void onSectionChanged(final String newSection) {
        sectionChanged = true;
        section = newSection;
    }

    public void onTabChanged(final String newTab) {
        tabChanged = true;
        tab = newTab;
    }

    public void onListingChanged() {
        listingChanged = true;
    }

    public void onExpand() {
        expandChanged = true;
        expanded = true;
    }

    public void onCollapse() {
        expandChanged = true;
        expanded = false;
    }

    public boolean processChanges(final boolean hasOpenTabs) {
        if (!isDirty()) {
            return false;
        }

        if (tabChanged) {
            if (tab == null) {
                // no (more) tabs open, clear selection
                last = null;
                if (!expanded) {
                    expandDefault = true;
                    expandListing = true;
                }
            } else {
                // tab received focus
                if (expanded) {
                    collapseAll = true;
                    collapseListing = true;
                }
                if (StringUtils.isNotEmpty(section) && StringUtils.isNotEmpty(tab)) {
                    last = new Selection(section, tab);
                }
            }
        } else {
            if (listingChanged || expandChanged) {
                if (expanded) {
                    expandListing = true;
                    shelveSelection = expandChanged && hasOpenTabs;
                    blurTabs = expandChanged && hasOpenTabs;
                } else {
                    collapseListing = true;
                    restoreSelection = expandChanged && currentSectionMatchesLastSection();
                    focusTabs = !restoreSelection && expandChanged && hasOpenTabs;
                }
            }

            if (sectionChanged) {
                if (isShelved()) {
                    restoreSelection = !expanded && currentSectionMatchesLastSection();
                    focusTabs = !restoreSelection && focusTabs;
                }
            }
        }

        if (last != null) {
            if (shelveSelection) {
                last.shelve();
            } else if(restoreSelection) {
                last.restore();
            }
        }

        return renderStateIsDirty();
    }

    private boolean isShelved() {
        return last != null && last.shelved;
    }

    private boolean renderStateIsDirty() {
        return expandDefault || collapseAll || collapseListing || expandListing || focusTabs || blurTabs ||
                restoreSelection || shelveSelection;
    }

    private boolean currentSectionMatchesLastSection() {
        return last != null && last.section.equals(section);
    }

    public boolean isDirty() {
        return sectionChanged || tabChanged || listingChanged || expandChanged;
    }

    public void reset() {
        sectionChanged = false;
        tabChanged = false;
        listingChanged = false;
        expandChanged = false;

        expandDefault = false;
        collapseAll = false;
        expandListing = false;
        collapseListing = false;
        focusTabs = false;
        blurTabs = false;
        shelveSelection = false;
        restoreSelection = false;
    }

    public boolean isExpanded() {
        return expanded;
    }

    public String getTab() {
        return last == null ? null : last.tab;
    }

    // Render state
    public boolean isExpandDefault() {
        return expandDefault;
    }

    public boolean isCollapseAll() {
        return collapseAll;
    }

    public boolean isExpandListing() {
        return expandListing;
    }

    public boolean isCollapseListing() {
        return collapseListing;
    }

    public boolean isFocusTabs() {
        return focusTabs;
    }

    public boolean isBlurTabs() {
        return blurTabs;
    }

    public boolean isShelveSelection() {
        return shelveSelection;
    }

    public boolean isRestoreSelection() {
        return restoreSelection;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).
                append("expandChanged", expandChanged).
                append("sectionChanged", sectionChanged).
                append("listingChanged", listingChanged).
                append("tabChanged", tabChanged).
                append("tab", tab).
                append("section", section).
                append("expanded", isExpanded()).
                append("last", last).
                append("expandDefault", expandDefault).
                append("expandListing", expandListing).
                append("collapseAll", collapseAll).
                append("collapseListing", collapseListing).
                append("focusTabs", focusTabs).
                append("blurTabs", blurTabs).
                append("shelveSelection", shelveSelection).
                append("restoreSelection", restoreSelection).
                toString();
    }

    private static class Selection implements IClusterable {
        boolean shelved;
        String tab;
        String section;

        public Selection(final String section, final String tab) {
            this.section = section;
            this.tab = tab;
        }

        @Override
        public String toString() {
            return new ToStringBuilder(this).
                    append("section", section).
                    append("tab", tab).
                    append("shelved", shelved).
                    toString();
        }

        public void shelve() {
            shelved = true;
        }

        public void restore() {
            shelved = false;
        }
    }
}
