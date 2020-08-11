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

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import static org.apache.maven.plugins.annotations.ResolutionScope.RUNTIME;

@Mojo(name = "export", defaultPhase = LifecyclePhase.VALIDATE, requiresDependencyResolution = RUNTIME)
public class ExportMojo extends AbstractL10nMojo {

    @Parameter(defaultValue = "false")
    private String full;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        final ExportFileWriter writer;

        switch (getFileFormat()) {
            case CSV:
                writer = new CsvExportFileWriter(getCSVFormat());
                break;
            case Excel:
                writer = new ExcelExportFileWriter();
                break;
            default:
                throw new MojoExecutionException("Unrecognized format");
        }

        try {
            new Exporter(getBaseDir(), writer).export(getLocale(), Boolean.valueOf(full));
        } catch (IOException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }
    }

}
