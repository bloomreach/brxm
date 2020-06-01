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
package org.hippoecm.hst.solr.content.beans.query;

import java.io.Serializable;
import java.util.List;

import org.apache.solr.common.SolrDocument;
import org.hippoecm.hst.content.beans.standard.IdentifiableContentBean;
import org.hippoecm.hst.solr.content.beans.BindingException;

public interface Hit extends Serializable {

    SolrDocument getDoc();

    /**
     * @return the {@link org.hippoecm.hst.content.beans.standard.IdentifiableContentBean}
     * @throws BindingException when the {@link SolrDocument} cannot be binded to the {@link org.hippoecm.hst.content.beans.standard.IdentifiableContentBean}
     */
    IdentifiableContentBean getBean() throws BindingException;

    /**
     * @return the score for this hit and -1 if there is no score available
     */
    float getScore();

    /**
     * @return the {@link List} of {@link Highlight}s and empty List if there are no highlights
     */
    public List<Highlight> getHighlights();
}
