/*
 *  Copyright 2008 Hippo.
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
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.hst.core.request.HstRequestContext;

/**
 * AbstractContentRewriter
 * <P>
 * An abstract adapter class for {@link ContentEditor}. 
 * The methods in this class are empty. 
 * This class exists as convenience for creating <CODE>ContentRewriter</CODE> objects.
 * </P>
 * @version $Id$
 */
public abstract class AbstractContentRewriter<T> implements ContentRewriter<T> {
    
    public AbstractContentRewriter() {
        
    }
    
    /**
     * @deprecated Use {@link #rewrite(Object, Node, HstRequestContext)} instead.
     */
    public T rewrite(T content, Node contentNode, HstRequest request, HstResponse response) {
        return null;
    }

    public T rewrite(T content, Node contentNode, HstRequestContext requestContext) {
        return null;
    }

    public T rewrite(T content, Node contentNode, HstRequestContext requestContext, String targetSiteMountAlias) {
        return null;
    }
    
    public T rewrite(T content, Node contentNode, HstRequestContext requestContext, Mount targetMount) {
        return null;
    }
    
}
