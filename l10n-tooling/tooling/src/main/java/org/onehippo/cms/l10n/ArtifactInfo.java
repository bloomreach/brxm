/*
 *  Copyright 2016-2020 Hippo B.V. (http://www.onehippo.com)
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

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collection;
import java.util.HashSet;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.apache.commons.lang.StringUtils.substringAfter;
import static org.apache.commons.lang.StringUtils.substringBefore;

class ArtifactInfo {

    private final URL manifestURL;
    private final Collection<String> entries = new HashSet<>(1000);
    private final Properties pomProperties = new Properties();

    ArtifactInfo(final URL manifestURL) throws IOException {
        this.manifestURL = manifestURL;
        try (final ZipInputStream in = new ZipInputStream(getJarURL().openStream())) {
            ZipEntry entry;
            while ((entry = in.getNextEntry()) != null) {
                final String entryName = entry.getName();
                if (entryName.startsWith("META-INF")) {
                    if (entryName.endsWith("pom.properties")) {
                        pomProperties.load(in);
                    }
                } else if (!entry.isDirectory()) {
                    entries.add(entryName);
                }
            }
        }
    }

    /* constructor for testing purposes */
    ArtifactInfo(String artifactId) {
        pomProperties.setProperty("artifactId", artifactId);
        manifestURL = null;
    }

    boolean isBloomreachArtifact() {
        return isBloomreachArtifactGroupId(getGroupId());
    }

    String getGroupId() {
        return pomProperties.getProperty("groupId");
    }

    String getArtifactId() {
        return pomProperties.getProperty("artifactId");
    }

    String getVersion() {
        return pomProperties.getProperty("version");
    }

    String getCoordinates() {
        return String.format("%s:%s:%s", getGroupId(), getArtifactId(), getVersion());
    }

    Collection<String> getEntries() {
        return entries;
    }

    File getJarFile() throws MalformedURLException, URISyntaxException {
        return new File(getJarURL().toURI());
    }

    public static boolean isBloomreachArtifactGroupId(final String groupId) {
        return groupId != null && (groupId.startsWith("org.onehippo.cms") || groupId.startsWith("com.onehippo.cms") || groupId.startsWith("com.bloomreach.xm"));
    }

    private URL getJarURL() throws MalformedURLException {
        return new URL(substringAfter(substringBefore(manifestURL.toString(), "!/META-INF/MANIFEST.MF"), "jar:"));
    }
}
