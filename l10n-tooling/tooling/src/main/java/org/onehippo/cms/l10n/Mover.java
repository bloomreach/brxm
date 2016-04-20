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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.commons.cli.Option.UNLIMITED_VALUES;
import static org.apache.commons.lang.StringUtils.isEmpty;
import static org.apache.commons.lang.StringUtils.substringBefore;
import static org.onehippo.cms.l10n.TranslationsUtils.mapRegistryFileToResourceBundleFile;
import static org.onehippo.cms.l10n.TranslationsUtils.mapResourceBundleToRegistryInfoFile;

public class Mover {

    private static final Logger log = LoggerFactory.getLogger(Mover.class);

    private final Collection<String> locales;
    private final String moduleName;
    private final Registry registry;

    Mover(final File baseDir, final String moduleName, final Collection<String> locales) throws IOException {
        this.locales = locales;
        this.moduleName = moduleName;
        registry = new Registry(new File(baseDir, "resources"));
    }

    void moveBundle(final String srcPath, final String destPath) throws IOException {
        final String srcInfoFileName = mapResourceBundleToRegistryInfoFile(srcPath);
        final String destInfoFileName = mapResourceBundleToRegistryInfoFile(destPath);
        final RegistryInfo srcRegistryInfo = registry.getRegistryInfo(srcInfoFileName);
        final RegistryInfo destRegistryInfo = registry.getRegistryInfo(destInfoFileName);
        if (!srcRegistryInfo.exists()) {
            throw new IOException("Registry info file " + srcInfoFileName + " does not exist for bundle " + srcPath);
        }
        if (destRegistryInfo.exists()) {
            throw new IOException("Destination info file " + destInfoFileName + " already exists for bundle " + destPath);
        }
        final BundleType bundleType = srcRegistryInfo.getBundleType();
        final Collection<String> locales = new HashSet<>(this.locales);
        locales.add("en");
        final Map<ResourceBundle, File> bundles = new HashMap<>();
        for (final String locale : locales) {
            final String srcBundleFileName = mapRegistryFileToResourceBundleFile(srcInfoFileName, bundleType, locale);
            final Iterator<ResourceBundle> srcBundles = registry.getAllResourceBundles(locale, srcRegistryInfo).iterator();
            if (!srcBundles.hasNext()) {
                log.info("No bundle file: {}", srcBundleFileName);
            } else {
                final String destBundleFileName = mapRegistryFileToResourceBundleFile(destInfoFileName, bundleType, locale);
                final File destBundleFile = registry.getRegistryFile(destBundleFileName);
                if (destBundleFile.exists()) {
                    throw new IOException("Destination bundle file already exists: " + destBundleFileName);
                }
                bundles.put(srcBundles.next(), destBundleFile);
            }
        }
        
        // move registry info
        destRegistryInfo.setBundleType(srcRegistryInfo.getBundleType());
        for (String key : srcRegistryInfo.getKeys()) {
            destRegistryInfo.putKeyData(key, srcRegistryInfo.getKeyData(key));
        }
        destRegistryInfo.save();
        srcRegistryInfo.delete();
        
        // move bundles
        for (Map.Entry<ResourceBundle, File> entry : bundles.entrySet()) {
            final ResourceBundle srcBundle = entry.getKey();
            srcBundle.setModuleName(moduleName);
            srcBundle.move(entry.getValue());
        }
    }

    void moveKey(final String srcPath, String srcKey, String destKey) throws IOException {
        final String srcInfoFileName = mapResourceBundleToRegistryInfoFile(srcPath);
        final RegistryInfo registryInfo = registry.getRegistryInfo(srcInfoFileName);
        if (!registryInfo.exists()) {
            throw new IOException("Registry info file " + srcInfoFileName + " does not exist for bundle " + srcPath);
        }
        final String srcRegistryKey = srcKey;
        final String destRegistryKey = destKey;
        final String bundleName = substringBefore(srcKey, "/");
        srcKey = srcKey.indexOf('/') == -1 ? srcKey : srcKey.substring(srcKey.indexOf('/')+1);
        destKey = destKey.indexOf('/') == -1 ? destKey : destKey.substring(destKey.indexOf('/')+1);
        final KeyData keyData = registryInfo.getKeyData(srcRegistryKey);
        if (keyData == null) {
            throw new IOException("Key not found: " + srcRegistryKey);
        }
        final Collection<String> locales = new HashSet<>(this.locales);
        locales.add("en");
        final Collection<ResourceBundle> bundles = new ArrayList<>();
        for (final String locale : locales) {
            final ResourceBundle resourceBundle = registry.getResourceBundle(bundleName, locale, registryInfo);
            if (!resourceBundle.exists()) {
                log.info("Bundle not found: {}", resourceBundle.getId());
            } else {
                bundles.add(resourceBundle);
            }
        }
        
        // rename key in registry
        registryInfo.putKeyData(destRegistryKey, keyData);
        registryInfo.removeKeyData(srcRegistryKey);
        registryInfo.save();
        
        // rename key in bundles
        for (final ResourceBundle bundle : bundles) {
            final Map<String, String> entries = bundle.getEntries();
            final String value = entries.get(srcKey);
            if (value != null) {
                entries.remove(srcKey);
                entries.put(destKey, value);
                bundle.save();
            }
        }
    }

    public static void main(String[] args) throws Exception {

        final Options options = new Options();
        final Option basedirOption = new Option("basedir", "basedir", true, "the project base directory");
        basedirOption.setRequired(true);
        options.addOption(basedirOption);
        final Option localesOption = new Option("locales", "locales", true, "comma-separated list of locales to extract");
        localesOption.setRequired(true);
        localesOption.setValueSeparator(',');
        localesOption.setArgs(UNLIMITED_VALUES);
        options.addOption(localesOption);
        final Option commandOption = new Option("command", "command", true, "command to execute: one of initialize, update, or report");
        commandOption.setRequired(true);
        options.addOption(commandOption);
        final Option srcPathOption = new Option("srcPath", "srcPath", true, "the path to the bundle to move");
        srcPathOption.setRequired(true);
        options.addOption(srcPathOption);
        final Option destPathOption = new Option("destPath", "destPath", true, "the destination path");
        options.addOption(destPathOption);
        final Option srcKeyOption = new Option("srcKey", "srcKey", true, "the name of the key to move");
        options.addOption(srcKeyOption);
        final Option destKeyOption = new Option("destKey", "destKey", true, "the new key name");
        options.addOption(destKeyOption);
        
        final CommandLineParser parser = new DefaultParser();
        final CommandLine commandLine = parser.parse(options, args);
        final File baseDir = new File(commandLine.getOptionValue("basedir")).getCanonicalFile();
        final String moduleName = baseDir.getName();
        final Collection<String> locales = Arrays.asList(commandLine.getOptionValues("locales"));
        TranslationsUtils.checkLocales(locales);
        final String command = commandLine.getOptionValue("command");
        final String srcPath = commandLine.getOptionValue("srcPath");
        final String destPath = commandLine.getOptionValue("destPath");
        final String srcKey = commandLine.getOptionValue("srcKey");
        final String destKey = commandLine.getOptionValue("destKey");
        
        final Mover mover = new Mover(baseDir, moduleName, locales);

        switch (command) {
            case "moveBundle":
                if (isEmpty(destPath)) {
                    throw new IllegalArgumentException("Missing option destPath");
                }
                mover.moveBundle(srcPath, destPath);
                break;
            case "moveKey": {
                if (isEmpty(srcKey)) {
                    throw new IllegalArgumentException("Missing option srcKey");
                }
                if (isEmpty(destKey)) {
                    throw new IllegalArgumentException("Missing option destKey");
                }
                mover.moveKey(srcPath, srcKey, destKey);
                break;
            }
            default:
                throw new IllegalArgumentException("Unrecognized command: " + command);
        }

    }
}