/*
 *  Copyright 2010-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.repository.jackrabbit;

import javax.jcr.NamespaceException;

import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.spi.commons.conversion.CachingNameResolver;
import org.apache.jackrabbit.spi.commons.conversion.IllegalNameException;
import org.apache.jackrabbit.spi.commons.conversion.MalformedPathException;
import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;
import org.apache.jackrabbit.spi.commons.conversion.NameResolver;
import org.apache.jackrabbit.spi.commons.conversion.ParsingNameResolver;
import org.apache.jackrabbit.spi.commons.conversion.ParsingPathResolver;
import org.apache.jackrabbit.spi.commons.conversion.PathResolver;
import org.apache.jackrabbit.spi.commons.name.NameFactoryImpl;
import org.apache.jackrabbit.spi.commons.name.PathFactoryImpl;
import org.apache.jackrabbit.spi.commons.namespace.NamespaceResolver;

public class HippoNamePathResolver implements NamePathResolver {

    private final NameResolver nResolver;

    private final PathResolver pResolver;

    public HippoNamePathResolver(NamespaceResolver nsResolver, boolean enableCaching) {
        NameResolver nr = new ParsingNameResolver(NameFactoryImpl.getInstance(), nsResolver);
        PathResolver pr = new ParsingPathResolver(PathFactoryImpl.getInstance(), nr);
        if (enableCaching) {
            this.nResolver = new CachingNameResolver(nr);
            this.pResolver = new HippoCachingPathResolver(pr, nr);
        } else {
            this.nResolver = nr;
            this.pResolver = pr;
        }
    }

    public Name getQName(String name) throws IllegalNameException, NamespaceException {
        return nResolver.getQName(name);
    }

    public String getJCRName(Name name) throws NamespaceException {
        return nResolver.getJCRName(name);
    }

    public Path getQPath(String path) throws MalformedPathException, IllegalNameException, NamespaceException {
        return pResolver.getQPath(path);
    }

    public Path getQPath(String path, boolean normalizeIdentifier) throws MalformedPathException, IllegalNameException, NamespaceException {
        return pResolver.getQPath(path, normalizeIdentifier);
    }

    public String getJCRPath(Path path) throws NamespaceException {
        return pResolver.getJCRPath(path);
    }

}
