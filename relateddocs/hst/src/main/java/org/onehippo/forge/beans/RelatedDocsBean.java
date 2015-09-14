/*
 * Copyright 2009-2015 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/

package org.onehippo.forge.beans;

import java.util.ArrayList;
import java.util.List;
import org.hippoecm.hst.content.beans.Node;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.content.beans.standard.HippoFacetSelect;
import org.hippoecm.hst.content.beans.standard.HippoItem;

@Node(jcrType = "relateddocs:docs")
public class RelatedDocsBean extends HippoItem {

    public List<HippoBean> getDocs() {

        List<HippoFacetSelect> reldocsfs = this.getChildBeans("hippo:facetselect");

        List<HippoBean> docs = new ArrayList<HippoBean>();

        for (HippoFacetSelect fs : reldocsfs) {
            HippoBean b = fs.getReferencedBean();

            if (b != null) {
                docs.add(b);
            }
        }

        return docs;
    }
}