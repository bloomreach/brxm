package org.onehippo.cms.l10n;

import java.io.IOException;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

import static org.apache.maven.plugins.annotations.ResolutionScope.RUNTIME;

@Mojo(name = "add-locale", defaultPhase = LifecyclePhase.VALIDATE, requiresDependencyResolution = RUNTIME)
public class AddLocaleMojo extends AbstractRegistrarMojo {
    
    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
            getRegistrar().addLocale(getLocale());
        } catch (IOException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }
    }
}
