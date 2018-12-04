/*
 * Copyright 2018 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *      http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.hippoecm.repository.deriveddata;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;

import org.hippoecm.repository.HierarchyResolverImpl;
import org.hippoecm.repository.api.HierarchyResolver;

public class ResolvePropertyResolver implements PropertyResolver {

    private final HierarchyResolver.Entry lastNode;
    private final String relativePath;
    private final Node modified;
    private Property property;

    public ResolvePropertyResolver(final HierarchyResolver.Entry lastNode, final String relativePath
            , final Node modified) {
        this.lastNode = lastNode;
        this.relativePath = relativePath;
        this.modified = modified;
    }

    public static Property getProperty(final HierarchyResolver.Entry lastNode, final String relativePath
            , final Node modified) throws RepositoryException {
        ResolvePropertyResolver resolvePropertyResolver = new ResolvePropertyResolver(lastNode, relativePath, modified);
        resolvePropertyResolver.resolve();
        return resolvePropertyResolver.getProperty();
    }

    @Override
    public Property getProperty() {
        return this.property;
    }

    @Override
    public String getRelativePath(){
        return relativePath;
    }

    @Override
    public Node getModified(){
        return modified;
    }

    @Override
    public void resolve() throws RepositoryException {
        this.property = new HierarchyResolverImpl().getProperty(modified, relativePath, lastNode);

    }
}
