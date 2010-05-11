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

    WireframeSettings settings;
    String defaultToggleUnit;

    public UnitExpandCollapseBehavior(WireframeSettings settings) {
        this(settings, "center");
    }

    public UnitExpandCollapseBehavior(WireframeSettings settings, String defaultToggleUnit) {
        this.settings = settings;
        this.defaultToggleUnit = defaultToggleUnit;
    }

    public void toggle(String position, AjaxRequestTarget target) {
        UnitSettings unitSettings = settings.getUnit(position);
        if(unitSettings != null) {
            boolean expand = !unitSettings.isExpanded();
            String jsMethod = expand ? "YAHOO.hippo.LayoutManager.expandUnit" : "YAHOO.hippo.LayoutManager.collapseUnit";
            target.appendJavascript(jsMethod + "('" + settings.getRootId().getElementId() + "', '" + position + "');");
            unitSettings.setExpanded(expand);
            onToggle(expand, target);
        }
    }

    public void toggle(AjaxRequestTarget target) {
        toggle(defaultToggleUnit, target);
    }

    protected void onToggle(boolean expand, AjaxRequestTarget target) {
    }

}