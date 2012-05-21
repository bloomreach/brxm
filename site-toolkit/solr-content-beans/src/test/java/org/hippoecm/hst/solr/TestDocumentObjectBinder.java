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
import java.util.LinkedList;

import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.util.DateUtil;
import org.hippoecm.hst.content.beans.standard.ContentBean;
import org.hippoecm.hst.content.beans.standard.IdentifiableContentBean;
import org.hippoecm.hst.solr.beans.TestBaseContentBean;
import org.hippoecm.hst.solr.beans.TestContentBean;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class TestDocumentObjectBinder {

    // TODO  ADD TESTS FOR mileage, price and sold !!! : Long, Double, Boolean
    // AND long, double, boolean native

    // private Long mileage;
    // private Double price;
    //  private Boolean sold;

    @Test
    public void testBaseContentBean() {
        DocumentObjectBinder binder = new DocumentObjectBinder();
        TestBaseContentBean contentBean = new TestBaseContentBean("my/simple/path");
        final SolrInputDocument solrInputDocument = binder.toSolrInputDocument(contentBean);
        // we now should have some *always* available fields
        assertTrue(solrInputDocument.getFieldNames().contains("id"));
        assertTrue(solrInputDocument.getFieldNames().contains(DocumentObjectBinder.HIPPO_CONTENT_BEAN_FQN_CLAZZ_NAME));
        assertTrue(solrInputDocument.getFieldNames().contains(DocumentObjectBinder.HIPPO_CONTENT_BEAN_FQN_CLAZZ_HIERARCHY));
        assertTrue(solrInputDocument.getFieldNames().contains(DocumentObjectBinder.HIPPO_CONTENT_BEAN_PATH_HIERARCHY));
        assertTrue(solrInputDocument.getFieldNames().contains(DocumentObjectBinder.HIPPO_CONTENT_BEAN_PATH_DEPTH));

        assertTrue(solrInputDocument.getFieldValue("id").equals("my/simple/path"));

        // assert some paths
        final Collection<Object> paths = solrInputDocument.getFieldValues(DocumentObjectBinder.HIPPO_CONTENT_BEAN_PATH_HIERARCHY);
        assertTrue(paths.contains("my"));
        assertTrue(paths.contains("my/simple"));
        assertTrue(paths.contains("my/simple/path"));

        // assert path depth
        assertTrue(solrInputDocument.getFieldValue(DocumentObjectBinder.HIPPO_CONTENT_BEAN_PATH_DEPTH).equals(new Integer(3)));
        assertTrue(solrInputDocument.getFieldValues(DocumentObjectBinder.HIPPO_CONTENT_BEAN_PATH_DEPTH).size() == 1);

        assertTrue(solrInputDocument.getFieldValue(DocumentObjectBinder.HIPPO_CONTENT_BEAN_FQN_CLAZZ_NAME).equals(TestBaseContentBean.class.getName()));
        assertTrue(solrInputDocument.getFieldValues(DocumentObjectBinder.HIPPO_CONTENT_BEAN_FQN_CLAZZ_NAME).size() == 1);


        // the class TestBaseContentBean and the IdentifiableContentBean interface
        assertTrue(solrInputDocument.getFieldValues(DocumentObjectBinder.HIPPO_CONTENT_BEAN_FQN_CLAZZ_HIERARCHY).size() == 3);
        final Collection<Object> clazzHierarchyValues = solrInputDocument.getFieldValues(DocumentObjectBinder.HIPPO_CONTENT_BEAN_FQN_CLAZZ_HIERARCHY);
        assertTrue(clazzHierarchyValues.contains(TestBaseContentBean.class.getName()));
        assertTrue(clazzHierarchyValues.contains(IdentifiableContentBean.class.getName()));
        assertTrue(clazzHierarchyValues.contains(ContentBean.class.getName()));

    }

    @Test
    public void testIncorrectBaseContentBean() {
        DocumentObjectBinder binder = new DocumentObjectBinder();
        // path is not allowed to be null
        TestBaseContentBean contentBean = new TestBaseContentBean(null);
        boolean expectedExceptionFound = false;
        try {
            final SolrInputDocument solrInputDocument = binder.toSolrInputDocument(contentBean);
            fail("We should't get here as new TestBaseContentBean(null) has a non allowed null path");
        } catch (IllegalStateException e) {
            expectedExceptionFound = true;
        }
              
        assertTrue("An IllegalStateException should have happened",expectedExceptionFound);
    }

    @Test
    public void testPathHierarchiesForContentBeans() {
        DocumentObjectBinder binder = new DocumentObjectBinder();
        TestBaseContentBean bean1 = new TestBaseContentBean("my/simple/path");
        TestBaseContentBean bean2 = new TestBaseContentBean("/my/simple/path");
        // trailing slashes are ignored, thus /my/simple/path results in same as /my/simple/path/
        TestBaseContentBean bean3 = new TestBaseContentBean("/my/simple/path/");

        final SolrInputDocument solrInputDocument1 = binder.toSolrInputDocument(bean1);
        final SolrInputDocument solrInputDocument2 = binder.toSolrInputDocument(bean2);
        final SolrInputDocument solrInputDocument3 = binder.toSolrInputDocument(bean3);

        final Collection<Object> paths1 = solrInputDocument1.getFieldValues(DocumentObjectBinder.HIPPO_CONTENT_BEAN_PATH_HIERARCHY);
        assertTrue(paths1.contains("my"));
        assertTrue(paths1.contains("my/simple"));
        assertTrue(paths1.contains("my/simple/path"));
        assertTrue(solrInputDocument1.getFieldValue(DocumentObjectBinder.HIPPO_CONTENT_BEAN_PATH_DEPTH).equals(new Integer(3)));


        final Collection<Object> paths2 = solrInputDocument2.getFieldValues(DocumentObjectBinder.HIPPO_CONTENT_BEAN_PATH_HIERARCHY);
        assertTrue(paths2.contains("/my"));
        assertTrue(paths2.contains("/my/simple"));
        assertTrue(paths2.contains("/my/simple/path"));
        assertTrue(solrInputDocument2.getFieldValue(DocumentObjectBinder.HIPPO_CONTENT_BEAN_PATH_DEPTH).equals(new Integer(3)));

        final Collection<Object> paths3 = solrInputDocument3.getFieldValues(DocumentObjectBinder.HIPPO_CONTENT_BEAN_PATH_HIERARCHY);
        assertTrue(paths3.contains("/my"));
        assertTrue(paths3.contains("/my/simple"));
        assertTrue(paths3.contains("/my/simple/path"));
        assertTrue(solrInputDocument3.getFieldValue(DocumentObjectBinder.HIPPO_CONTENT_BEAN_PATH_DEPTH).equals(new Integer(3)));



        // test http path
        SolrInputDocument doc = binder.toSolrInputDocument(new TestBaseContentBean("http://www.example.com"));
        Collection<Object> paths = doc.getFieldValues(DocumentObjectBinder.HIPPO_CONTENT_BEAN_PATH_HIERARCHY);
        assertTrue(paths.contains("http:"));
        assertTrue(paths.contains("http:/"));
        assertTrue(paths.contains("http://www.example.com"));
        assertTrue(doc.getFieldValue(DocumentObjectBinder.HIPPO_CONTENT_BEAN_PATH_DEPTH).equals(new Integer(3)));


        // test http path
        doc = binder.toSolrInputDocument(new TestBaseContentBean("http://www.example.com/foo/bar"));
        paths = doc.getFieldValues(DocumentObjectBinder.HIPPO_CONTENT_BEAN_PATH_HIERARCHY);
        assertTrue(paths.contains("http:"));
        assertTrue(paths.contains("http:/"));
        assertTrue(paths.contains("http://www.example.com"));
        assertTrue(paths.contains("http://www.example.com/foo"));
        assertTrue(paths.contains("http://www.example.com/foo/bar"));
        assertTrue(doc.getFieldValue(DocumentObjectBinder.HIPPO_CONTENT_BEAN_PATH_DEPTH).equals(new Integer(5)));
    }

    @Test
    public void testContentBean() {
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

        TestContentBean myTestBean = new TestContentBean("my/simple/path", "titleValue", "summaryValue",
                new String[] {"foo", "bar"}, cal, date,
                mileage, price, sold, primitiveMileage, primitivePrice, primitiveSold );

        SolrInputDocument doc = binder.toSolrInputDocument(myTestBean);

        // because :
        // @IndexField
        // public String getTitle() {
        //       return title;
        // }
        assertTrue(doc.getFieldValue("title").equals("titleValue"));

        // because :
        // @IndexField
        // public String getSummary() {
        //       return summary;
        // }
        assertTrue(doc.getFieldValue("summary").equals("summaryValue"));

        // because :
        // @IndexField
        // public String[] getAuthors() {
        //    return authors;
        // }
        // getFieldValue returns the FIRST one
        assertTrue(doc.getFieldValue("authors").equals("foo"));
        Collection<Object> objects = doc.getFieldValues("authors");

        // an array is returned as an ArrayList from SolrInputField#getValues()
        assertTrue("array input is expected to be returned as array list", objects instanceof ArrayList<?>);
        assertTrue(objects.contains("foo"));
        assertTrue(objects.contains("bar"));

        // because :
        // @IndexField
        // public LinkedList<String> getAuthorsAsLinkdedList() {
        //     LinkedList<String> list = new LinkedList<String>();
        //     list.addAll(Arrays.asList(getAuthors()));
        //     return list;
        // }

        Collection<Object> linkedList = doc.getFieldValues("authorsAsLinkdedList");
        assertTrue("array input is expected to be returned as LinkedList", linkedList instanceof LinkedList<?>);
        assertTrue(linkedList.contains("foo"));
        assertTrue(linkedList.contains("bar"));

        // getTitleAsWell is indexed as 'titleAgain'
        // @IndexField(name="titleAgain")
        // public String getTitleAsWell() {
        //     return titleAsWell;
        // }
        assertTrue(doc.getFieldValue("titleAgain").equals("titleValue"));

        // because :
        // @IndexField
        // public Calendar getCalendar() {
        //     return calendar;
        // }

        assertTrue(doc.getFieldValue("calendar").equals(dateAsString));

        // because :
        // @IndexField
        // public Date getDate() {
        //     return date;
        // }
        assertTrue(doc.getFieldValue("date").equals(dateAsString));

        // Long mileage, Double price, Boolean sold should also all be there :
        assertTrue(doc.getFieldValue("mileage").equals(mileage));
        assertTrue(doc.getFieldValue("price").equals(price));
        assertTrue(doc.getFieldValue("sold").equals(sold));

        // primitive values are auto-boxed to objects:
        // long primitiveMileage, double primitivePrice, primitiveSold ;
        assertTrue(doc.getFieldValue("primitiveMileage") instanceof  Long);
        assertTrue(doc.getFieldValue("primitivePrice") instanceof  Double);
        assertTrue(doc.getFieldValue("primitiveSold") instanceof  Boolean);

        assertTrue(doc.getFieldValue("primitiveMileage").equals(primitiveMileage));
        assertTrue(doc.getFieldValue("primitivePrice").equals(primitivePrice));
        assertTrue(doc.getFieldValue("primitiveSold").equals(primitiveSold));
    }

    @Test
    public void testPopulateContentBean() {
        DocumentObjectBinder binder = new DocumentObjectBinder();
        Calendar cal = Calendar.getInstance();
        Date date = cal.getTime();
        Long mileage = 100L;
        Double price = 25.25;
        Boolean sold = true;
        long primitiveMileage = 50;
        double primitivePrice = 50.50;
        boolean primitiveSold = true;

        TestContentBean myTestBean = new TestContentBean("my/simple/path", "titleValue", "summaryValue",
                new String[] {"foo", "bar"}, cal, date,
                mileage, price, sold, primitiveMileage, primitivePrice, primitiveSold );

        SolrInputDocument toSolrInputDoc = binder.toSolrInputDocument(myTestBean);

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

        TestContentBean bindedBean = binder.getBean(TestContentBean.class, solrDoc);

        // the path, title, titleAsWell and author should be repopulated, but 
        // NOT summary as there is not setter for summary
        assertEquals(bindedBean.getPath(), "my/simple/path");
        assertEquals(bindedBean.getTitle(), "titleValue");
        assertTrue(bindedBean.getAuthors().length == 2);
        assertTrue(bindedBean.getAuthors()[0].equals("foo") || bindedBean.getAuthors()[0].equals("bar"));
        assertTrue(bindedBean.getAuthors()[1].equals("foo") || bindedBean.getAuthors()[1].equals("bar"));

        assertTrue(bindedBean.getAuthorsAsLinkdedList().size() == 2);
        assertTrue(bindedBean.getAuthorsAsLinkdedList().contains("foo"));
        assertTrue(bindedBean.getAuthorsAsLinkdedList().contains("bar"));

        assertTrue(bindedBean.getDate().equals(date));
        assertTrue(bindedBean.getCalendar().equals(cal));

        assertNull(bindedBean.getSummary());


        assertTrue(bindedBean.getMileage().equals(mileage));
        assertTrue(bindedBean.getPrice().equals(price));
        assertTrue(bindedBean.getSold().equals(sold));
        assertTrue(bindedBean.getPrimitiveMileage() == primitiveMileage);
        assertTrue(bindedBean.getPrimitivePrice() == primitivePrice);
        assertTrue(bindedBean.isPrimitiveSold() == primitiveSold);
    }



}
