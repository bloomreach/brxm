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
package org.onehippo.cms.translations;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.commons.cli.Option.UNLIMITED_VALUES;
import static org.onehippo.cms.translations.ResourceBundleLoader.getResourceBundleLoaders;
import static org.onehippo.cms.translations.TranslationsUtils.mapSourceBundleFileToTargetBundleFile;

public class Extractor {
    
    private static final Logger log = LoggerFactory.getLogger(Extractor.class);

    private final File registryDir;
    private final Collection<String> locales;
    private final String moduleName;
    
    public Extractor(final File registryDir, final String moduleName, final Collection<String> locales) {
        this.registryDir = registryDir;
        this.locales = locales;
        this.moduleName = moduleName;
    }
    
    public void extract() throws IOException {
        for (ResourceBundleLoader loader : getResourceBundleLoaders(locales)) {
            for (ResourceBundle sourceBundle : loader.loadBundles()) {
                final String bundleFileName = mapSourceBundleFileToTargetBundleFile(
                        sourceBundle.getFileName(), sourceBundle.getType(), sourceBundle.getLocale());
                final ResourceBundle targetBundle = ResourceBundle.createInstance(
                        sourceBundle.getName(), bundleFileName, new File(registryDir, bundleFileName), sourceBundle.getType());
                targetBundle.setModuleName(moduleName);
                for (Map.Entry<String, String> entry : sourceBundle.getEntries().entrySet()) {
                    targetBundle.getEntries().put(entry.getKey(), entry.getValue());
                }
                if (!targetBundle.isEmpty()) {
                    targetBundle.save();
                } else {
                    log.warn("Not saving empty bundle: {}", targetBundle.getId());
                }
            }
        }
    }
    
    public static void main(String[] args) throws Exception {
        
        final Options options = new Options();
        final Option basedirOption = new Option("d", "basedir", true, "the project base directory");
        basedirOption.setRequired(true);
        options.addOption(basedirOption);
        final Option localesOption = new Option("l", "locales", true, "comma-separated list of locales to extract");
        localesOption.setRequired(true);
        localesOption.setValueSeparator(',');
        localesOption.setArgs(UNLIMITED_VALUES);
        options.addOption(localesOption);
        
        final CommandLineParser parser = new DefaultParser();
        final CommandLine commandLine = parser.parse(options, args);
        final File baseDir = new File(commandLine.getOptionValue("d")).getCanonicalFile();
        final Collection<String> locales = Arrays.asList(commandLine.getOptionValues("l"));
        TranslationsUtils.checkLocales(locales);
        final String moduleName = baseDir.getName();
        final File registryDir = new File(baseDir, "resources");
        if (!registryDir.exists()) {
            registryDir.mkdirs();
        }
        
        new Extractor(registryDir, moduleName, locales).extract();
    }
    
}
