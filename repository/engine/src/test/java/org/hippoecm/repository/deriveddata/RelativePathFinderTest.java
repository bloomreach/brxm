/*
 * Copyright 2018 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *      http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.hippoecm.repository.deriveddata;

import org.junit.Assert;
import org.junit.Test;

public class RelativePathFinderTest {

    @Test
    public void getRelativePath() {
        RelativePathFinder finder = new RelativePathFinder("/derived", "/derived/subnode/test");
        Assert.assertEquals("subnode/test", finder.getRelativePath());
    }

    @Test
    public void getRelativePath_subNodeWithSameName() {
        RelativePathFinder finder = new RelativePathFinder("/derived", "/derived/derived/test");
        Assert.assertEquals("derived/test", finder.getRelativePath());
    }

    @Test(expected = IllegalArgumentException.class)
    public void getRelativePath_propertyPathDoesNotStartWithNodePath() {
        RelativePathFinder finder = new RelativePathFinder("/derive", "/derived/derived/test");
        Assert.assertEquals("derived/test", finder.getRelativePath());
    }

    @Test
    public void getRelativePath_sameNameSiblingPropertyWithCounter() {
        RelativePathFinder finder = new RelativePathFinder(" /test/document/document"
                , " /test/document/document[3]/hippostdpubwf:lastModificationDate");
        Assert.assertEquals("hippostdpubwf:lastModificationDate", finder.getRelativePath());
    }

    @Test
    public void getRelativePath_sameNameSiblingNodeWithCounter() {
        RelativePathFinder finder = new RelativePathFinder(" /test/document/document[1]"
                , " /test/document/document/hippostdpubwf:lastModificationDate");
        Assert.assertEquals("hippostdpubwf:lastModificationDate", finder.getRelativePath());
    }

    @Test
    public void getRelativePath_sameNameSiblingWithCounters() {
        RelativePathFinder finder = new RelativePathFinder(" /test/document/document[1]"
                , " /test/document/document[2]/hippostdpubwf:lastModificationDate");
        Assert.assertEquals("hippostdpubwf:lastModificationDate", finder.getRelativePath());
    }

}