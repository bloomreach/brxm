/*
 *  Copyright 2012-2013 Hippo B.V. (http://www.onehippo.com)
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

import java.util.Collections;
import java.util.List;

import org.hippoecm.hst.content.rewriter.ImageVariant;

/**
 * <p>
 *  An image variant can hold information for a {@link org.hippoecm.hst.content.rewriter.ContentRewriter} which image variant to use instead of the image variant
 *  in the html content that the {@link org.hippoecm.hst.content.rewriter.ContentRewriter} will rewrite.
 * </p>
 * <p>
 *     
 * </p>
 */
public class DefaultImageVariant implements ImageVariant {
    /**
     * holds the name of the image variant to use. This value is not allowed to be <code>null</code> or blank
     */
    private String name;
    /**
     * replaces holds the List of variant names that should be replaces by <code>name</code>. When this List is <code>empty</code>,
     * it means that <b>all</b> variants will be replaced by <code>name</code>.
     */
    private List<String> replaces;
    /**
     * when fallback is <code>true</code>, the original variant will be used when the variant for <code>name</code> does not exist
     */
    private boolean fallback;
    
    public DefaultImageVariant(String name, List<String> replaces, boolean fallback) {
        this.name = name;
        this.replaces = replaces;
        this.fallback = fallback;
    }

    @Override
    public String getName() {
        return name;
    }


    @Override
    public List<String> getReplaces() {
        if (replaces == null) {
            return Collections.emptyList();
        }
        return replaces;
    }


    @Override
    public boolean isFallback() {
        return fallback;
    }
}
