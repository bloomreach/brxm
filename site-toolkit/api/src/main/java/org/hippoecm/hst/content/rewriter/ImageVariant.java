/*
 *  Copyright 2012 Hippo.
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
package org.hippoecm.hst.content.rewriter;

import java.util.Collections;
import java.util.List;

/**
 * <p>
 *  An image variant can hold information for a {@link ContentRewriter} which image variant to use instead of the image variant
 *  in the html content that the {@link ContentRewriter} will rewrite.
 * </p>
 * <p>
 *     
 * </p>
 */
public class ImageVariant {
    /**
     * holds the name of the image variant to use. This value is not allowed to be <code>null</code> or blank
     */
    private String name;
    /**
     * replace holds the List of variant names that should be replace by <code>name</code>. When this List is <code>empty</code>,
     * it means that <b>all</b> variants will be replaced by <code>name</code>.
     */
    private List<String> replace;
    /**
     * when fallback is <code>true</code>, the original variant will be used when the variant for <code>name</code> does not exist
     */
    private boolean fallback;
    
    public ImageVariant(String name, List<String> replace, boolean fallback) {
        this.name = name;
        this.replace = replace;
        this.fallback = fallback;
    }

    /**
     * @return the variant name to use. This value is never <code>null</code> or blank (empty)
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the List of variants that should be replaced with {@link #getName()} and an EMPTY List if no explicit items for replace are configured.
     * When <b>all</b> variants need to be replaced by default, this List can return EMPTY list
     * @return the List of variants that should be replaced with {@link #getName()} and an EMPTY List if no explicit items for replace are configured
     */
    public List<String> getReplace() {
        if (replace == null) {
            return Collections.emptyList();
        }
        return replace;
    }

    /**
     * @return <code>true</code> when the original variant should be shown when the variant for <code>name</code> does not exist
     */
    public boolean isFallback() {
        return fallback;
    }
}
