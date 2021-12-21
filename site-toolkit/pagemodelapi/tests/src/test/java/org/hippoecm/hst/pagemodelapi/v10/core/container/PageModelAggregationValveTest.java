/*
 * Copyright 2021 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.pagemodelapi.v10.core.container;

import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;

import org.apache.groovy.util.Maps;
import org.assertj.core.api.Assertions;
import org.hippoecm.hst.pagemodelapi.v10.core.container.PageModelAggregationValve;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class PageModelAggregationValveTest {

    @Test
    public void residual_parameters_to_map() throws Exception {

        final Map<String, String> parameters = Maps.of(
                "foo", "foo",
                "bar", "bar",
                "lux", "lux");
        final String[] variants = null;

        final List<String> paramsInfoNames = Lists.asList("foo", new String[0]);

        Map<String, String> result = PageModelAggregationValve.getResidualParametersMap(parameters, variants, paramsInfoNames);

        assertThat(result)
                .as("Expected that 'foo' was filtered out since explicit parameter and not residual")
                .containsOnlyKeys("bar", "lux");
    }

    @Test
    public void residual_parameters_null_values_to_map() throws Exception {

        final Map<String, String> parameters = Maps.of(
                "foo", "bar",
                "bar", "bar",
                "lux", null);
        final String[] variants = null;

        final List<String> paramsInfoNames = Lists.asList("foo", new String[0]);

        Map<String, String> result = PageModelAggregationValve.getResidualParametersMap(parameters, variants, paramsInfoNames);

        assertThat(result)
                .as("Expected that 'foo' was filtered out since explicit parameter and not residual")
                .containsOnlyKeys("bar", "lux");

        assertThat(result.get("lux")).isNull();
    }

}
