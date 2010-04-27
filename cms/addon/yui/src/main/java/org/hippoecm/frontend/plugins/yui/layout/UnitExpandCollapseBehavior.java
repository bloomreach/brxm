/*
 * Copyright 2010 Hippo.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.hippoecm.frontend.plugins.yui.layout;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AbstractBehavior;

public class UnitExpandCollapseBehavior extends AbstractBehavior {

    //Component c;
    WireframeSettings settings;
    String defaultToggleUnit;

    public UnitExpandCollapseBehavior(WireframeSettings settings) {
        this(settings, "center");
    }

    public UnitExpandCollapseBehavior(WireframeSettings settings, String defaultToggleUnit) {
        this.settings = settings;
        this.defaultToggleUnit = defaultToggleUnit;
    }


//    @Override
//    public void bind(Component component) {
//        super.bind(c = component);
//    }
//
//    protected Component getComponent() {
//        return c;
//    }

    public void toggle(String position, AjaxRequestTarget target) {
        UnitSettings unitSettings = settings.getUnit(position);
        if(unitSettings != null) {
            if(unitSettings.isExpanded()) {
                target.appendJavascript("YAHOO.hippo.LayoutManager.collapseUnit('" + settings.getRootId().getElementId() + "', '" + position + "');");
            } else {
                target.appendJavascript("YAHOO.hippo.LayoutManager.expandUnit('" + settings.getRootId().getElementId() + "', '" + position + "');");
            }
            unitSettings.setExpanded(!unitSettings.isExpanded());
        }
    }

    public void toggle(AjaxRequestTarget target) {
        toggle(defaultToggleUnit, target);
    }

}