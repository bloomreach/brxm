/*
 *  Copyright 2017 Hippo B.V. (http://www.onehippo.com)
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

import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashSet;

import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.maven.plugins.annotations.ResolutionScope.COMPILE;

/**
 * Generate localized hcm-module.yaml
 */
@Mojo(name = "generate-yaml", defaultPhase = LifecyclePhase.PREPARE_PACKAGE, requiresDependencyResolution = COMPILE)
public class GenerateYamlMojo extends AbstractL10nMojo {

    static final Logger log = LoggerFactory.getLogger(GenerateYamlMojo.class);

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {

        final Collection<String> locales = new HashSet<>(getLocales());
        try {

            final Path modulePath = getBaseDir().toPath();
            final String outputDirectory = project.getBuild().getOutputDirectory();
            final Path srcTplModuleDescriptorPath = modulePath.resolve("resources").resolve("hcm-module.yaml");
            final Path srcTplAddLocalePath = modulePath.resolve("resources").resolve("add-locale.yaml");

            if (!Files.exists(srcTplModuleDescriptorPath)) {
                log.info(ANSI_YELLOW + "hcm-module.yaml file doesnt exist, skipping... " + srcTplModuleDescriptorPath.getParent()  + ANSI_RESET);
                return;
            }

            String addLocalesTpl = null;
            if (Files.exists(srcTplAddLocalePath)) {
                log.info(ANSI_YELLOW + "Reading " + srcTplAddLocalePath + ANSI_RESET);
                addLocalesTpl = FileUtils.readFileToString(srcTplAddLocalePath.toFile(), Charset.forName("UTF-8"));
            }

            log.info(ANSI_YELLOW + "Reading " + srcTplModuleDescriptorPath + ANSI_RESET);
            final String moduleHcmTpl = FileUtils.readFileToString(srcTplModuleDescriptorPath.toFile(), Charset.forName("UTF-8"));

            for (final String locale : locales) {
                final Path hcmConfigFolder = Paths.get(outputDirectory, locale, "hcm-config");
                if (Files.exists(hcmConfigFolder)) {
                    final Path targetFile = Paths.get(outputDirectory, locale, "hcm-module.yaml");
                    log.info(ANSI_YELLOW + "Copying hcm-module.yaml from " + srcTplModuleDescriptorPath.getParent() + " to " + targetFile.getParent() + ANSI_RESET);
                    final String moduleHcm = moduleHcmTpl.replace("${locale}", locale);
                    Files.write(targetFile, moduleHcm.getBytes());

                    if (addLocalesTpl != null) {
                        final Path localeTargetFile = hcmConfigFolder.resolve("add-locale.yaml");
                        log.info(ANSI_YELLOW + "Copying add-locale.yaml from " + srcTplAddLocalePath.getParent() + " to " + localeTargetFile.getParent() + ANSI_RESET);
                        String localeHcm = addLocalesTpl.replace("${locale}", locale);
                        Files.write(localeTargetFile, localeHcm.getBytes());
                    }

                } else {
                    log.info(ANSI_RED + "No hcm-config folder was found, skipping..." + srcTplModuleDescriptorPath.getParent() + ANSI_RESET);
                }
            }

        } catch (Exception e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }
    }

    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_BLACK = "\u001B[30m";
    public static final String ANSI_RED = "\u001B[31m";
    public static final String ANSI_GREEN = "\u001B[32m";
    public static final String ANSI_YELLOW = "\u001B[33m";
    public static final String ANSI_BLUE = "\u001B[34m";
    public static final String ANSI_PURPLE = "\u001B[35m";
    public static final String ANSI_CYAN = "\u001B[36m";
    public static final String ANSI_WHITE = "\u001B[37m";
}
