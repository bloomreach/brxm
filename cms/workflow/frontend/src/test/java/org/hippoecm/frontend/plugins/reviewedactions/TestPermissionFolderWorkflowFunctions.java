/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
