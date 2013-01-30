package org.hippoecm.frontend.plugins.reviewedactions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertTrue;

/**
 * @version "$Id$"
 */
public class TestPermissionFolderWorkflowFunctions {

    private static Logger log = LoggerFactory.getLogger(TestPermissionFolderWorkflowFunctions.class);

    @Test
    public void testSwapping() throws Exception {

        List<String> list = new ArrayList<String>();

        list.add("one");
        list.add("two");
        list.add("three");

        int i = list.indexOf("two");
        Collections.swap(list, i, i-1);

        assertTrue(list.indexOf("two")==0);

        int j = list.indexOf("two");
        Collections.swap(list, j, j + 1);

        assertTrue(list.indexOf("two") == 1);


    }
}
