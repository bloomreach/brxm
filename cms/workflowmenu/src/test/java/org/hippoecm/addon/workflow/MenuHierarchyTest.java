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

package org.hippoecm.addon.workflow;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.wicket.Component;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.util.tester.WicketTester;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class MenuHierarchyTest {

    @Test
    public void list() {
        new WicketTester();
        MenuHierarchy hierarchy = new MenuHierarchy(Collections.singletonList("info"), null, null);
        final ActionDescription action = new ActionDescription("infoId") {

            @Override
            public String getSubMenu() {
                return "info";
            }

            @Override
            protected void invoke() {
                // do nothing
            }
        };
        MenuBar menuBar = new MenuBar("info", hierarchy);
        action.add(new Fragment("text", "text", menuBar));
        hierarchy.put("info", action);
        hierarchy.put("info", action);

        final List<Component> actual = hierarchy.list(menuBar)
                .stream()
                .filter(component -> component instanceof MenuLabel)
                .collect(Collectors.toList());
        assertEquals(2, actual.size());
    }
}
