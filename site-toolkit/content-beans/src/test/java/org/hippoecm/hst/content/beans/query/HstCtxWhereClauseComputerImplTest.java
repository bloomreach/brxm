package org.hippoecm.hst.content.beans.query;

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
        assertEquals(0, m2.compareTo(m1));

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

}
