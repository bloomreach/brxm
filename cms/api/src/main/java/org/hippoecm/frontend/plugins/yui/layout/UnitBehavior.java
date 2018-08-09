/*
 *  Copyright 2008-2018 Hippo B.V. (http://www.onehippo.com)
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

import java.util.Map;

import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.wicket.Component;
import org.apache.wicket.Page;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.AjaxRequestTarget.AbstractListener;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.request.cycle.RequestCycle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This behavior stores the component id as it's markupId in the {@link UnitSettings} and if the request rendering this
 * behavior is an Ajax-request, it will force the whole wireframe to re-render (since this unit might be added to an
 * already existing wireframe).  
 */
public class UnitBehavior extends Behavior {

    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(UnitBehavior.class);

    private String position;

    public UnitBehavior(String position) {
        this.position = position;
    }

    @Override
    public void bind(final Component component) {
        super.bind(component);

        // re-render complete wireframe during the render phase
        AjaxRequestTarget target = RequestCycle.get().find(AjaxRequestTarget.class);
        if (target != null) {
            target.addListener(new AbstractListener() {
                public void onBeforeRespond(Map map, AjaxRequestTarget target) {
                    if (target.getPage() != component.findParent(Page.class)) {
                        return;
                    }
                    IWireframe wireframe = WireframeUtils.getParentWireframe(component);
                    if (wireframe != null) {
                        wireframe.render(target);
                    } else {
                        log.warn("Unable to find parent wireframe-behavior");
                    }
                }

                public void onAfterRespond(Map map, AjaxRequestTarget.IJavaScriptResponse response) {
                }
            });
        }
    }

    public String getPosition() {
        return position;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof UnitBehavior;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(11, 97).toHashCode();
    }

}
