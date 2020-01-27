/*
 *  Copyright 2008-2020 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.content.beans.standard;


import org.hippoecm.hst.content.annotations.PageModelIgnore;

/**
 * Implementing classes represent a html node in the ecm repository. 
 */
public interface HippoHtmlBean extends HippoBean {


    @PageModelIgnore
    @Override
    String getName();

    @PageModelIgnore
    @Override
    String getDisplayName();

    /**
     * <p>
     *     Since in the Page Model API (PMA) we do not want to serialize the raw String content (but instead a rewritten
     *     content resolving internal links), we ignore the {@link #getContent()} in PMA via {@link @PageModelIgnore}
     * </p>
     * @return the string value of the content for the html bean
     */
    @PageModelIgnore
    String getContent();
    
}
