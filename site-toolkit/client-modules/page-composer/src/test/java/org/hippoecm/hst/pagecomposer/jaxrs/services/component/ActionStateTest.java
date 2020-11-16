/*
 * Copyright 2020 Hippo B.V. (http://www.onehippo.com)
 */

package org.hippoecm.hst.pagecomposer.jaxrs.services.component;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

import static java.util.Collections.emptySet;
import static java.util.stream.Collectors.toMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hippoecm.hst.pagecomposer.jaxrs.services.component.ActionState.merge;

public class ActionStateTest {


    @Test
    public void test_merge_replacing_an_action() {

        final Set<Action> a1 = new HashSet<>();
        a1.add(HstAction.XPAGE_PUBLISH.toAction(true));

        final Set<Action> a2 = new HashSet<>();
        a2.add(HstAction.XPAGE_PUBLISH.toAction(false));

        final Map<String, Boolean> actions = merge(new ActionState(a1, emptySet()), new ActionState(a2, emptySet()))
                .getActions().stream()
                .collect(toMap(Action::getName, Action::isEnabled));

        assertThat(actions).hasSize(1);
        assertThat(actions.get("publish")).isFalse();

    }

    @Test
    public void test_merge_replacing_an_action_twice() {

        final Set<Action> a1 = new HashSet<>();
        a1.add(HstAction.XPAGE_PUBLISH.removeAction());

        final Set<Action> a2 = new HashSet<>();
        a2.add(HstAction.XPAGE_PUBLISH.toAction(false));

        final ActionState firstMerge = merge(new ActionState(a1, emptySet()), new ActionState(a2, emptySet()));
        final Map<String, Boolean> firstActions = firstMerge
                .getActions().stream()
                .collect(toMap(Action::getName, Action::isEnabled));

        assertThat(firstActions).hasSize(1);
        assertThat(firstActions.get("publish")).isFalse();

        final Set<Action> a3 = new HashSet<>();
        a3.add(HstAction.XPAGE_PUBLISH.toAction(true));

        final Map<String, Boolean> secondActions = merge(firstMerge, new ActionState(a3, emptySet()))
                .getActions().stream()
                .collect(toMap(Action::getName, Action::isEnabled));

        assertThat(secondActions).hasSize(1);
        assertThat(secondActions.get("publish")).isTrue();

    }


    @Test
    public void test_merge_removing_an_action() {

        final Set<Action> a1 = new HashSet<>();
        a1.add(HstAction.XPAGE_PUBLISH.toAction(true));
        a1.add(HstAction.XPAGE_COPY.toAction(true));

        final Set<Action> a2 = new HashSet<>();
        a2.add(HstAction.XPAGE_PUBLISH.removeAction());

        final Map<String, Boolean> actions = merge(new ActionState(a1, emptySet()), new ActionState(a2, emptySet()))
                .getActions().stream()
                .collect(toMap(Action::getName, Action::isEnabled));

        assertThat(actions).hasSize(1);
        assertThat(actions.get("copy")).isTrue();

    }
}