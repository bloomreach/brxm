/*
 *  Copyright 2014 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.skin;

import java.io.IOException;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.apache.wicket.util.resource.ResourceStreamNotFoundException;
import org.hippoecm.frontend.plugins.standards.image.InlineSvg;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IconResourceReference extends PackageResourceReference {
    public static final Logger log = LoggerFactory.getLogger(IconResourceReference.class);
    
    private Icon icon;
    
    public IconResourceReference(final Class<?> scope, final String name) {
        super(scope, name);
    }

    public IconResourceReference(final Class<?> scope, final String name, Icon icon) {
        super(scope, name);
        this.icon = icon;
    }

    /**
     * Writes this icon to the response. If it is not an icon from {@link Icon} it will be rendered inline as
     * <svg><g>...</g></svg>, otherwise it will be rendered as <svg><use xlink:href="#iconID"/></svg> and a couple
     * of helper/marker classes will be added. For example, the icon Icon.CARET_DOWN_MEDIUM will be rendered as
     * <svg class="hi hi-medium hi-caret hi-caret-down"><use xlink:href="#hi-caret-down-medium"/></svg>
     */
    public void writeToResponse() {
        if (icon == null) {
            try {
                final String svg = InlineSvg.svgAsString(this);
                RequestCycle.get().getResponse().write(svg);
            } catch (ResourceStreamNotFoundException | IOException e) {
                log.error("Failed to load svg image[{}]", getKey(), e);
            }
        } else {
            //caret_down_medium
            StringBuilder css = new StringBuilder("hi");

            //"hi hi-medium hi-caret hi-caret-down"
            String[] nameParts = StringUtils.split(icon.name().toLowerCase(), '_');
            //hi-medium
            css.append(" hi-").append(nameParts[nameParts.length - 1]);
            css.append(" hi-").append(nameParts[0]);
            if (nameParts.length == 3) {
                css.append(" hi-").append(nameParts[0]).append("-").append(nameParts[1]);
            }

            String id = "hi-" + StringUtils.replace(icon.name().toLowerCase(), "_", "-");
            RequestCycle.get().getResponse().write("<svg class=\"" + css.toString() + "\"><use xlink:href=\"#" + id + "\" /></svg>");
        }
    }

    public void setIcon(final Icon icon) {
        this.icon = icon;
    }
}
