package org.onehippo.cms.l10n;

import java.io.IOException;

import org.apache.maven.plugin.MojoExecutionException;

public abstract class AbstractRegistrarMojo extends AbstractL10nMojo {

    private Registrar registrar;
    
    protected Registrar getRegistrar() throws IOException, MojoExecutionException {
        if (registrar == null) {
            registrar = new Registrar(getBaseDir(), getModuleName(), getLocales(), getResourcesClassLoader());
        }
        return registrar;
    }
    
}
