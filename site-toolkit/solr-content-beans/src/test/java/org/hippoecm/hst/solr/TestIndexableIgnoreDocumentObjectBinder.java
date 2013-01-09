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

package org.hippoecm.hst.solr;

import org.apache.solr.common.SolrInputDocument;
import org.hippoecm.hst.solr.beans.TestIndexableIgnoreContentBean;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TestIndexableIgnoreDocumentObjectBinder {

    @Test
    public void testBaseContentBean() {
        DocumentObjectBinder binder = new DocumentObjectBinder();
        TestIndexableIgnoreContentBean contentBean = new TestIndexableIgnoreContentBean("my/simple/path");
        final SolrInputDocument solrInputDocument = binder.toSolrInputDocument(contentBean);
        // we now should have only the mandatory fields 'id' and DocumentObjectBinder.HIPPO_CONTENT_BEAN_FQN_CLAZZ_NAME
        assertTrue(solrInputDocument.getFieldNames().contains("id"));
        assertTrue(solrInputDocument.getFieldNames().contains(DocumentObjectBinder.HIPPO_CONTENT_BEAN_FQN_CLAZZ_NAME));

        assertFalse(solrInputDocument.getFieldNames().contains(DocumentObjectBinder.HIPPO_CONTENT_BEAN_FQN_CLAZZ_HIERARCHY));
        assertFalse(solrInputDocument.getFieldNames().contains(DocumentObjectBinder.HIPPO_CONTENT_BEAN_PATH_HIERARCHY));
        assertFalse(solrInputDocument.getFieldNames().contains(DocumentObjectBinder.HIPPO_CONTENT_BEAN_PATH_DEPTH));

        assertTrue(solrInputDocument.getFieldValue("id").equals(DocumentObjectBinder.NOOP_EMPTY_DOC_ID));
        Object s = solrInputDocument.getFieldValue(DocumentObjectBinder.HIPPO_CONTENT_BEAN_FQN_CLAZZ_NAME);
        assertTrue(solrInputDocument.getFieldValue(DocumentObjectBinder.HIPPO_CONTENT_BEAN_FQN_CLAZZ_NAME).equals(TestIndexableIgnoreContentBean.class.getName()));
        assertTrue(solrInputDocument.getFieldValues(DocumentObjectBinder.HIPPO_CONTENT_BEAN_FQN_CLAZZ_NAME).size() == 1);

    }

}
