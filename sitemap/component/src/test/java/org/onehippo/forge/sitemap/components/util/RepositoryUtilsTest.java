/*
 * Copyright 2012-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.forge.sitemap.components.util;

import junit.framework.Assert;
import org.hippoecm.repository.api.StringCodecFactory;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.onehippo.forge.sitemap.components.util.RepositoryUtils.encodePath;
import static org.onehippo.forge.sitemap.components.util.RepositoryUtils.getIndexedNodeNames;
import static org.onehippo.forge.sitemap.components.util.RepositoryUtils.indexedNodesInPathBMatchIndexedNodesInPathAWhenPathAHasThatNode;
import static org.onehippo.forge.sitemap.components.util.RepositoryUtils.localizePath;

public class RepositoryUtilsTest {
    @Test
    public void testLocalizePath() throws Exception {
        String basePath = "/content/documents";
        String expected = "news/2012/01/02/happy-news";
        String pathToLocalize = basePath + "/" + expected;

        Assert.assertEquals(expected, localizePath(basePath, pathToLocalize));
    }

    @Test
    public void testEncodePathWithNodeStartingWithNumber() throws Exception {
        String encoded2006 = StringCodecFactory.ISO9075Helper.encodeLocalName("2006");
        String expected = "/a/" + encoded2006 + "/asdfsda/s";
        String input = "/a/2006/asdfsda/s";

        String result = encodePath(input);
        Assert.assertEquals(expected, result);
    }

    @Test
    public void testEncodePathDoesntEncodeStars() throws Exception {
        String input = "*";
        String expected = "*";
        String result = encodePath(input);

        Assert.assertEquals(expected, result);
    }

    @Test
    public void testEncodeDoubleSlashDoesntEatDoubleSlash() throws Exception {
        String input = "//content/documents//news//";
        String expected = "//content/documents//news//";
        String result = encodePath(input);

        Assert.assertEquals(expected, result);
    }

    @Test
    public void testGetIndexedNodeNames() throws Exception {
        String input = "//blah[2]/test/documents/news/january-and-february[1]";
        List<String> expected = Arrays.asList("blah", "january-and-february");
        List<String> result = getIndexedNodeNames(input);

        Assert.assertEquals(expected, result);
    }

    @Test
    public void testIndexedNodesInPathsMatch() throws Exception {
        String pathA = "//blah[2]/test/documents/news/january-and-february[1]";
        String pathB = "//blah[2]/test/documents/news/january-and-february[1]";
        boolean result = indexedNodesInPathBMatchIndexedNodesInPathAWhenPathAHasThatNode(pathA, pathB);

        Assert.assertEquals(true, result);
    }

    @Test
    public void testIndexedNodesInPathsMatchDontMatch() throws Exception {
        String pathA = "//blah[2]/test/documents/news/january-and-february";
        String pathB = "//blah[2]/test/documents/news/january-and-february[1]";
        boolean result = indexedNodesInPathBMatchIndexedNodesInPathAWhenPathAHasThatNode(pathA, pathB);

        Assert.assertEquals(false, result);
    }
}
