/*
 *  Copyright 2010 Hippo.
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

import org.apache.wicket.ResourceReference;
import org.apache.wicket.Session;
import org.apache.wicket.behavior.HeaderContributor;
import org.apache.wicket.markup.html.CSSPackageResource;
import org.apache.wicket.markup.html.PackageResource;
import org.hippoecm.frontend.service.IconSize;

public final class BrowserStyle {

    private static final Map<String, Boolean> customPackageResourceExists = new ConcurrentHashMap<String, Boolean>();

    private BrowserStyle() {
    }

    public static HeaderContributor getStyleSheet() {
        return CSSPackageResource.getHeaderContribution(BrowserStyle.class, "res/style.css");
    }

    public static ResourceReference getIcon(String customName, String defaultName, IconSize size) {
        Session session = Session.get();
        String customResourceKey = "res/" + customName + "-" + size.getSize() + ".png"+session.getLocale()+session.getStyle();
        if (!customPackageResourceExists.containsKey(customResourceKey)) {
            Boolean resourceExists = PackageResource.exists(BrowserStyle.class, "res/" + customName + "-" + size.getSize() + ".png", session
                .getLocale(),session.getStyle());
            customPackageResourceExists.put(customResourceKey, resourceExists);
        }
        if (customPackageResourceExists.get(customResourceKey)) {
            return getIcon(customName, size);
        } else {
            return getIcon(defaultName, size);
        }
    }

    public static ResourceReference getIcon(String name, IconSize size) {
        Session session = Session.get();
        return new ResourceReference(BrowserStyle.class, "res/" + name + "-" + size.getSize() + ".png", session
                .getLocale(), session.getStyle());
    }

}
