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
package org.hippoecm.frontend.plugins.standards.icon;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.wicket.Session;
import org.apache.wicket.request.resource.CssResourceReference;
import org.apache.wicket.request.resource.PackageResource;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.apache.wicket.request.resource.ResourceReference;
import org.hippoecm.frontend.service.IconSize;
import org.hippoecm.frontend.skin.Icon;

public final class BrowserStyle {

    private static final Map<String, Boolean> customPackageResourceExists = new ConcurrentHashMap<String, Boolean>();

    private BrowserStyle() {
    }

    public static ResourceReference getStyleSheet() {
        return new CssResourceReference(BrowserStyle.class, "res/style.css");
    }

    @SuppressWarnings("deprecation")
    public static ResourceReference getIcon(String customName, String defaultName, IconSize size) {
        Session session = Session.get();
        String customResourceKey = "res/" + customName + "-" + size.getSize() + ".png" + session.getLocale() + session.getStyle();
        if (!customPackageResourceExists.containsKey(customResourceKey)) {
            Boolean resourceExists = PackageResource.exists(BrowserStyle.class, "res/" + customName + "-" + size.getSize() + ".png", session
                    .getLocale(), session.getStyle(), null);
            customPackageResourceExists.put(customResourceKey, resourceExists);
        }
        if (customPackageResourceExists.get(customResourceKey)) {
            return getIcon(customName, size);
        } else {
            return getIcon(defaultName, size);
        }
    }

    /**
     * @param name name part of the icon file
     * @param size size of the icon
     * @return reference to the icon
     * @deprecated use an {@link Icon} value instead.
     */
    @Deprecated
    public static ResourceReference getIcon(String name, IconSize size) {
        ResourceReference reference = Icon.referenceByName(name, size, null);
        if (reference == null) {
            Session session = Session.get();
            reference = new PackageResourceReference(BrowserStyle.class, "res/" + name + "-" + size.getSize() + ".png",
                    session.getLocale(), session.getStyle(), null);
        }
        return reference;
    }

}
