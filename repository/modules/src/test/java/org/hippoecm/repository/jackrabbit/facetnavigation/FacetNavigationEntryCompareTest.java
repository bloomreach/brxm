/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.repository.jackrabbit.facetnavigation;


import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

import javax.jcr.RepositoryException;

import org.hippoecm.repository.FacetedNavigationEngine.Count;
import org.hippoecm.repository.jackrabbit.facetnavigation.AbstractFacetNavigationProvider.FacetNavigationEntry;
import org.junit.Test;

public class FacetNavigationEntryCompareTest {

    @Test
    public void testFacetSearchEntryCompareAndEqualsAndHashCode() throws RepositoryException {
        FacetNavigationProvider fsp = new FacetNavigationProvider();
        int total = 10000;
        FacetNavigationEntry[] fseArr = createFacetNavigationEntries(fsp, total);
        Arrays.sort(fseArr);
        FacetNavigationEntry prev = null;
        for(FacetNavigationEntry e : fseArr) {
            if (prev != null) {
                assertTrue("Sorting of comparable FacetSearchEntry does not work correct", e.count.count <= prev.count.count);
                if(e.compareTo(prev) == 0) {
                    // now assert equals is true, otherwise we do not follow this strong suggested rule for FacetSearchEntry : From Comparable interface:
                    /* <p>It is strongly recommended, but <i>not</i> strictly required that
                    * <tt>(x.compareTo(y)==0) == (x.equals(y))</tt>.  Generally speaking, any
                    * class that implements the <tt>Comparable</tt> interface and violates
                    * this condition should clearly indicate this fact.  The recommended
                    * language is "Note: this class has a natural ordering that is
                    * inconsistent with equals."
                    * */
                    assertTrue("Equals is not consistent with compareTo contract",e.equals(prev) && prev.equals(e));
                    assertTrue("Hashes must be the same when comparable returns 0", e.hashCode() == prev.hashCode());
                } else {
                    assertFalse("Equals is not consistent with compareTo contract",e.equals(prev) || prev.equals(e));
                    // hashCode is allowed to be the same for unequal objects
                }
            }
            prev = e;
        }
        
    }
   
    @Test
    public void testFacetSearchEntryInMaps() throws RepositoryException {
        int total = 10000;
        FacetNavigationProvider fsp = new FacetNavigationProvider();
        FacetNavigationEntry[] fseArr = createFacetNavigationEntries(fsp, total);
    
        List<FacetNavigationEntry> list = Arrays.asList(fseArr);
        assertTrue("List should contain "+total+" entries", list.size() == total);
        
        Set<FacetNavigationEntry> set = new HashSet<FacetNavigationEntry>(list);
        TreeSet<FacetNavigationEntry> treeSet = new TreeSet<FacetNavigationEntry>(list);

        // if comparaTo and equals do not behave correctly, HashSet and TreeSet will behave differently 
        assertTrue("HashSet and TreeSet should contain equal number of items", set.size() == treeSet.size());
        
    }
    
   // creates a random set of FacetSearchEntry where there are some with equal facetValue and count
    private FacetNavigationEntry[] createFacetNavigationEntries(FacetNavigationProvider fsp, int total) {  
        int maxCount = 9;
        Random rand = new Random(System.currentTimeMillis());
        String[] facetValues = {"val1","val2","val3","val4","val5","val6","val7","val8","val9","val10"};
        FacetNavigationEntry[] fseArr = new FacetNavigationEntry[total];
        for (int j = 0; j < total; j++) {
            int randCount = rand.nextInt(maxCount + 1);
            String randValue = facetValues[rand.nextInt(facetValues.length)];
            fseArr[j] = fsp.new FacetNavigationEntry(randValue, new Count(randCount));
        }
        return fseArr;
    }
    
    
    
    
}
