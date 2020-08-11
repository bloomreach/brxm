/*
 * Copyright 2017-2018 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onehippo.cms7.essentials.plugin.sdk.utils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;
import java.util.Map;

import org.apache.maven.archetype.common.DefaultPomManager;
import org.apache.maven.model.BuildBase;
import org.apache.maven.model.Model;
import org.apache.maven.model.ModelBase;
import org.apache.maven.model.Plugin;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.Xpp3DomUtils;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MavenModelUtils {
    private MavenModelUtils() {
        throw new IllegalStateException("Utility class");
    }

    private static Logger log = LoggerFactory.getLogger(MavenModelUtils.class);

    public static Model readPom(File pomFile) {
        try {
            return new DefaultPomManager().readPom(pomFile);
        } catch (XmlPullParserException | IOException e) {
            log.error("Error parsing pom '{}'.", pomFile, e);
        }
        return null;
    }

    public static Model readPom(URL pomUrl) {
        try (InputStream pomStream = pomUrl.openStream()) {
            return new DefaultPomManager().readPom(pomStream);
        } catch (XmlPullParserException | IOException e) {
            log.error("Error parsing pom resource", e);
        }
        return null;
    }

    public static boolean writePom(final Model model, final File pomFile) {
        try {
            new DefaultPomManager().writePom(model, pomFile, pomFile);
            return true;
        } catch (IOException e) {
            log.error("Error adding maven dependency", e);
        }
        return false;
    }

    public static void mergeProperties(ModelBase model, ModelBase incomingModel) {
        // Copy all properties. Existing properties will be overwritten
        model.getProperties().putAll(incomingModel.getProperties());
    }

    public static void mergeBuildPlugins(BuildBase build, BuildBase incomingBuild) {
        final Map<String, Plugin> pluginsById = build.getPluginsAsMap();
        final List<Plugin> incomingPlugins = incomingBuild.getPlugins();

        for (Plugin incomingPlugin : incomingPlugins) {
            String incomingPluginId = incomingPlugin.getKey();

            if (!pluginsById.containsKey(incomingPluginId)) {
                build.addPlugin(incomingPlugin);
            } else {
                log.info("Try to merge plugin configuration of plugins with id: {}", incomingPluginId);
                Plugin plugin = pluginsById.get(incomingPluginId);

                plugin.setConfiguration(Xpp3DomUtils.mergeXpp3Dom((Xpp3Dom) incomingPlugin.getConfiguration(),
                        (Xpp3Dom) plugin.getConfiguration()));
            }
        }
    }

}
