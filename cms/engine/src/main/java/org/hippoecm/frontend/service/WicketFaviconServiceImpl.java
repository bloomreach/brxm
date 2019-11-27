/*
 * Copyright 2019 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *      http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.hippoecm.frontend.service;

import java.util.Objects;
import java.util.function.Supplier;

import org.apache.wicket.request.Url;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.resource.PackageResource;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.apache.wicket.request.resource.ResourceReference;
import org.apache.wicket.request.resource.UrlResourceReference;
import org.hippoecm.frontend.PluginApplication;

public class WicketFaviconServiceImpl implements FaviconService, WicketFaviconService {

    private static final String SUFFIX = "-icon.png";
    private static final ResourceReference DEFAULT_FAVICON = new UrlResourceReference(
            Url.parse("skin/images/cms" + SUFFIX));

    private final Supplier<String> pluginApplicationNameSupplier;

    public WicketFaviconServiceImpl(Supplier<String> pluginApplicationNameSupplier) {
        Objects.requireNonNull(pluginApplicationNameSupplier);
        this.pluginApplicationNameSupplier = pluginApplicationNameSupplier;
    }

    public WicketFaviconServiceImpl() {
        this(() -> PluginApplication.get().getPluginApplicationName());
    }

    @Override
    public String getRelativeFaviconUrl() {
        return RequestCycle.get().urlFor(getFaviconResourceReference(), null).toString();
    }

    @Override
    public ResourceReference getFaviconResourceReference() {
        final Class<WicketFaviconServiceImpl> scope = WicketFaviconServiceImpl.class;
        final String name = getFaviconFileName();
        return PackageResource.exists(scope, name, null, null, null) ?
                new PackageResourceReference(scope, name) : DEFAULT_FAVICON;
    }

    private String getFaviconFileName() {
        return pluginApplicationNameSupplier.get() + SUFFIX;
    }
}
