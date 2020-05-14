/*
 * Copyright 2020 Hippo B.V. (http://www.onehippo.com)
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

package org.hippoecm.frontend.plugins.reviewedactions;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.Properties;
import java.util.function.UnaryOperator;

import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.onehippo.repository.branch.BranchConstants;

import static org.junit.Assert.assertEquals;
@RunWith(Parameterized.class)
public class BranchInfoBuilderTest {

    private final BranchInfoBuilder builder;
    private final String expected;

    private static UnaryOperator<String> propertyResolver = (key) -> {
        try (InputStream input = ClassLoader.getSystemResourceAsStream("org/hippoecm/frontend/plugins/reviewedactions/AbstractDocumentWorkflowPlugin.properties")) {
            Properties prop = new Properties();
            prop.load(input);
            final String value = prop.getProperty(key);
            if (value == null) {
                throw new IllegalArgumentException(String.format("key:%s not found", key));
            }
            return value;
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return null;
    };

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {getUserInfoBuilder()
                        .draftChanges(true)
                        , "Core document (offline, draft changes)"},
                {getUserInfoBuilder()
                        .draftChanges(true)
                        .unpublishedChanges(true)
                        , "Core document (offline, draft changes)"},
                {getUserInfoBuilder()
                        .unpublishedChanges(true)
                        , "Core document (offline)"},
                {getUserInfoBuilder()
                        .draftChanges(true)
                        .live(true)
                        , "Core document (live, draft changes)"},
                {getUserInfoBuilder()
                        .draftChanges(true)
                        .live(true)
                        .unpublishedChanges(true)
                        , "Core document (live, draft changes)"},
                {getUserInfoBuilder()
                        , "Core document (offline)"},
                {getUserInfoBuilder()
                        .live(true)
                        , "Core document (live)"},
                {getUserInfoBuilder()
                        .draftChanges(true)
                        .unpublishedChanges(true)
                        , "Core document (offline, draft changes)"},
                {new BranchInfoBuilder(propertyResolver, () -> "q")
                        .draftChanges(true)
                        .unpublishedChanges(true)
                        , "Project version 'q' (offline, draft changes)"},
                {new BranchInfoBuilder(propertyResolver, () -> "master")
                        .draftChanges(true)
                        .unpublishedChanges(true)
                        , "Core document (offline, draft changes)"},
        });
    }

    @NotNull
    private static BranchInfoBuilder getUserInfoBuilder() {
        return new BranchInfoBuilder(propertyResolver, () -> BranchConstants.MASTER_BRANCH_ID);
    }

    @Before
    public void setUp() throws Exception {
    }

    public BranchInfoBuilderTest(BranchInfoBuilder builder, String expected){
        this.builder = builder;
        this.expected = expected;
    }

    @Test
    public void test() {
        assertEquals(expected,builder.build());
    }
}
