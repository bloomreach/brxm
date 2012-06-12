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
import org.hippoecm.hst.solr.beans.TestContentBean;
import org.hippoecm.hst.solr.beans.TestContentBeanWithCompounds;
import org.hippoecm.hst.solr.beans.compound.TestAddress;
import org.hippoecm.hst.solr.beans.compound.TestExplicitFieldEndingsAddress;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class TestCompoundDocumentObjectBinder {

    @Test
    public void testCompoundContentBean() {
        DocumentObjectBinder binder = new DocumentObjectBinder();


        Calendar cal = Calendar.getInstance();
        Date date = cal.getTime();
        String dateAsString = DateUtil.getThreadLocalDateFormat().format(date);
        Long mileage = 100L;
        Double price = 25.25;
        Boolean sold = true;
        long primitiveMileage = 50;
        double primitivePrice = 50.50;
        boolean primitiveSold = true;
        Integer[] longlat = new Integer[2];
        longlat[0] = 12354;
        longlat[1] = 324;
        
        TestAddress mainAddres = new TestAddress("oosteinde", 11, cal, date,
                mileage, price, sold, primitiveMileage, primitivePrice, primitiveSold, longlat);
        List<TestAddress> addressList = new ArrayList<TestAddress>();
        addressList.add(new TestAddress("oosteinde", 12, cal, date,
                mileage, price, sold, primitiveMileage, primitivePrice, primitiveSold, longlat));
        addressList.add(new TestAddress("eastend", 13, cal, date,
                mileage, price, sold, primitiveMileage, primitivePrice, primitiveSold, longlat));


        TestContentBeanWithCompounds testBean = new TestContentBeanWithCompounds("my/simple/path", mainAddres, addressList);

        SolrInputDocument doc = binder.toSolrInputDocument(testBean);


        // compound fields get there values mapped to fields with automatic endmappings :

        // integer --> _i
        // long --> _l
        // fload --> _f
        // date --> _dt
        // calendat --> _dt
        // etc

        // because:
        // @IndexField
        // public TestAddress getMainAddress() {
        //    return mainAddress;
        // }

        // will result in the 'mainAddress' prefix + the fieldnames or TestAddress compound + automatic endmapping

        assertTrue(doc.getFieldValue("mainAddress_street_compound_t").equals("oosteinde"));
        assertTrue(doc.getFieldValue("mainAddress_number_compound_i").equals(11));

        // Long mileage, Double price, Boolean sold should also all be there :
        assertTrue(doc.getFieldValue("mainAddress_mileage_compound_l").equals(mileage));
        assertTrue(doc.getFieldValue("mainAddress_price_compound_d").equals(price));
        assertTrue(doc.getFieldValue("mainAddress_sold_compound_b").equals(sold));

        // primitive values are auto-boxed to objects:
        // long primitiveMileage, double primitivePrice, primitiveSold ;
        assertTrue(doc.getFieldValue("mainAddress_primitiveMileage_compound_l") instanceof  Long);
        assertTrue(doc.getFieldValue("mainAddress_primitivePrice_compound_d") instanceof  Double);
        assertTrue(doc.getFieldValue("mainAddress_primitiveSold_compound_b") instanceof  Boolean);

        assertTrue(doc.getFieldValue("mainAddress_primitiveMileage_compound_l").equals(primitiveMileage));
        assertTrue(doc.getFieldValue("mainAddress_primitivePrice_compound_d").equals(primitivePrice));
        assertTrue(doc.getFieldValue("mainAddress_primitiveSold_compound_b").equals(primitiveSold));

        assertTrue(doc.getFieldValues("mainAddress_longlat_compound_mi").size() == 2);
        assertTrue(doc.getFieldValues("mainAddress_longlat_compound_mi").contains(12354));
        assertTrue(doc.getFieldValues("mainAddress_longlat_compound_mi").contains(324));

        // below should give same results for 'allAddresses' and 'theNameOfCopyAddresses' 
        String[] duplicateFields = {"allAddresses", "theNameOfCopyAddresses"};
        
        for (String fieldName : duplicateFields) {
            // The plural addressList should get its values returned as List and also contain '_multiple' in its name
            Collection<Object> streets = doc.getFieldValues(fieldName + "_street_multiple_compound_t");
            assertTrue("array input is expected to be returned as ArrayList", streets instanceof ArrayList<?>);
            assertTrue(streets.size() == 2);
            assertTrue(((ArrayList) streets).get(0) instanceof String);
            assertTrue(streets.contains("oosteinde"));
            assertTrue(streets.contains("eastend"));

            Collection<Object> numbers = doc.getFieldValues(fieldName + "_number_multiple_compound_i");
            assertTrue("array input is expected to be returned as ArrayList", numbers instanceof ArrayList<?>);
            assertTrue(numbers.size() == 2);
            assertTrue(((ArrayList) numbers).get(0) instanceof Integer);
            assertTrue(numbers.contains(new Integer(12)));
            assertTrue(numbers.contains(new Integer(13)));

            Collection<Object> mileages = doc.getFieldValues(fieldName + "_mileage_multiple_compound_l");
            assertTrue("array input is expected to be returned as ArrayList", mileages instanceof ArrayList<?>);
            assertTrue(mileages.size() == 2);
            assertTrue(((ArrayList) mileages).get(0) instanceof Long);
            assertTrue(mileages.contains(new Long(100)));

            Collection<Object> primitiveMileages = doc.getFieldValues(fieldName + "_primitiveMileage_multiple_compound_l");
            assertTrue("array input is expected to be returned as ArrayList", primitiveMileages instanceof ArrayList<?>);
            assertTrue(primitiveMileages.size() == 2);
            assertTrue(((ArrayList) primitiveMileages).get(0) instanceof Long);
            assertTrue(primitiveMileages.contains(new Long(50)));

            Collection<Object> solds = doc.getFieldValues(fieldName + "_sold_multiple_compound_b");
            assertTrue("array input is expected to be returned as ArrayList", solds instanceof ArrayList<?>);
            assertTrue(solds.size() == 2);
            assertTrue(((ArrayList) solds).get(0) instanceof Boolean);
            assertTrue(solds.contains(new Boolean(true)));

            Collection<Object> primitiveSolds = doc.getFieldValues(fieldName + "_primitiveSold_multiple_compound_b");
            assertTrue("array input is expected to be returned as ArrayList", primitiveSolds instanceof ArrayList<?>);
            assertTrue(primitiveSolds.size() == 2);
            assertTrue(((ArrayList) primitiveSolds).get(0) instanceof Boolean);
            assertTrue(primitiveSolds.contains(new Boolean(true)));

            Collection<Object> prices = doc.getFieldValues(fieldName + "_price_multiple_compound_d");
            assertTrue("array input is expected to be returned as LinkedList", prices instanceof ArrayList<?>);
            assertTrue(prices.size() == 2);
            assertTrue(((ArrayList) prices).get(0) instanceof Double);
            assertTrue(prices.contains(new Double(25.25)));

            Collection<Object> primitivePrices = doc.getFieldValues(fieldName + "_primitivePrice_multiple_compound_d");
            assertTrue("array input is expected to be returned as ArrayList", primitivePrices instanceof ArrayList<?>);
            assertTrue(primitivePrices.size() == 2);
            assertTrue(((ArrayList) primitivePrices).get(0) instanceof Double);
            assertTrue(primitivePrices.contains(new Double(50.50)));

            Collection<Object> dates = doc.getFieldValues(fieldName + "_date_multiple_compound_dt");
            assertTrue("array input is expected to be returned as ArrayList", dates instanceof ArrayList<?>);
            assertTrue(dates.size() == 2);
            assertTrue(((ArrayList) dates).get(0) instanceof String);
            assertTrue(dates.contains(dateAsString));

            Collection<Object> calendars = doc.getFieldValues(fieldName + "_calendar_multiple_compound_dt");
            assertTrue("array input is expected to be returned as ArrayList", calendars instanceof ArrayList<?>);
            assertTrue(calendars.size() == 2);
            assertTrue(((ArrayList) calendars).get(0) instanceof String);
            assertTrue(calendars.contains(dateAsString));

            // since we have two compounds both with longlat 12354 and 324 we have size 4 in total
            assertTrue(doc.getFieldValues(fieldName + "_longlat_multiple_compound_mi").size() == 4);
            assertTrue(doc.getFieldValues(fieldName + "_longlat_multiple_compound_mi").contains(12354));
            assertTrue(doc.getFieldValues(fieldName + "_longlat_multiple_compound_mi").contains(324));

        }
        
        
    }

    @Test
    public void testExplicitFieldEndingsCompoundContentBean() {
        DocumentObjectBinder binder = new DocumentObjectBinder();

        Calendar cal = Calendar.getInstance();
        String dateAsString = DateUtil.getThreadLocalDateFormat().format(cal.getTime());

        TestExplicitFieldEndingsAddress explicitFieldsAddress = new TestExplicitFieldEndingsAddress("oosteinde", 11, cal);

        TestContentBeanWithCompounds testBean = new TestContentBeanWithCompounds("my/simple/path", explicitFieldsAddress);

        SolrInputDocument doc = binder.toSolrInputDocument(testBean);

        // because the TestExplicitFieldEndingsAddress has @IndexField names ending with _xxx we should find these in the doc
        assertNotNull(doc.getFieldValues("explicitFieldsAddress_calendar_cal"));
        assertNotNull(doc.getFieldValues("explicitFieldsAddress_number_int"));
        assertNotNull(doc.getFieldValues("explicitFieldsAddress_street_text"));

        assertTrue(doc.getFieldValue("explicitFieldsAddress_calendar_cal").equals(dateAsString));
        assertTrue(doc.getFieldValue("explicitFieldsAddress_number_int").equals(new Integer(11)));
        assertTrue(doc.getFieldValue("explicitFieldsAddress_street_text").equals("oosteinde"));

    }

    @Test
    public void testPopulateCompoundContentBean() {
        DocumentObjectBinder binder = new DocumentObjectBinder();

        Calendar cal = Calendar.getInstance();
        Date date = cal.getTime();
        String dateAsString = DateUtil.getThreadLocalDateFormat().format(date);
        Long mileage = 100L;
        Double price = 25.25;
        Boolean sold = true;
        long primitiveMileage = 50;
        double primitivePrice = 50.50;
        boolean primitiveSold = true;

        Integer[] longlat = new Integer[2];
        longlat[0] = 12354;
        longlat[1] = 324;

        TestAddress mainAddres = new TestAddress("oosteinde", 11, cal, date,
                mileage, price, sold, primitiveMileage, primitivePrice, primitiveSold, longlat);
        List<TestAddress> addressList = new ArrayList<TestAddress>();
        addressList.add(new TestAddress("oosteinde", 12, cal, date,
                mileage, price, sold, primitiveMileage, primitivePrice, primitiveSold, longlat));
        addressList.add(new TestAddress("oosteinde", 13, cal, date,
                mileage, price, sold, primitiveMileage, primitivePrice, primitiveSold, longlat));


        TestContentBeanWithCompounds testBean = new TestContentBeanWithCompounds("my/simple/path", mainAddres, addressList);


        SolrInputDocument toSolrInputDoc = binder.toSolrInputDocument(testBean);

        SolrDocument solrDoc = new SolrDocument();
        for (String fieldName : toSolrInputDoc.getFieldNames()) {

            Collection<Object> values = toSolrInputDoc.getFieldValues(fieldName);
            if (values.size() == 1) {
               Object val = values.iterator().next();
               solrDoc.setField(fieldName, val);
            } else {
              solrDoc.setField(fieldName, values);
            }
        }

        TestContentBeanWithCompounds bindedBean = binder.getBean(TestContentBeanWithCompounds.class, solrDoc);

        assertNull("Compounds should not get RE-populated, even when there is a setter",bindedBean.getAllAddresses());
        assertNull("Compounds should not get RE-populated, even when there is a setter",bindedBean.getMainAddress());
        assertNull("Compounds should not get RE-populated, even when there is a setter",bindedBean.getExplicitFieldsAddress());

        assertTrue(bindedBean.getIdentifier().equals("my/simple/path"));

    }


}
