/*
 *  Copyright 2013-2022 Hippo B.V. (http://www.onehippo.com)
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
        ShownImageVariantsBuilder shownImageVariantsBuilder = new ShownImageVariantsBuilder();
        shownImageVariantsBuilder.setAllImageVariants(Arrays.asList("A", "B", "C", "D"));
        shownImageVariantsBuilder.setExcludedImageVariants(new ArrayList<String>());
        shownImageVariantsBuilder.setIncludedImageVariants(null);
        shownImageVariantsBuilder.build();
        final List<String> actual = shownImageVariantsBuilder.getShownImageVariants();
        Assert.assertArrayEquals(Arrays.asList("A", "B", "C", "D").toArray(), actual.toArray());
    }

    @Test
    public void getShownImageVariants_IncludedFilled_ExcludedEmpty_returnIntersectionIncludedVariantsAll(){
        ShownImageVariantsBuilder shownImageVariantsBuilder = new ShownImageVariantsBuilder();
        shownImageVariantsBuilder.setAllImageVariants(Arrays.asList("A", "B", "C", "D"));
        shownImageVariantsBuilder.setExcludedImageVariants(new ArrayList<String>());
        shownImageVariantsBuilder.setIncludedImageVariants(Arrays.asList("A","E"));
        shownImageVariantsBuilder.build();
        final List<String> actual = shownImageVariantsBuilder.getShownImageVariants();
        Assert.assertArrayEquals(Arrays.asList("A").toArray(), actual.toArray());
    }

    @Test
    public void getShownImageVariants_IncludedFilled_ExcludedFilled_returnIntersectionIncludedShownMinusExcluded(){
        ShownImageVariantsBuilder shownImageVariantsBuilder = new ShownImageVariantsBuilder();
        shownImageVariantsBuilder.setAllImageVariants(Arrays.asList("A", "B", "C", "D"));
        shownImageVariantsBuilder.setExcludedImageVariants(Arrays.asList("A"));
        shownImageVariantsBuilder.setIncludedImageVariants(Arrays.asList("A","E"));
        shownImageVariantsBuilder.build();
        final List<String> actual = shownImageVariantsBuilder.getShownImageVariants();
        final List<String> expected = new ArrayList<>();
        Assert.assertArrayEquals(expected.toArray(), actual.toArray());
    }



    @Test
    public void getShownImageVariants_IncludedFilledExcludedEmpty_returnIntersectionAllIncluded(){
        ShownImageVariantsBuilder shownImageVariantsBuilder = new ShownImageVariantsBuilder();
        shownImageVariantsBuilder.setAllImageVariants(Arrays.asList("A", "B", "C", "D"));
        shownImageVariantsBuilder.setExcludedImageVariants(new ArrayList<String>());
        shownImageVariantsBuilder.setIncludedImageVariants(Arrays.asList("A","B","D"));
        shownImageVariantsBuilder.build();
        final List<String> actual = shownImageVariantsBuilder.getShownImageVariants();
        Assert.assertArrayEquals(Arrays.asList("A", "B", "D").toArray(), actual.toArray());
    }

    @Test
    public void getShownImageVariants_IncludedNotExistsExcludedFilled_returnAllMinusExcluded(){
        ShownImageVariantsBuilder shownImageVariantsBuilder = new ShownImageVariantsBuilder();
        List<String> allImageVariants = Arrays.asList("A", "B", "C", "D");
        List<String> includedImageVariants = null;
        List<String> excludedImageVariants = Arrays.asList("C");
        shownImageVariantsBuilder.setAllImageVariants(allImageVariants);
        shownImageVariantsBuilder.setExcludedImageVariants(excludedImageVariants);
        shownImageVariantsBuilder.setIncludedImageVariants(includedImageVariants);
        shownImageVariantsBuilder.build();
        final List<String> actual = shownImageVariantsBuilder.getShownImageVariants();
        final List<String> expected = Arrays.asList("A","B","D");
        Assert.assertArrayEquals(expected.toArray(), actual.toArray());
    }

    @Test
    public void getShownImageVariants_IncludedEmpty_ExcludedFilled_returnAllMinusExcluded(){
        ShownImageVariantsBuilder shownImageVariantsBuilder = new ShownImageVariantsBuilder();
        List<String> allImageVariants = Arrays.asList("A", "B", "C", "D");
        List<String> includedImageVariants = new ArrayList<>();
        List<String> excludedImageVariants = Arrays.asList("C");
        shownImageVariantsBuilder.setAllImageVariants(allImageVariants);
        shownImageVariantsBuilder.setExcludedImageVariants(excludedImageVariants);
        shownImageVariantsBuilder.setIncludedImageVariants(includedImageVariants);
        shownImageVariantsBuilder.build();
        final List<String> actual = shownImageVariantsBuilder.getShownImageVariants();
        final List<String> expected = Arrays.asList("A","B","D");
        Assert.assertArrayEquals(expected.toArray(), actual.toArray());
    }

    @Test
    public void getShownImageVariants_IncludedOneEmptyElement_ExcludedFilled_returnAllMinusExcluded(){
        ShownImageVariantsBuilder shownImageVariantsBuilder = new ShownImageVariantsBuilder();
        List<String> allImageVariants = Arrays.asList("A", "B", "C", "D");
        List<String> includedImageVariants = Arrays.asList("");
        List<String> excludedImageVariants = Arrays.asList("C");
        shownImageVariantsBuilder.setAllImageVariants(allImageVariants);
        shownImageVariantsBuilder.setExcludedImageVariants(excludedImageVariants);
        shownImageVariantsBuilder.setIncludedImageVariants(includedImageVariants);
        shownImageVariantsBuilder.build();
        final List<String> actual = shownImageVariantsBuilder.getShownImageVariants();
        final List<String> expected = Arrays.asList("A","B","D");
        Assert.assertArrayEquals(expected.toArray(), actual.toArray());
    }
}
