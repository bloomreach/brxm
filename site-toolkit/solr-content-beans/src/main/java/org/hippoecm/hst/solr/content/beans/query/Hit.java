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
package org.hippoecm.hst.solr.content.beans.query;

import java.io.Serializable;
import java.util.List;

import org.apache.solr.common.SolrDocument;
import org.hippoecm.hst.content.beans.standard.ContentBean;

public interface Hit extends Serializable {

    SolrDocument getDoc();

    ContentBean getContentBean();

    /**
     * @return the score for this hit and -1 if there is no score available
     */
    float getScore();

    /**
     * @return the {@link List} of {@link Highlight}s and empty List if there are no highlights
     */
    public List<Highlight> getHighlights();
}
