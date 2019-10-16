/*
 *  Copyright 2016-2016 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms.l10n;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;

public abstract class ResourceBundleLoader {
    
    final Collection<String> locales;
    final ClassLoader classLoader;

    ResourceBundleLoader(final Collection<String> locales, final ClassLoader classLoader) {
        this.locales = locales;
        this.classLoader = classLoader;
    }

    public final Collection<ResourceBundle> loadBundles() throws IOException {
        Collection<ResourceBundle> bundles = new ArrayList<>();
        for (ArtifactInfo artifactInfo : getHippoArtifactsOnClasspath()) {
            collectResourceBundles(artifactInfo, bundles);
        }
        return bundles;
    }

    protected abstract void collectResourceBundles(final ArtifactInfo artifactInfo, final Collection<ResourceBundle> bundles) throws IOException;

    public static Collection<ResourceBundleLoader> getResourceBundleLoaders(final Collection<String> locales, final ClassLoader classLoader, final String[] excludes) {
        return Arrays.asList(new AngularResourceBundleLoader(locales, classLoader),
                new WicketResourceBundleLoader(locales, classLoader, excludes), new RepositoryResourceBundleLoader(locales, classLoader));
    }

    private List<ArtifactInfo> getHippoArtifactsOnClasspath() throws IOException {
        final List<ArtifactInfo> hippoJars = new LinkedList<>();
        final Enumeration<URL> resources = classLoader.getResources("META-INF/MANIFEST.MF");
        while (resources.hasMoreElements()) {
            final URL manifestURL = resources.nextElement();
            if (isJarURL(manifestURL)) {
                final ArtifactInfo artifactInfo = new ArtifactInfo(manifestURL);
                if (artifactInfo.isBloomreachArtifact()) {
                    hippoJars.add(artifactInfo);
                }
            }
        }
        return hippoJars;
    }

    private static boolean isJarURL(final URL manifestURL) {
        return manifestURL.toString().endsWith(".jar!/META-INF/MANIFEST.MF");
    }

}
