/**
 * Copyright (C) 2012 Hippo B.V.
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

package org.hippoecm.hst.demo.beans;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.RepositoryException;

import org.hippoecm.hst.content.beans.Node;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.content.beans.standard.HippoFacetSelect;
import org.hippoecm.hst.content.beans.standard.HippoFolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Node(jcrType = "relateddocs:docs")
public class RelatedDocsBean extends HippoFolder {

    private static final Logger log = LoggerFactory.getLogger(RelatedDocsBean.class);

    public List<HippoBean> getRelatedDocs(String primaryNodeType) {
        if (primaryNodeType == null) {
            return null;
        }

        List<HippoFacetSelect> relatedDocs = getChildBeansByName("relateddocs:reldoc");

        if (relatedDocs != null) {
            List<HippoBean> beans = new ArrayList<HippoBean>(relatedDocs.size());
            try {
                for (HippoFacetSelect facetSelect : relatedDocs) {
                    if (facetSelect != null) {
                        HippoBean bean = facetSelect.getReferencedBean();
                        if (bean != null) {
                            String nodeType = bean.getNode().getPrimaryNodeType().getName();
                            if (nodeType.equals(primaryNodeType)) {
                                beans.add(bean);
                            }
                        }
                    }
                }
            } catch (RepositoryException e) {
                log.warn("Error while searching related documents of type " + primaryNodeType, e);
            }
            return beans;
        }

        return null;
    }

}
