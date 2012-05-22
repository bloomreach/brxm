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

package org.hippoecm.hst.solr;

import org.apache.solr.common.SolrInputDocument;
import org.hippoecm.hst.solr.beans.TestContentBeanWithIgnoredFieldsForCompounds;
import org.hippoecm.hst.solr.beans.compound.TestIgnoredSuperFieldsAddress;
import org.junit.Test;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class TestIgnoredSuperFieldsCompoundDocumentObjectBinder {

    @Test
    public void testMissingPathIndexFieldForCompound() {
        DocumentObjectBinder binder = new DocumentObjectBinder();

        TestIgnoredSuperFieldsAddress address =
                new TestIgnoredSuperFieldsAddress("/my/path/to/address","oosteinde", 11);


        TestContentBeanWithIgnoredFieldsForCompounds compoundBean =
                new TestContentBeanWithIgnoredFieldsForCompounds("my/simple/path", address);




        /* the TestIgnoredSuperFieldsAddress implements IdentifiableContentBean, which contains:
        * @IgnoreForCompoundBean
        * @IndexField(name="id")
        * String getPath();
        *
        * Hence, the compound TestIgnoredSuperFieldsAddress should get its supertype getPath IGNORED for indexing, thus
        * there should be no field for "id" when the TestIgnoredSuperFieldsAddress is indexed in TestContentBeanWithIgnoredFieldsForCompounds
        * However, when it is indexed directly and NOT as compound, then the "id" MUST be there
        *
        */

        SolrInputDocument addressAsDoc = binder.toSolrInputDocument(address);
        // now also "id" should be there because directly indexed
        assertTrue(addressAsDoc.getFieldValue("id").equals("/my/path/to/address"));
        assertTrue(addressAsDoc.getFieldValue("street").equals("oosteinde"));
        assertTrue(addressAsDoc.getFieldValue("number").equals(11));


        // now, "id" from compound doc should be skipped due to @IgnoreForCompoundBean
        SolrInputDocument addressAsCompoundDoc = binder.toSolrInputDocument(compoundBean);

        assertTrue(addressAsCompoundDoc.getFieldValue("address_street_compound_t").equals("oosteinde"));
        assertTrue(addressAsCompoundDoc.getFieldValue("address_number_compound_i").equals(11));

        assertNull("There should be no field for address_id_compound_t because of @IgnoreForCompoundBean",
                addressAsCompoundDoc.getFieldValue("address_id_compound_t"));

    }


}
