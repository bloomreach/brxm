/**
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *         http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.hst.cache.esi;

/**
 * ESIFragmentType
 */
public enum ESIFragmentType {

    /** ESI comment block. e.g., "&lt;!--esi ... --&gt;" */
    COMMENT_BLOCK,

    /** ESI include tag block. e.g., "&lt;esi:include .../&gt;" */
    INCLUDE_TAG,

    /** ESI comment tag block. e.g., "&lt;esi:comment .../&gt;" */
    COMMENT_TAG,

    /** ESI remove tag block. e.g., "&lt;esi:remove&gt;...&lt;/esi:remove&gt;" */
    REMOVE_TAG,

    /** ESI vars tag block. e.g., "&lt;esi:vars&gt; ... &lt;/esi:vars&gt;" */
    VARS_TAG

}
