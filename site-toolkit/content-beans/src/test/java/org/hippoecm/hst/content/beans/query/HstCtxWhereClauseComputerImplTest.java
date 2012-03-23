package org.hippoecm.hst.content.beans.query;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

import org.junit.Test;

import static org.hippoecm.hst.content.beans.query.HstCtxWhereClauseComputerImpl.Mapper;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Tests {@link HstCtxWhereClauseComputerImpl}.
 */
public class HstCtxWhereClauseComputerImplTest {

    @Test
    public void mapperEquals() throws HstContextualizeException {
        Mapper m1 = new Mapper("a/b", "a/b");
        Mapper m2 = new Mapper("a/b", "a/b");
        assertTrue(m1.equals(m2));
        assertTrue(m2.equals(m1));

        Mapper m3 = new Mapper("a/b/c", "a/b/c");
        assertFalse(m1.equals(m3));
        assertFalse(m3.equals(m1));
    }

    @Test
    public void mapperCompareTo() throws HstContextualizeException {
        Mapper m1 = new Mapper("a/b", "a/b");
        Mapper m2 = new Mapper("a/b", "a/b");
        assertEquals(0, m1.compareTo(m1));
        assertEquals(0, m1.compareTo(m2));
        assertTrue(m1.equals(m2));
        assertEquals(0, m2.compareTo(m1));
        assertTrue(m2.equals(m1));

        Mapper m3 = new Mapper("a/b/c", "a/b/c");
        assertTrue(m2.compareTo(m3) > 0);
        assertTrue(m3.compareTo(m2) < 0);
        assertTrue(m2.compareTo(m3) == -(m3.compareTo(m2)));

        // test transitivity
        Mapper m4 = new Mapper("a/b/c/d", "a/b/c/d");
        assertTrue(m3.compareTo(m4) > 0);
        assertTrue(m2.compareTo(m4) > 0);
        assertTrue(m4.compareTo(m3) < 0);
        assertTrue(m4.compareTo(m2) < 0);
    }
    
    @Test
    public void mapperOrderingInTreeSet() throws HstContextualizeException {
        Mapper m1 = new Mapper("a/b", "a/b");
        Mapper m2 = new Mapper("a/b", "a/b");
        Mapper m3 = new Mapper("a/b/c", "a/b/c");
        Mapper m4 = new Mapper("a/b/c/d", "a/b/c/d");

        Set<Mapper> setMappers = new HashSet<Mapper>();
        setMappers.add(m1);
        setMappers.add(m2);
        setMappers.add(m3);
        setMappers.add(m4);


        Set<Mapper> treeSetMappers = new TreeSet<Mapper>();
        treeSetMappers.add(m1);
        treeSetMappers.add(m2);
        treeSetMappers.add(m3);
        treeSetMappers.add(m4);

        assertTrue(setMappers.size() == 3);
        assertTrue(treeSetMappers.size() == 3);
        Iterator<Mapper> iterator = treeSetMappers.iterator();
        // for the sorted tree set we expect first the mappers with the deepest path
        assert(iterator.next().length == 4);
        assert(iterator.next().length == 3);
        assert(iterator.next().length == 2);
    }

}
