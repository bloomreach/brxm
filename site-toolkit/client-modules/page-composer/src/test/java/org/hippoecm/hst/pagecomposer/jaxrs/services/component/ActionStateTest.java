/*
 * Copyright 2020 Bloomreach
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
package org.hippoecm.hst.pagecomposer.jaxrs.services.component;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import static java.util.Collections.emptyMap;
import static java.util.stream.Collectors.toMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hippoecm.hst.pagecomposer.jaxrs.services.component.ActionState.merge;

public class ActionStateTest {

    @Test
    public void test_merge_adding_an_action() {

        final Map<NamedCategory, Boolean> a2 = new HashMap<>();
        a2.put(HstAction.XPAGE_PUBLISH, false);

        final Map<String, Boolean> actions = merge(new ActionState(emptyMap(), emptyMap()), new ActionState(a2, emptyMap()))
                .getActions().entrySet().stream()
                .collect(toMap(e -> e.getKey().getName(), Map.Entry::getValue));

        assertThat(actions).hasSize(1);
        assertThat(actions.get("publish")).isFalse();

    }


    @Test
    public void test_merge_replacing_an_action() {

        final Map<NamedCategory, Boolean> a1 = new HashMap<>();
        a1.put(HstAction.XPAGE_PUBLISH, true);

        final Map<NamedCategory, Boolean> a2 = new HashMap<>();
        a2.put(HstAction.XPAGE_PUBLISH, false);

        final Map<String, Boolean> actions = merge(new ActionState(a1, emptyMap()), new ActionState(a2, emptyMap()))
                .getActions().entrySet().stream()
                .collect(toMap(e -> e.getKey().getName(), Map.Entry::getValue));

        assertThat(actions).hasSize(1);
        assertThat(actions.get("publish")).isFalse();

    }

    @Test
    public void test_merge_replacing_an_action_twice() {

        final Map<NamedCategory, Boolean> a1 = new HashMap<>();
        a1.put(HstAction.XPAGE_PUBLISH, null);

        final Map<NamedCategory, Boolean> a2 = new HashMap<>();
        a2.put(HstAction.XPAGE_PUBLISH, false);

        final ActionState firstMerge = merge(new ActionState(a1, emptyMap()), new ActionState(a2, emptyMap()));
        final Map<String, Boolean> firstActions = firstMerge
                .getActions().entrySet().stream()
                .collect(toMap(e -> e.getKey().getName(), Map.Entry::getValue));

        assertThat(firstActions).hasSize(1);
        assertThat(firstActions.get("publish")).isFalse();

        final Map<NamedCategory, Boolean> a3 = new HashMap<>();
        a3.put(HstAction.XPAGE_PUBLISH, true);

        final Map<String, Boolean> secondActions = merge(firstMerge, new ActionState(a3, emptyMap()))
                .getActions().entrySet().stream()
                .collect(toMap(e -> e.getKey().getName(), Map.Entry::getValue));

        assertThat(secondActions).hasSize(1);
        assertThat(secondActions.get("publish")).isTrue();

    }


    @Test
    public void test_merge_removing_an_action() {

        final Map<NamedCategory, Boolean> a1 = new HashMap<>();
        a1.put(HstAction.XPAGE_PUBLISH, true);
        a1.put(HstAction.XPAGE_COPY, true);

        final Map<NamedCategory, Boolean> a2 = new HashMap<>();
        a2.put(HstAction.XPAGE_PUBLISH, null);

        final Map<String, Boolean> actions = merge(new ActionState(a1, emptyMap()), new ActionState(a2, emptyMap()))
                .getActions().entrySet().stream()
                .collect(toMap(e -> e.getKey().getName(), Map.Entry::getValue));

        assertThat(actions).hasSize(1);
        assertThat(actions.get("copy")).isTrue();

    }
}
