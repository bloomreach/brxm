/*
 *  Copyright 2010-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.repository.facetnavigation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.jackrabbit.facetnavigation.FacNavNodeType;
import org.junit.Test;
public class FacetedNavigationFilteredTest extends AbstractDateFacetNavigationTest {
      
    
    @Test
    public void testEqualsFilteredNavigation() throws RepositoryException, IOException {
        commonStart();

        Node testNode = session.getRootNode().getNode("test");
        createDateStructure(testNode);

        Node facetNavigation = testNode.addNode("facetnavigation");
        facetNavigation = facetNavigation.addNode("hippo:navigation", FacNavNodeType.NT_FACETNAVIGATION);
        facetNavigation.setProperty(HippoNodeType.HIPPO_DOCBASE, session.getRootNode().getNode("test/documents")
                .getIdentifier());
        facetNavigation.setProperty(FacNavNodeType.HIPPOFACNAV_FACETS, new String[] { "hippo:date$year",  "hippo:date$month"});
        facetNavigation.setProperty(FacNavNodeType.HIPPOFACNAV_FACETNODENAMES, new String[] {"year","month"});
        facetNavigation.setProperty(FacNavNodeType.HIPPOFACNAV_FILTERS, new String[] {"hippo:brand=peugeot"});

        session.save();
        facetNavigation = session.getRootNode().getNode("test/facetnavigation/hippo:navigation");
        
        // there are five peugeot cars (thus hippo:brand=peugeot)
        assertEquals(5L, facetNavigation.getNode("year").getProperty(HippoNodeType.HIPPO_COUNT).getLong());
        assertEquals(5L, facetNavigation.getNode("year").getNode("hippo:resultset").getProperty(HippoNodeType.HIPPO_COUNT).getLong());
        
        
        // now test for another equal: three of the five peugeot cars are red
        facetNavigation.setProperty(FacNavNodeType.HIPPOFACNAV_FILTERS, new String[] {"hippo:brand=peugeot", "hippo:color=red"});
        session.save();
        facetNavigation = session.getRootNode().getNode("test/facetnavigation/hippo:navigation");
       
        // there are three  peugeot cars (thus hippo:brand=peugeot AND hippo:color=red)
        assertEquals(3L, facetNavigation.getNode("year").getProperty(HippoNodeType.HIPPO_COUNT).getLong());
        assertEquals(3L, facetNavigation.getNode("year").getNode("hippo:resultset").getProperty(HippoNodeType.HIPPO_COUNT).getLong());
        
        // test for not equal (!=) we have 4 cars that are not a peugeot
        facetNavigation.setProperty(FacNavNodeType.HIPPOFACNAV_FILTERS, new String[] {"hippo:brand!=peugeot" });
        session.save();
        facetNavigation = session.getRootNode().getNode("test/facetnavigation/hippo:navigation");
        // there are four cars that are not a peugeot (thus hippo:brand!=peugeot)
        assertEquals(4L, facetNavigation.getNode("year").getProperty(HippoNodeType.HIPPO_COUNT).getLong());
        assertEquals(4L, facetNavigation.getNode("year").getNode("hippo:resultset").getProperty(HippoNodeType.HIPPO_COUNT).getLong());
        
    }
    
    @Test
    public void testNotEqualsFilteredNavigation() throws RepositoryException, IOException {
        commonStart();

        Node testNode = session.getRootNode().getNode("test");
        createDateStructure(testNode);

        Node facetNavigation = testNode.addNode("facetnavigation");
        facetNavigation = facetNavigation.addNode("hippo:navigation", FacNavNodeType.NT_FACETNAVIGATION);
        facetNavigation.setProperty(HippoNodeType.HIPPO_DOCBASE, session.getRootNode().getNode("test/documents")
                .getIdentifier());
        facetNavigation.setProperty(FacNavNodeType.HIPPOFACNAV_FACETS, new String[] { "hippo:date$year",  "hippo:date$month"});
        facetNavigation.setProperty(FacNavNodeType.HIPPOFACNAV_FACETNODENAMES, new String[] {"year","month"});
        facetNavigation.setProperty(FacNavNodeType.HIPPOFACNAV_FILTERS, new String[] {"hippo:brand != peugeot"});

        session.save();
        facetNavigation = session.getRootNode().getNode("test/facetnavigation/hippo:navigation");
        
        // there are four cars that are not a peugeot (thus hippo:brand!=peugeot)
        assertEquals(4L, facetNavigation.getNode("year").getProperty(HippoNodeType.HIPPO_COUNT).getLong());
        assertEquals(4L, facetNavigation.getNode("year").getNode("hippo:resultset").getProperty(HippoNodeType.HIPPO_COUNT).getLong());
        
        
        facetNavigation.setProperty(FacNavNodeType.HIPPOFACNAV_FILTERS, new String[] {"hippo:brand != peugeot", "hippo:color != red"});
        session.save();
        facetNavigation = session.getRootNode().getNode("test/facetnavigation/hippo:navigation");
       
        // there are two  car not a peugeot AND not red (thus hippo:brand != peugeot AND hippo:color != red)
        assertEquals(2L, facetNavigation.getNode("year").getProperty(HippoNodeType.HIPPO_COUNT).getLong());
        assertEquals(2L, facetNavigation.getNode("year").getNode("hippo:resultset").getProperty(HippoNodeType.HIPPO_COUNT).getLong());
        
        // a different way to do not equal:
        
        facetNavigation.setProperty(FacNavNodeType.HIPPOFACNAV_FILTERS, new String[] {"not(hippo:brand = peugeot)", "not(hippo:color = red)"});
        session.save();
        facetNavigation = session.getRootNode().getNode("test/facetnavigation/hippo:navigation");
       
        // there are two  car not a peugeot AND not red (thus hippo:brand != peugeot AND hippo:color != red)
        assertEquals(2L, facetNavigation.getNode("year").getProperty(HippoNodeType.HIPPO_COUNT).getLong());
        assertEquals(2L, facetNavigation.getNode("year").getNode("hippo:resultset").getProperty(HippoNodeType.HIPPO_COUNT).getLong());
    }
    
    
    @Test
    public void testEqualsPrimTypeFilteredNavigation() throws RepositoryException, IOException {
        commonStart();

        Node testNode = session.getRootNode().getNode("test");
        createDateStructure(testNode);

        Node facetNavigation = testNode.addNode("facetnavigation");
        facetNavigation = facetNavigation.addNode("hippo:navigation", FacNavNodeType.NT_FACETNAVIGATION);
        facetNavigation.setProperty(HippoNodeType.HIPPO_DOCBASE, session.getRootNode().getNode("test/documents")
                .getIdentifier());
        facetNavigation.setProperty(FacNavNodeType.HIPPOFACNAV_FACETS, new String[] { "hippo:date$year",  "hippo:date$month", "jcr:primaryType"});
        facetNavigation.setProperty(FacNavNodeType.HIPPOFACNAV_FACETNODENAMES, new String[] {"year","month", "prim"});
        facetNavigation.setProperty(FacNavNodeType.HIPPOFACNAV_FILTERS, new String[] {"jcr:primaryType = hippo:testcardocument"});

        
        session.save();
        facetNavigation = session.getRootNode().getNode("test/facetnavigation/hippo:navigation");
      
        // all cars are of type hippo:testcardocument, thus we expect to find 9
        assertEquals(9L, facetNavigation.getNode("year").getProperty(HippoNodeType.HIPPO_COUNT).getLong());
        assertEquals(9L, facetNavigation.getNode("year").getNode("hippo:resultset").getProperty(HippoNodeType.HIPPO_COUNT).getLong());
        
        // change to non existing document type:
        
        facetNavigation.setProperty(FacNavNodeType.HIPPOFACNAV_FILTERS, new String[] {"jcr:primaryType = hippo:nonexisting"});

        session.save();
        facetNavigation = session.getRootNode().getNode("test/facetnavigation/hippo:navigation");
        
        assertEquals(0L, facetNavigation.getNode("year").getProperty(HippoNodeType.HIPPO_COUNT).getLong());
        assertEquals(0L, facetNavigation.getNode("year").getNode("hippo:resultset").getProperty(HippoNodeType.HIPPO_COUNT).getLong());
    }
    
    
   /*
    * This test is to make sure filtering of the faceted navigation works as expected
    */
   @Test
   public void testTextContainsNavigation() throws RepositoryException, IOException {
       
       commonStart();

       Node testNode = session.getRootNode().getNode("test");
       createDateStructure(testNode);

       Node facetNavigation = testNode.addNode("facetnavigation");
       facetNavigation = facetNavigation.addNode("hippo:navigation", FacNavNodeType.NT_FACETNAVIGATION);
       facetNavigation.setProperty(HippoNodeType.HIPPO_DOCBASE, session.getRootNode().getNode("test/documents")
               .getIdentifier());
       facetNavigation.setProperty(FacNavNodeType.HIPPOFACNAV_FACETS, new String[] { "hippo:date$year",  "hippo:date$month"});
       facetNavigation.setProperty(FacNavNodeType.HIPPOFACNAV_FACETNODENAMES, new String[] {"year","month"});
       facetNavigation.setProperty(FacNavNodeType.HIPPOFACNAV_FILTERS, new String[] {"hippo:brand=peugeot", "contains(.,jumps)" });

       session.save();
       
       facetNavigation = session.getRootNode().getNode("test/facetnavigation/hippo:navigation");
       
       // there are three peugeot cars which contain 'jumps' in a child node
       assertEquals(3L, facetNavigation.getNode("year").getProperty(HippoNodeType.HIPPO_COUNT).getLong());
       //assertEquals(3L, navigation.getNode("year").getNode("hippo:resultset").getProperty(HippoNodeType.HIPPO_COUNT).getLong());
       
       // change the filter:
       facetNavigation.setProperty(FacNavNodeType.HIPPOFACNAV_FILTERS, new String[] {"hippo:brand=peugeot", "contains(.,quick)" });
       session.save();
       facetNavigation = session.getRootNode().getNode("test/facetnavigation/hippo:navigation");
       
       // we only have one peugeot which contains 'quick' in a child node
       assertEquals(1L, facetNavigation.getNode("year").getProperty(HippoNodeType.HIPPO_COUNT).getLong());
       assertEquals(1L, facetNavigation.getNode("year").getNode("hippo:resultset").getProperty(HippoNodeType.HIPPO_COUNT).getLong());
      
       // change the filter:
       facetNavigation.setProperty(FacNavNodeType.HIPPOFACNAV_FILTERS, new String[] {"hippo:brand=peugeot", "contains(.,quick brown)" });
       session.save();
       facetNavigation = session.getRootNode().getNode("test/facetnavigation/hippo:navigation");
       // we have one car with 'brown' and one car with 'quick brown' : default operator of space is AND
       assertEquals(1L, facetNavigation.getNode("year").getProperty(HippoNodeType.HIPPO_COUNT).getLong());
       assertEquals(1L, facetNavigation.getNode("year").getNode("hippo:resultset").getProperty(HippoNodeType.HIPPO_COUNT).getLong());
       
       
       // change the filter:
       facetNavigation.setProperty(FacNavNodeType.HIPPOFACNAV_FILTERS, new String[] {"hippo:brand=peugeot", "contains(.,quick OR brown)" });
       session.save();
       facetNavigation = session.getRootNode().getNode("test/facetnavigation/hippo:navigation");
       // we have one car with 'brown' and one car with 'quick brown' : since operator is OR, we expect 2 cars
       assertEquals(2L, facetNavigation.getNode("year").getProperty(HippoNodeType.HIPPO_COUNT).getLong());
       assertEquals(2L, facetNavigation.getNode("year").getNode("hippo:resultset").getProperty(HippoNodeType.HIPPO_COUNT).getLong());
       
   }
   
   /*
    * test for not contains
    */
   @Test
   public void testTextNotContainsNavigation() throws RepositoryException, IOException {
       commonStart();

       Node testNode = session.getRootNode().getNode("test");
       createDateStructure(testNode);

       Node facetNavigation = testNode.addNode("facetnavigation");
       facetNavigation = facetNavigation.addNode("hippo:navigation", FacNavNodeType.NT_FACETNAVIGATION);
       facetNavigation.setProperty(HippoNodeType.HIPPO_DOCBASE, session.getRootNode().getNode("test/documents")
               .getIdentifier());
       facetNavigation.setProperty(FacNavNodeType.HIPPOFACNAV_FACETS, new String[] { "hippo:date$year",  "hippo:date$month"});
       facetNavigation.setProperty(FacNavNodeType.HIPPOFACNAV_FACETNODENAMES, new String[] {"year","month"});
       facetNavigation.setProperty(FacNavNodeType.HIPPOFACNAV_FILTERS, new String[] {"hippo:brand=peugeot", "not(contains(.,jumps))" });

       session.save();
       
       facetNavigation = session.getRootNode().getNode("test/facetnavigation/hippo:navigation");
       
       // there are tow peugeot cars which do NOT contain 'jumps' in a child node
       assertEquals(2L, facetNavigation.getNode("year").getProperty(HippoNodeType.HIPPO_COUNT).getLong());
       assertEquals(2L, facetNavigation.getNode("year").getNode("hippo:resultset").getProperty(HippoNodeType.HIPPO_COUNT).getLong());
   }
   
   /*
    * As prefix wildcards blow up in inverted indexes such as Lucene, we do not support them
    */
   @Test
   public void testNotAllowedWildcardPrefixNavigation() throws RepositoryException, IOException {
       commonStart();

       Node testNode = session.getRootNode().getNode("test");
       createDateStructure(testNode);

       Node facetNavigation = testNode.addNode("facetnavigation");
       facetNavigation = facetNavigation.addNode("hippo:navigation", FacNavNodeType.NT_FACETNAVIGATION);
       facetNavigation.setProperty(HippoNodeType.HIPPO_DOCBASE, session.getRootNode().getNode("test/documents")
               .getIdentifier());
       facetNavigation.setProperty(FacNavNodeType.HIPPOFACNAV_FACETS, new String[] { "hippo:date$year",  "hippo:date$month"});
       facetNavigation.setProperty(FacNavNodeType.HIPPOFACNAV_FACETNODENAMES, new String[] {"year","month"});
       facetNavigation.setProperty(FacNavNodeType.HIPPOFACNAV_FILTERS, new String[] {"hippo:brand=peugeot", "contains(.,*umps)" });

       session.save();
       
       facetNavigation = session.getRootNode().getNode("test/facetnavigation/hippo:navigation");
       // we have an unsupported prefix wildcard
       assertFalse(facetNavigation.hasNodes());
   }
   
   /*
    * This test is to make sure filtering of the faceted navigation works as expected
    */
   @Test
   public void testPropertyTextContainsNavigation() throws RepositoryException, IOException {
       commonStart();

       Node testNode = session.getRootNode().getNode("test");
       createDateStructure(testNode);

       Node facetNavigation = testNode.addNode("facetnavigation");
       facetNavigation = facetNavigation.addNode("hippo:navigation", FacNavNodeType.NT_FACETNAVIGATION);
       facetNavigation.setProperty(HippoNodeType.HIPPO_DOCBASE, session.getRootNode().getNode("test/documents")
               .getIdentifier());
       facetNavigation.setProperty(FacNavNodeType.HIPPOFACNAV_FACETS, new String[] { "hippo:date$year",  "hippo:date$month"});
       facetNavigation.setProperty(FacNavNodeType.HIPPOFACNAV_FACETNODENAMES, new String[] {"year","month"});
       facetNavigation.setProperty(FacNavNodeType.HIPPOFACNAV_FILTERS, new String[] {"contains(hippo:brand,peugeot)" });
       
       session.save();
       facetNavigation = session.getRootNode().getNode("test/facetnavigation/hippo:navigation");
       
       // there are 5 cars where hippo:brand = peugeot
       assertEquals(5L, facetNavigation.getNode("year").getProperty(HippoNodeType.HIPPO_COUNT).getLong());
       
       
       facetNavigation.setProperty(FacNavNodeType.HIPPOFACNAV_FILTERS, new String[] {"contains(hippo:brand,peugeot mercedes)" });
       session.save();
       facetNavigation = session.getRootNode().getNode("test/facetnavigation/hippo:navigation");
       // default operator for a space is AND, and there are no cars that are peugeot AND mercedes, hence we expect 0
       assertEquals(0L, facetNavigation.getNode("year").getProperty(HippoNodeType.HIPPO_COUNT).getLong());
       assertEquals(0L, facetNavigation.getNode("year").getNode("hippo:resultset").getProperty(HippoNodeType.HIPPO_COUNT).getLong());
       
       facetNavigation.setProperty(FacNavNodeType.HIPPOFACNAV_FILTERS, new String[] {"contains(hippo:brand,peugeot OR mercedes)" });
       session.save();
       facetNavigation = session.getRootNode().getNode("test/facetnavigation/hippo:navigation");
       // there are 5 cars where hippo:brand = peugeot and 2 cars have hippo:brand = mercedes, hence we expect 7 cars now
       assertEquals(7L, facetNavigation.getNode("year").getProperty(HippoNodeType.HIPPO_COUNT).getLong());
       assertEquals(7L, facetNavigation.getNode("year").getNode("hippo:resultset").getProperty(HippoNodeType.HIPPO_COUNT).getLong());
       
       
       facetNavigation.setProperty(FacNavNodeType.HIPPOFACNAV_FILTERS, new String[] {"not(contains(hippo:brand,peugeot OR mercedes))" });
       session.save();
       facetNavigation = session.getRootNode().getNode("test/facetnavigation/hippo:navigation");
       // there are 5 cars where hippo:brand = peugeot and 2 cars have hippo:brand = mercedes, hence we expect the 2 cars that are of type bmw
       assertEquals(2L, facetNavigation.getNode("year").getProperty(HippoNodeType.HIPPO_COUNT).getLong());
       assertEquals(2L, facetNavigation.getNode("year").getNode("hippo:resultset").getProperty(HippoNodeType.HIPPO_COUNT).getLong());
   }
   
   /*
    * This test is to make sure filtering of the faceted navigation works as expected
    */
   @Test
   public void testPhraseTextNavigation() throws RepositoryException, IOException {
       commonStart();

       Node testNode = session.getRootNode().getNode("test");
       createDateStructure(testNode);

       Node facetNavigation = testNode.addNode("facetnavigation");
       facetNavigation = facetNavigation.addNode("hippo:navigation", FacNavNodeType.NT_FACETNAVIGATION);
       facetNavigation.setProperty(HippoNodeType.HIPPO_DOCBASE, session.getRootNode().getNode("test/documents")
               .getIdentifier());
       facetNavigation.setProperty(FacNavNodeType.HIPPOFACNAV_FACETS, new String[] { "hippo:date$year",  "hippo:date$month"});
       facetNavigation.setProperty(FacNavNodeType.HIPPOFACNAV_FACETNODENAMES, new String[] {"year","month"});
       
       // a phrase query: only documents having this exact ordering of words should have a hit
       facetNavigation.setProperty(FacNavNodeType.HIPPOFACNAV_FILTERS, new String[] {"hippo:brand=peugeot", "contains(.,\"brown fox jumps\")" });

       session.save();
       facetNavigation = session.getRootNode().getNode("test/facetnavigation/hippo:navigation");
       
       // there are two peugeot cars which contain the phrase 'brown fox jumps'
       assertEquals(2L, facetNavigation.getNode("year").getProperty(HippoNodeType.HIPPO_COUNT).getLong());
       assertEquals(2L, facetNavigation.getNode("year").getNode("hippo:resultset").getProperty(HippoNodeType.HIPPO_COUNT).getLong());
       
       // change the filter to a phrase the none of the documents has:
       facetNavigation.setProperty(FacNavNodeType.HIPPOFACNAV_FILTERS, new String[] {"hippo:brand=peugeot", "contains(.,\"brown jumps\")" });
       session.save();
       facetNavigation = session.getRootNode().getNode("test/facetnavigation/hippo:navigation");
       
       // there are 0 peugeot cars which contain the phrase 'brown jumps' (they contains 'brown fox jumps')
       assertEquals(0L, facetNavigation.getNode("year").getProperty(HippoNodeType.HIPPO_COUNT).getLong());
       assertEquals(0L, facetNavigation.getNode("year").getNode("hippo:resultset").getProperty(HippoNodeType.HIPPO_COUNT).getLong());
       
   }
   
   @Test
   public void testWildcardTextNavigation() throws RepositoryException, IOException {
       commonStart();

       Node testNode = session.getRootNode().getNode("test");
       createDateStructure(testNode);

       Node facetNavigation = testNode.addNode("facetnavigation");
       facetNavigation = facetNavigation.addNode("hippo:navigation", FacNavNodeType.NT_FACETNAVIGATION);
       facetNavigation.setProperty(HippoNodeType.HIPPO_DOCBASE, session.getRootNode().getNode("test/documents")
               .getIdentifier());
       facetNavigation.setProperty(FacNavNodeType.HIPPOFACNAV_FACETS, new String[] { "hippo:date$year",  "hippo:date$month"});
       facetNavigation.setProperty(FacNavNodeType.HIPPOFACNAV_FACETNODENAMES, new String[] {"year","month"});
       
       // a phrase query: only documents having this exact ordering of words should have a hit
       facetNavigation.setProperty(FacNavNodeType.HIPPOFACNAV_FILTERS, new String[] {"hippo:brand=peugeot", "contains(.,bro?n)" });

       session.save();
       facetNavigation = session.getRootNode().getNode("test/facetnavigation/hippo:navigation");
       
       // there are two peugeot cars which contain 'brown' and thus match bro?n
       assertEquals(2L, facetNavigation.getNode("year").getProperty(HippoNodeType.HIPPO_COUNT).getLong());
       assertEquals(2L, facetNavigation.getNode("year").getNode("hippo:resultset").getProperty(HippoNodeType.HIPPO_COUNT).getLong());
       //assertEquals(3L, navigation.getNode("year").getNode("hippo:resultset").getProperty(HippoNodeType.HIPPO_COUNT).getLong());
       
       // change the filter to a phrase the none of the documents has:
       facetNavigation.setProperty(FacNavNodeType.HIPPOFACNAV_FILTERS, new String[] {"hippo:brand=peugeot", "contains(., laz*)" });
       session.save();
       facetNavigation = session.getRootNode().getNode("test/facetnavigation/hippo:navigation");
       
       // there are 5 peugeot cars which  should match: 4 having lazy, 1 having laziest
       assertEquals(5L, facetNavigation.getNode("year").getProperty(HippoNodeType.HIPPO_COUNT).getLong());
       assertEquals(5L, facetNavigation.getNode("year").getNode("hippo:resultset").getProperty(HippoNodeType.HIPPO_COUNT).getLong());
   }
   
   /*
    * test that when the filter only contains text, this works the same as contains(.,some text)
    */
   @Test
   public void testNodeScopeFreeTextNavigation() throws RepositoryException, IOException {
       commonStart();

       Node testNode = session.getRootNode().getNode("test");
       createDateStructure(testNode);

       Node facetNav = testNode.addNode("facetnavigation");
       
       Node facetNavigation;
       Node facetNavigation2;
       
       facetNavigation =  facetNav.addNode("hippo:navigation", FacNavNodeType.NT_FACETNAVIGATION);
       facetNavigation2 = facetNav.addNode("hippo:navigation2", FacNavNodeType.NT_FACETNAVIGATION);
       
       facetNavigation.setProperty(HippoNodeType.HIPPO_DOCBASE, session.getRootNode().getNode("test/documents").getIdentifier());
       facetNavigation2.setProperty(HippoNodeType.HIPPO_DOCBASE, session.getRootNode().getNode("test/documents").getIdentifier());
       
       facetNavigation.setProperty(FacNavNodeType.HIPPOFACNAV_FACETS, new String[] { "hippo:date$year",  "hippo:date$month"});
       facetNavigation2.setProperty(FacNavNodeType.HIPPOFACNAV_FACETS, new String[] { "hippo:date$year",  "hippo:date$month"});

       facetNavigation.setProperty(FacNavNodeType.HIPPOFACNAV_FACETNODENAMES, new String[] {"year","month"});
       facetNavigation2.setProperty(FacNavNodeType.HIPPOFACNAV_FACETNODENAMES, new String[] {"year","month"});
       
       // we set facetNavigation with contains(.,text) and the second just with text. Now, we need to
       facetNavigation.setProperty(FacNavNodeType.HIPPOFACNAV_FILTERS, new String[] {"hippo:brand=peugeot", "contains(.,bro?n)" });
       facetNavigation2.setProperty(FacNavNodeType.HIPPOFACNAV_FILTERS, new String[] {"hippo:brand=peugeot", "bro?n" });
       
       session.save();
       facetNavigation = session.getRootNode().getNode("test/facetnavigation/hippo:navigation");
       facetNavigation2 = session.getRootNode().getNode("test/facetnavigation/hippo:navigation2");
       
       assertEquals(facetNavigation2.getNode("year").getProperty(HippoNodeType.HIPPO_COUNT).getLong(), facetNavigation.getNode("year").getProperty(HippoNodeType.HIPPO_COUNT).getLong());
       assertEquals(facetNavigation2.getNode("year").getNode("hippo:resultset").getProperty(HippoNodeType.HIPPO_COUNT).getLong(), facetNavigation.getNode("year").getNode("hippo:resultset").getProperty(HippoNodeType.HIPPO_COUNT).getLong());
       
     
   }
   
   @Test
   public void testBoostingTextNavigation() throws RepositoryException, IOException {
       commonStart();

       Node testNode = session.getRootNode().getNode("test");
       createDateStructure(testNode);

       Node facetNavigation = testNode.addNode("facetnavigation");
       facetNavigation = facetNavigation.addNode("hippo:navigation", FacNavNodeType.NT_FACETNAVIGATION);
       facetNavigation.setProperty(HippoNodeType.HIPPO_DOCBASE, session.getRootNode().getNode("test/documents")
               .getIdentifier());
       facetNavigation.setProperty(FacNavNodeType.HIPPOFACNAV_FACETS, new String[] { "hippo:date$year",  "hippo:date$month"});
       facetNavigation.setProperty(FacNavNodeType.HIPPOFACNAV_FACETNODENAMES, new String[] {"year","month"});
      
       facetNavigation.setProperty(FacNavNodeType.HIPPOFACNAV_FILTERS, new String[] {"contains(.,peugeot OR mercedes)" });
       session.save();
       
       facetNavigation = session.getRootNode().getNode("test/facetnavigation/hippo:navigation");
       // there are 5 cars where hippo:brand = peugeot and 2 cars have hippo:brand = mercedes, hence we expect 7 cars now
       assertEquals(7L, facetNavigation.getNode("year").getProperty(HippoNodeType.HIPPO_COUNT).getLong());
       assertEquals(7L, facetNavigation.getNode("year").getNode("hippo:resultset").getProperty(HippoNodeType.HIPPO_COUNT).getLong());
       
       // because we have only 2 mercedes's and 5 peugeot's, by defaults, a mercedes will have a higher lucene score. so we expect first
       // 2 mercedes's
       NodeIterator nodes = facetNavigation.getNode("year").getNode("hippo:resultset").getNodes();
       int i = 1;
       while(nodes.hasNext()) {
           Node n = nodes.nextNode();
           if(i == 1 || i == 2) {
               assertTrue("mercedes".equals(n.getProperty("hippo:brand").getString()));
           } else {
               assertTrue("peugeot".equals(n.getProperty("hippo:brand").getString()));
           }
           i++;
       }
       
       // now we boost the peugeot's to be more important:
       facetNavigation.setProperty(FacNavNodeType.HIPPOFACNAV_FILTERS, new String[] {"contains(.,peugeot^10 OR mercedes)" });
       session.save();
       
       facetNavigation = session.getRootNode().getNode("test/facetnavigation/hippo:navigation");
       
       // now we expect the peugeot to have a higher score:
       nodes = facetNavigation.getNode("year").getNode("hippo:resultset").getNodes();
       i = 1;
       while(nodes.hasNext()) {
           Node n = nodes.nextNode();
           // first 5 are peugeot now
           if(i == 1 || i == 2 || i == 3 || i == 4 || i == 5) {
               assertTrue("peugeot".equals(n.getProperty("hippo:brand").getString()));
           } else {
               assertTrue("mercedes".equals(n.getProperty("hippo:brand").getString()));
           }
           i++;
       }
       
    // and also show it works for searching in a property and not on '.'
       facetNavigation.setProperty(FacNavNodeType.HIPPOFACNAV_FILTERS, new String[] {"contains(hippo:brand,peugeot^10 OR mercedes)" });
       session.save();
       
       facetNavigation = session.getRootNode().getNode("test/facetnavigation/hippo:navigation");
       
       // now we expect the peugeot to have a higher score:
       nodes = facetNavigation.getNode("year").getNode("hippo:resultset").getNodes();
       i = 1;
       while(nodes.hasNext()) {
           Node n = nodes.nextNode();
           // first 5 are peugeot now
           if(i == 1 || i == 2 || i == 3 || i == 4 || i == 5) {
               assertTrue("peugeot".equals(n.getProperty("hippo:brand").getString()));
           } else {
               assertTrue("mercedes".equals(n.getProperty("hippo:brand").getString()));
           }
           i++;
       }
       
       // and also show it works for searching in a property and not on '.'
       facetNavigation.setProperty(FacNavNodeType.HIPPOFACNAV_FILTERS, new String[] {"contains(hippo:brand,peugeot OR mercedes^10)" });
       session.save();
       
       facetNavigation = session.getRootNode().getNode("test/facetnavigation/hippo:navigation");
       
       // now we expect the peugeot to have a higher score:
       nodes = facetNavigation.getNode("year").getNode("hippo:resultset").getNodes();
       i = 1;
       while(nodes.hasNext()) {
           Node n = nodes.nextNode();
           // first 5 are peugeot now
           if(i == 1 || i == 2) {
               assertTrue("mercedes".equals(n.getProperty("hippo:brand").getString()));
           } else {
               assertTrue("peugeot".equals(n.getProperty("hippo:brand").getString()));
           }
           i++;
       }
   }

}
