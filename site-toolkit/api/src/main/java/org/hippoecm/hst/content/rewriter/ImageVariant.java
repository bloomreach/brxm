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
package org.hippoecm.hst.content.rewriter;

import java.util.List;

public interface ImageVariant {

    /**
     * @return the variant name to use. This value is never <code>null</code> or blank (empty)
     */
    String getName();

    /**
     * Returns the List of variants that should be replaced with {@link #getName()} and an EMPTY List if no explicit items for replaces are configured.
     * When <b>all</b> variants need to be replaced by default, this List can return EMPTY list
     * @return the List of variants that should be replaced with {@link #getName()} and an EMPTY List if no explicit items for replaces are configured
     */
    List<String> getReplaces();

    /**
     * @return <code>true</code> when the original variant should be shown when the variant for <code>name</code> does not exist
     */
    boolean isFallback();
}
