/*
 *  Copyright 2010-2013 Hippo B.V. (http://www.onehippo.com)
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
import org.apache.wicket.markup.html.WebMarkupContainer;

public class ExpandCollapseLink extends WebMarkupContainer {

    private Boolean hasExpandableParent;

    public ExpandCollapseLink(String id) {
        super(id);
    }

    @Override
    public boolean isVisible() {
        return hasExpandableParent();
    }

    @Override
    public boolean isEnabled() {
        return hasExpandableParent();
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
