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
        try {
            new Exporter(getBaseDir(), getCSVFormat()).export(getLocale(), Boolean.valueOf(full));
        } catch (IOException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }
    }

}
