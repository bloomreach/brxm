/*
 *  Copyright 2014-2015 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.freemarker.jcr;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

import org.hippoecm.hst.site.HstServices;

import freemarker.cache.TemplateLoader;

public abstract class AbstractTemplateLoader implements TemplateLoader {

    protected TemplateLoadingCache getLoadingCache() {
        return HstServices.getComponentManager().getComponent(
                TemplateLoadingCache.class,
                "org.hippoecm.hst.freemarker");
    }

    public void closeTemplateSource(Object templateSource) throws IOException {
        return;
    }

    public long getLastModified(Object templateSource) {
        try {
            validateTemplateSourceObject(templateSource);
        } catch (IOException e) {
            throw new IllegalStateException(e.toString(), e);
        }
        return ((RepositorySource)templateSource).getPlaceHolderLastModified();
    }


    public Reader getReader(Object templateSource, String encoding) throws IOException {
        validateTemplateSourceObject(templateSource);
        final RepositorySource repoSource = (RepositorySource) templateSource;
        if (!repoSource.isFound()) {
            String msg = String.format("Repository templateSource '%s' not found",
                    repoSource.getAbsJcrPath());
            throw new IOException(msg);
        }
        return new StringReader(repoSource.getTemplate());
    }

    private void validateTemplateSourceObject(final Object templateSource) throws IOException {
        if (!(templateSource instanceof RepositorySource)) {
            String msg = String.format("templateSource should be of type '%s' but was of type '%s'",
                    RepositorySource.class.getName(), templateSource.getClass().getName());
            throw new IOException(msg);
        }
    }


}
