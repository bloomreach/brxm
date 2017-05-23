/*
 *  Copyright 2016-2017 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cm.model;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.onehippo.cm.model.serializer.ResourceNameResolver;
import org.onehippo.cm.model.serializer.ResourceNameResolverImpl;


public class ResourceNameResolverImplTest {

    private ResourceNameResolver nameResolver;

    @Before
    public void setUp() throws Exception {
        nameResolver = new ResourceNameResolverImpl();
    }

    @Test
    public void verify_name_clashes() {

        String propertyPath = nameResolver.generateName("/a/b/c.txt");
        Assert.assertEquals("/a/b/c.txt", propertyPath);

        propertyPath = nameResolver.generateName("/a/b/c.txt");
        Assert.assertEquals("/a/b/c-1.txt", propertyPath);

        propertyPath = nameResolver.generateName("/a/a/c");
        Assert.assertEquals("/a/a/c", propertyPath);

        propertyPath = nameResolver.generateName("/a/a/c");
        Assert.assertEquals("/a/a/c-1", propertyPath);

        propertyPath = nameResolver.generateName("/a/b/c[0].txt");
        Assert.assertEquals("/a/b/c[0].txt", propertyPath);

        propertyPath = nameResolver.generateName("/a/b/c[0].txt");
        Assert.assertEquals("/a/b/c[0]-1.txt", propertyPath);
    }

    @Test
    public void verify_dir_name_clashes() {

        String propertyPath2 = nameResolver.generateName("/a/b/c/d");
        Assert.assertEquals("/a/b/c/d", propertyPath2);

        String propertyPath = nameResolver.generateName("/a/b/c");
        Assert.assertEquals("/a/b/c", propertyPath);



    }


}