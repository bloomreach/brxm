/*
 *  Copyright 2010-2018 Hippo B.V. (http://www.onehippo.com)
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
import org.apache.wicket.request.resource.PackageResource;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.apache.wicket.request.resource.ResourceReference;
import org.hippoecm.frontend.service.IconSize;

public final class BrowserStyle {

    private static final Map<String, Boolean> customPackageResourceExists = new ConcurrentHashMap<>();

    private BrowserStyle() {
    }

    public static ResourceReference getIconOrNull(String name, IconSize size) {
        final Session session = Session.get();
        final String customPngPath = getPackageResourcePath(name, size, ".png");
        if (customResourceExists(customPngPath, session)) {
            return createResourceReference(customPngPath, session);
        }
        final String customSvgPath = getPackageResourcePath(name, size, ".svg");
        if (customResourceExists(customSvgPath, session)) {
            return createResourceReference(customSvgPath, session);
        }
        return null;
    }

    private static boolean customResourceExists(final String packageResourcePath, final Session session) {
        final String customResourceKey = packageResourcePath + session.getLocale() + session.getStyle();
        if (!customPackageResourceExists.containsKey(customResourceKey)) {
            Boolean resourceExists = PackageResource.exists(BrowserStyle.class, packageResourcePath,
                    session.getLocale(), session.getStyle(), null);
            customPackageResourceExists.put(customResourceKey, resourceExists);
            return resourceExists;
        } else {
            return customPackageResourceExists.get(customResourceKey);
        }
    }

    private static ResourceReference createResourceReference(final String path, final Session session) {
        return new PackageResourceReference(BrowserStyle.class, path, session.getLocale(), session.getStyle(), null);
    }

    private static String getPackageResourcePath(final String name, final IconSize size, final String extension) {
        return "res/" + name + "-" + size.getSize() + extension;
    }
}
