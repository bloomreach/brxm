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
import java.util.Collection;
import java.util.HashSet;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

import static org.apache.maven.plugins.annotations.ResolutionScope.COMPILE;

@Mojo(name = "extract", defaultPhase = LifecyclePhase.VALIDATE, requiresDependencyResolution = COMPILE)
public class ExtractMojo extends AbstractL10nMojo {
    
    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        final Collection<String> locales = new HashSet<>(getLocales());
        locales.add("en");
        try {
            new Extractor(getRegistryDir(), getModuleName(), locales, getResourcesClassLoader(), getExcludes()).extract();
        } catch (IOException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }
    }

    private File getRegistryDir() throws IOException {
        return new File(getBaseDir(), "resources");
    }

}
