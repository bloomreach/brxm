/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.content.rewriter.impl;

import javax.jcr.Node;

import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.content.rewriter.ContentRewriter;
import org.hippoecm.hst.content.rewriter.ImageVariant;
import org.hippoecm.hst.core.request.HstRequestContext;

/**
 * AbstractContentRewriter
 * <p>
 * An abstract adapter class for {@link ContentRewriter}.
 * The methods in this class are empty. 
 * This class exists as convenience for creating <code>ContentRewriter</code> objects.
 * </p>
 * @version $Id$
 */
public abstract class AbstractContentRewriter<T> implements ContentRewriter<T> {
    
    private boolean fullyQualifiedLinks;
    private boolean canonicalLinks;
    private ImageVariant imageVariant;
    
    public AbstractContentRewriter() {
        
    }

    @Override
    public T rewrite(final T content, final HstRequestContext requestContext) {
        return null;
    }

    @Override
    public T rewrite(T content, Node contentNode, HstRequestContext requestContext) {
        return null;
    }

    @Override
    public T rewrite(T content, Node contentNode, HstRequestContext requestContext, String targetMountAlias) {
        return null;
    }
    
    @Override
    public T rewrite(T content, Node contentNode, HstRequestContext requestContext, Mount targetMount) {
        return null;
    }

    @Override
    public void setFullyQualifiedLinks(boolean fullyQualifiedLinks) {
        this.fullyQualifiedLinks = fullyQualifiedLinks;
    }

    @Override
    public boolean isFullyQualifiedLinks() {
        return fullyQualifiedLinks;
    }

    @Override
    public void setCanonicalLinks(final boolean canonicalLinks) {
        this.canonicalLinks = canonicalLinks;
    }

    @Override
    public boolean isCanonicalLinks() {
        return canonicalLinks;
    }

    @Override
    public void setImageVariant(final ImageVariant imageVariant) {
        this.imageVariant = imageVariant;
    }

    @Override
    public ImageVariant getImageVariant() {
        return imageVariant;
    }
}
