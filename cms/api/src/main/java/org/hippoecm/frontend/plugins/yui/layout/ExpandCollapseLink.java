/*
 *  Copyright 2010-2014 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.plugins.yui.layout;

import org.apache.wicket.MarkupContainer;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.panel.Panel;
import org.hippoecm.frontend.plugins.standards.icon.HippoIcon;
import org.hippoecm.frontend.skin.Icon;

public class ExpandCollapseLink extends Panel {

    private Boolean hasExpandableParent;
    private final HippoIcon expandButton;
    private final HippoIcon collapseButton;

    public ExpandCollapseLink(String id, boolean isExpanded) {
        super(id);

        expandButton = HippoIcon.fromSprite("expandButton", Icon.EXPAND_TINY);
        add(expandButton);

        collapseButton = HippoIcon.fromSprite("collapseButton", Icon.COLLAPSE_TINY);
        add(collapseButton);

        setExpanded(isExpanded);
    }

    @Override
    public boolean isVisible() {
        return hasExpandableParent();
    }

    @Override
    public boolean isEnabled() {
        return hasExpandableParent();
    }

    public void setExpanded(boolean isExpanded) {
        collapseButton.setVisible(isExpanded);
        expandButton.setVisible(!isExpanded);
    }

    private boolean hasExpandableParent() {
        if(hasExpandableParent == null) {
            hasExpandableParent = false;
            MarkupContainer parent = getParent();
            while (parent != null) {
                for (Behavior behavior : parent.getBehaviors()) {
                    if (behavior instanceof WireframeBehavior) {
                        WireframeBehavior wireframe = (WireframeBehavior) behavior;
                        if (wireframe.hasExpandableUnit()) {
                            hasExpandableParent = true;
                            break;
                        }
                    }
                }
                parent = parent.getParent();
            }
        }
        return hasExpandableParent;
    }

    @Override
    protected void onComponentTag(final ComponentTag tag) {
        super.onComponentTag(tag);
        tag.put("onclick", "YAHOO.hippo.LayoutManager.handleExpandCollapse(this); return false;");
    }

    @Override
    protected void onDetach() {
        super.onDetach();
        hasExpandableParent = null;
    }
}
