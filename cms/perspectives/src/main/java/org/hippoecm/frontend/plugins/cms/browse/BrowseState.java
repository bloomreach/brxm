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

import java.util.Objects;

import javax.jcr.Node;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.apache.wicket.model.IModel;
import org.apache.wicket.util.io.IClusterable;
import org.hippoecm.frontend.model.JcrNodeModel;

public class BrowseState implements IClusterable {

    // UI events
    private boolean sectionChanged;
    private boolean tabChanged;
    private boolean listingChanged;
    private boolean expandChanged;
    private boolean navLocationChanged;

    // render actions
    private boolean expandDefault;
    private boolean expandListing;
    private boolean collapseAll;
    private boolean collapseListing;
    private boolean focusTabs;
    private boolean blurTabs;
    private boolean shelveSelection;
    private boolean restoreSelection;

    // UI state
    private String tab;
    private String section;
    private boolean expanded;
    private Selection last;
    private NavLocation navLocation;

    public void onSectionChanged(final String newSection) {
        sectionChanged = true;
        section = newSection;
    }

    public void onTabChanged(final String newTab) {
        if (tab != null && newTab == null) {
            onLastTabClosed(tab);
        }
        tabChanged = true;
        tab = newTab;
    }

    private void onLastTabClosed(final String path) {
        if (navLocation != null && path.equals(navLocation.getPath())) {
            final JcrNodeModel documentModel = new JcrNodeModel(path);
            final IModel<Node> folderModel = documentModel.getParentModel();
            onNavLocationChanged(NavLocation.folder(folderModel, NavLocation.Mode.ADD));
        }
    }

    public void onListingChanged() {
        listingChanged = true;
    }

    public void onNavLocationChanged(final NavLocation newNavLocation) {
        if (!Objects.equals(navLocation, newNavLocation)) {
            navLocationChanged = true;
            navLocation = newNavLocation;
        }
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
                blurTabs = hasOpenTabs;
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
                // Note that here, "expanded" already contains the new value, i.e. after the expansion/collapsing. See onExpand/onCollapse.
                if (expanded) {
                    expandListing = true;
                    shelveSelection = expandChanged && hasOpenTabs;
                    blurTabs = expandChanged && hasOpenTabs;
                } else {
                    collapseListing = true;
                    restoreSelection = expandChanged && currentSectionMatchesLastSection();
                    focusTabs = expandChanged && !currentSectionMatchesLastSection() && hasOpenTabs;
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
                restoreSelection || shelveSelection || navLocationChanged;
    }

    private boolean currentSectionMatchesLastSection() {
        return last != null && last.section.equals(section);
    }

    public boolean isDirty() {
        return sectionChanged || tabChanged || listingChanged || expandChanged || navLocationChanged;
    }

    public void reset() {
        sectionChanged = false;
        tabChanged = false;
        listingChanged = false;
        expandChanged = false;
        navLocationChanged = false;

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

    public NavLocation getNavLocation() {
        return navLocation;
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

    public boolean isUpdateNavLocation() {
        return navLocationChanged;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE).
                append("blurTabs", blurTabs).
                append("collapseAll", collapseAll).
                append("collapseListing", collapseListing).
                append("expandChanged", expandChanged).
                append("expandDefault", expandDefault).
                append("expanded", isExpanded()).
                append("expandListing", expandListing).
                append("focusTabs", focusTabs).
                append("last", last).
                append("listingChanged", listingChanged).
                append("navLocation", navLocation).
                append("navLocationChanged", navLocationChanged).
                append("restoreSelection", restoreSelection).
                append("section", section).
                append("sectionChanged", sectionChanged).
                append("shelveSelection", shelveSelection).
                append("tab", tab).
                append("tabChanged", tabChanged).
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
