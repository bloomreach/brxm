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
package org.hippoecm.hst.content.beans.standard;

import org.hippoecm.hst.content.beans.Node;
import org.hippoecm.hst.content.beans.index.Indexable;
import org.hippoecm.repository.api.HippoNode;

/**
 * This bean is only to map a hippo:translation to a bean: Normally, you never use this bean at all, as translations are 
 * available through {@link HippoNode#getLocalizedName()}
 *
 */

@Indexable(ignore = true)
@Node(jcrType="hippo:translation")
public class HippoTranslation extends HippoItem {
    
}
