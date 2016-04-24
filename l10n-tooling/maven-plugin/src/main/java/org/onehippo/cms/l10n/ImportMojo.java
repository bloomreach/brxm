package org.onehippo.cms.l10n;

import java.io.IOException;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import static org.apache.maven.plugins.annotations.ResolutionScope.RUNTIME;

@Mojo(name = "import", defaultPhase = LifecyclePhase.VALIDATE, requiresDependencyResolution = RUNTIME)
public class ImportMojo extends AbstractL10nMojo {

    @Parameter(required = true)
    private String file;
    
    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
            new Importer(getBaseDir(), getCSVFormat())._import(file, getLocale());
        } catch (IOException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }
    }

}
