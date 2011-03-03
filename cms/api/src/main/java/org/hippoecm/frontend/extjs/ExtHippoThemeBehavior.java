/*
 *  Copyright 2011 Hippo.
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
package org.hippoecm.frontend.extjs;

import org.apache.wicket.Component;
import org.apache.wicket.markup.html.CSSPackageResource;
import org.wicketstuff.js.ext.util.ExtCustomThemeBehavior;

/**
 * Adds the Hippo ExtJs theme CSS to the header.
 */
public class ExtHippoThemeBehavior extends ExtCustomThemeBehavior {

    @Override
    protected void addCustomTheme(final Component component) {
        component.add(CSSPackageResource.getHeaderContribution(getClass(), "hippotheme/ExtHippoTheme.css"));
    }

}
