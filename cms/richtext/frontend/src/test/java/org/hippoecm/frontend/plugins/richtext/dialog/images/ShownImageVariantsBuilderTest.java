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
package org.hippoecm.frontend.plugins.richtext.dialog.images;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

public class ShownImageVariantsBuilderTest {

    @Test
    public void getShownImageVariants_IncludeNotExists_ExcludedEmpty_returnAll(){
        ShownImageVariantsBuilder whiteBlackListResolver = new ShownImageVariantsBuilder();
        whiteBlackListResolver.setAllImageVariants(Arrays.asList("A", "B", "C", "D"));
        whiteBlackListResolver.setExcludedImageVariants(new ArrayList<String>());
        whiteBlackListResolver.setIncludedImageVariants(null);
        whiteBlackListResolver.build();
        final List<String> actual = whiteBlackListResolver.getShownImageVariants();
        Assert.assertArrayEquals(Arrays.asList("A", "B", "C", "D").toArray(), actual.toArray());
    }

    @Test
    public void getShownImageVariants_IncludedFilled_ExcludedEmpty_returnIntersectionIncludedVariantsAll(){
        ShownImageVariantsBuilder whiteBlackListResolver = new ShownImageVariantsBuilder();
        whiteBlackListResolver.setAllImageVariants(Arrays.asList("A", "B", "C", "D"));
        whiteBlackListResolver.setExcludedImageVariants(new ArrayList<String>());
        whiteBlackListResolver.setIncludedImageVariants(Arrays.asList("A","E"));
        whiteBlackListResolver.build();
        final List<String> actual = whiteBlackListResolver.getShownImageVariants();
        Assert.assertArrayEquals(Arrays.asList("A").toArray(), actual.toArray());
    }

    @Test
    public void getShownImageVariants_IncludedFilled_ExcludedFilled_returnIntersectionIncludedShownMinusExcluded(){
        ShownImageVariantsBuilder whiteBlackListResolver = new ShownImageVariantsBuilder();
        whiteBlackListResolver.setAllImageVariants(Arrays.asList("A", "B", "C", "D"));
        whiteBlackListResolver.setExcludedImageVariants(Arrays.asList("A"));
        whiteBlackListResolver.setIncludedImageVariants(Arrays.asList("A","E"));
        whiteBlackListResolver.build();
        final List<String> actual = whiteBlackListResolver.getShownImageVariants();
        final List<String> expected = new ArrayList<String>();
        Assert.assertArrayEquals(expected.toArray(), actual.toArray());
    }



    @Test
    public void getShownImageVariants_IncludedFilledExcludedEmpty_returnIntersectionAllIncluded(){
        ShownImageVariantsBuilder whiteBlackListResolver = new ShownImageVariantsBuilder();
        whiteBlackListResolver.setAllImageVariants(Arrays.asList("A", "B", "C", "D"));
        whiteBlackListResolver.setExcludedImageVariants(new ArrayList<String>());
        whiteBlackListResolver.setIncludedImageVariants(Arrays.asList("A","B","D"));
        whiteBlackListResolver.build();
        final List<String> actual = whiteBlackListResolver.getShownImageVariants();
        Assert.assertArrayEquals(Arrays.asList("A", "B", "D").toArray(), actual.toArray());
    }

    @Test
    public void getShownImageVariants_IncludedNotExistsExcludedFilled_returnAllMinusExcluded(){
        ShownImageVariantsBuilder whiteBlackListResolver = new ShownImageVariantsBuilder();
        List<String> allImageVariants = Arrays.asList("A", "B", "C", "D");
        List<String> includedImageVariants = null;
        List<String> excludedImageVariants = Arrays.asList("C");
        whiteBlackListResolver.setAllImageVariants(allImageVariants);
        whiteBlackListResolver.setExcludedImageVariants(excludedImageVariants);
        whiteBlackListResolver.setIncludedImageVariants(includedImageVariants);
        whiteBlackListResolver.build();
        final List<String> actual = whiteBlackListResolver.getShownImageVariants();
        final List<String> expected = Arrays.asList("A","B","D");
        Assert.assertArrayEquals(expected.toArray(), actual.toArray());
    }
}
