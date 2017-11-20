/*
 * Copyright 2017 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.cms7.essentials.dashboard.utils;

import com.google.common.base.Strings;
import org.apache.maven.archetype.common.DefaultPomManager;
import org.apache.maven.model.BuildBase;
import org.apache.maven.model.Model;
import org.apache.maven.model.ModelBase;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.Profile;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.Xpp3DomUtils;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MavenModelUtils {
    private MavenModelUtils() {
        throw new IllegalStateException("Utility class");
    }

    private static final String DEFAULT_PROFILE_ID = "default";
    private static Logger log = LoggerFactory.getLogger(MavenModelUtils.class);

    public static Model readPom(File pomFile) {
        try {
            return new DefaultPomManager().readPom(pomFile);
        } catch (XmlPullParserException | IOException e) {
            log.error("Error parsing pom", e);
        }
        return null;
    }

    public static Model readPom(InputStream pomStream) {
        try {
            return new DefaultPomManager().readPom(pomStream);
        } catch (XmlPullParserException | IOException e) {
            log.error("Error parsing pom resource", e);
        }
        return null;
    }

    public static boolean writePom(final Model model, final File pomFile) {
        try {
            new DefaultPomManager().writePom(model, pomFile, pomFile);

            // fix profile names (intellij expects default profile id)
            // see: http://youtrack.jetbrains.com/issue/IDEA-126568
            final List<Profile> profiles = model.getProfiles();
            boolean needsRewrite = false;
            for (Profile profile : profiles) {
                if (Strings.isNullOrEmpty(profile.getId()) || profile.getId().equals("default")) {
                    profile.setId("{{ESSENTIALS_DEFAULT_PLACEHOLDER}}");
                    needsRewrite = true;
                }
            }
            if (needsRewrite) {
                // replace default id:
                final String pomContent = GlobalUtils.readStreamAsText(new FileInputStream(pomFile));
                final Map<String, String> data = new HashMap<>();
                data.put("ESSENTIALS_DEFAULT_PLACEHOLDER", DEFAULT_PROFILE_ID);
                final String newContent = TemplateUtils.replaceStringPlaceholders(pomContent, data);
                GlobalUtils.writeToFile(newContent, pomFile.toPath());
                log.debug("Fixed default profile id");
            }
            log.debug("Written pom to: {}", pomFile.getAbsolutePath());
        } catch (IOException e) {
            log.error("Error adding maven dependency", e);
            return false;
        }
        return true;
    }

    public static void mergeProperties(ModelBase model, ModelBase temporaryModel) {
        // ModelBase can be a Model or a Profile...

        // Copy all properties. Existing properties will be overwritten
        model.getProperties().putAll( temporaryModel.getProperties() );
    }

    public static void mergeBuildPlugins(BuildBase modelBuild, BuildBase generatedModelBuild )
    {
        @SuppressWarnings( "unchecked" )
        Map<String, Plugin> pluginsByIds = modelBuild.getPluginsAsMap();
        @SuppressWarnings( "unchecked" )
        List<Plugin> generatedPlugins = generatedModelBuild.getPlugins();

        for ( Plugin generatedPlugin : generatedPlugins )
        {
            String generatedPluginsId = generatedPlugin.getKey();

            if ( !pluginsByIds.containsKey( generatedPluginsId ) )
            {
                modelBuild.addPlugin( generatedPlugin );
            }
            else
            {
                log.info( "Try to merge plugin configuration of plugins with id: {}", generatedPluginsId );
                Plugin modelPlugin = pluginsByIds.get( generatedPluginsId );

                modelPlugin.setConfiguration( Xpp3DomUtils.mergeXpp3Dom( (Xpp3Dom) generatedPlugin.getConfiguration(),
                        (Xpp3Dom) modelPlugin.getConfiguration() ) );
            }
        }
    }

}
