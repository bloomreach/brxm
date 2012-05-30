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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.util.DateUtil;
import org.hippoecm.hst.content.beans.index.IndexField;
import org.hippoecm.hst.solr.DocumentObjectBinder;
import org.hippoecm.hst.solr.beans.TestContentBeanWithCompositeCompound;
import org.hippoecm.hst.solr.beans.TestContentBeanWithCompounds;
import org.hippoecm.hst.solr.beans.compound.TestAddress;
import org.hippoecm.hst.solr.beans.compound.TestCompositeAddress;
import org.hippoecm.hst.solr.beans.compound.TestExplicitFieldEndingsAddress;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class TestCompositeCompoundDocumentObjectBinder {

    @Test
    public void testCompositeCompoundContentBean() {
        DocumentObjectBinder binder = new DocumentObjectBinder();


        TestCompositeAddress level3 =  new TestCompositeAddress("oostei", 3, null);
        TestCompositeAddress level2=  new TestCompositeAddress("oostein", 2, level3);
        TestCompositeAddress level1=  new TestCompositeAddress("oosteind", 1, level2);
        TestCompositeAddress level0 = new TestCompositeAddress("oosteinde", 0, level1);

        TestContentBeanWithCompositeCompound bean = new TestContentBeanWithCompositeCompound("/foo/bar", level0);
;
        /*
         * We now have a TestContentBeanWithCompositeCompound bean now with a composite compound in it:
         *
         * TestContentBeanWithCompositeCompound
         *          ` level0
         *               ` level1
         *                     ` level2
         *                          level3
         *
         */


        SolrInputDocument doc = binder.toSolrInputDocument(bean);

        // composite compound fields get there fields delimited by "_" and get their values mapped to fields with automatic endmappings
        // when they do not contain an endmapping themselves.


        // because TestContentBeanWithCompositeCompound contains
        // @IndexField
        // public TestCompositeAddress getRootAddress() {
        //    return rootAddress;
        // }
        // AND
        // because TestCompositeAddress contains
        // @IndexField
        // public String getStreet() {
        //    return street;
        // }
        // @IndexField
        // public TestCompositeAddress getChild() {
        //    return child;
        // }

        // AND TestCompositeAddress is composite

        // We expect
        // 1: 'rootAddress_street_compound_t'
        // 2: 'rootAddress_number_compound_i'
        // 3: 'rootAddress_child_street_compound_t'
        // 3: 'rootAddress_child_number_compound_i'
        // 3: 'rootAddress_child_child_street_compound_t'
        // 3: 'rootAddress_child_child_number_compound_i'
        // 3: 'rootAddress_child_child_child_street_compound_t'
        // 3: 'rootAddress_child_child_child_number_compound_i'

        // will result in the 'mainAddress' prefix + the fieldnames or TestAddress compound + automatic endmapping

        assertTrue(doc.getFieldValue("rootAddress_street_compound_t").equals("oosteinde"));
        assertTrue(doc.getFieldValue("rootAddress_number_compound_i").equals(0));


        assertTrue(doc.getFieldValue("rootAddress_child_street_compound_t").equals("oosteind"));
        assertTrue(doc.getFieldValue("rootAddress_child_number_compound_i").equals(1));

        assertTrue(doc.getFieldValue("rootAddress_child_child_street_compound_t").equals("oostein"));
        assertTrue(doc.getFieldValue("rootAddress_child_child_number_compound_i").equals(2));

        assertTrue(doc.getFieldValue("rootAddress_child_child_child_street_compound_t").equals("oostei"));
        assertTrue(doc.getFieldValue("rootAddress_child_child_child_number_compound_i").equals(3));

        // NOW TEST THE EXPLICIT FIELD ENDINGS :

        // Because TestCompositeAddress also contains

        // @IndexField(name = "numberAsWell_i")
        // public int getNumberWithEndMapping() {
        //    return numberWithEndMapping;
        // }
        // we also expect :
        // 1: 'rootAddress_numberAsWell_i'
        // 2: 'rootAddress_child_numberAsWell_i'
        // 3: 'rootAddress_child_child_numberAsWell_i'
        // 4: 'rootAddress_child_child_child_numberAsWell_i'

        assertTrue(doc.getFieldValue("rootAddress_numberAsWell_i").equals(0));
        assertTrue(doc.getFieldValue("rootAddress_child_numberAsWell_i").equals(1));
        assertTrue(doc.getFieldValue("rootAddress_child_child_numberAsWell_i").equals(2));
        assertTrue(doc.getFieldValue("rootAddress_child_child_child_numberAsWell_i").equals(3));

    }
}
