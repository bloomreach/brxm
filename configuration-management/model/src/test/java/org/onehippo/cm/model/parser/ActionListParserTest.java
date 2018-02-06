/*
 *  Copyright 2017 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cm.model.parser;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.onehippo.cm.model.AbstractBaseTest;
import org.onehippo.cm.model.definition.ActionItem;
import org.onehippo.cm.model.definition.ActionType;
import org.onehippo.cm.model.impl.GroupImpl;
import org.onehippo.cm.model.impl.ModuleImpl;
import org.onehippo.cm.model.impl.ProjectImpl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;


public class ActionListParserTest extends AbstractBaseTest {

    @Test
    public void testLoad() throws ParserException {
        InputStream stream = this.getClass().getResourceAsStream("/parser/value_test/hcm-actions.yaml");
        Map<String, Set<ActionItem>> actionsMap = parseActionMap(stream);

        assertTrue(actionsMap.size() == 3);
        Set<ActionItem> actionItemsV1 = actionsMap.get("1.0");
        assertTrue(actionItemsV1.size() == 2);
        assertEquals(ActionType.RELOAD, actionItemsV1.iterator().next().getType());
    }

    @Test
    public void expect_error_when_using_sns_in_path() {
        final String yaml =
                "action-lists:\n" +
                "- 1.0:\n" +
                "    /content/sns[1]: reload\n";

        try {
            parseActionMap(yaml);
            fail("Expected exception");
        } catch (ParserException e) {
            assertEquals("Path must not contain name indices", e.getMessage());
        }
    }

    @Test
    public void expect_error_for_append_action() {
        final String yaml =
                "action-lists:\n" +
                        "- 1.0:\n" +
                        "    /content/dup: append\n";

        try {
            parseActionMap(yaml);
            fail("Expected exception");
        } catch (ParserException e) {
            assertEquals("APPEND action type can't be specified in action lists file", e.getMessage());
        }
    }

    @Test
    public void expect_error_for_duplicate_path() {
        final String yaml =
                "action-lists:\n" +
                        "- 1.0:\n" +
                        "    /content/dup: delete\n" +
                        "    /content/dup: reload\n";

        try {
            parseActionMap(yaml);
            fail("Expected exception");
        } catch (ParserException e) {
            assertEquals("Duplicate paths are not allowed in the same version: /content/dup", e.getMessage());
        }
    }

    @Test
    public void expect_error_when_using_not_absolute_path() {
        final String yaml =
                "action-lists:\n" +
                "- 1.0:\n" +
                "    content/node: reload\n";

        try {
            parseActionMap(yaml);
            fail("Expected exception");
        } catch (ParserException e) {
            assertEquals("Path must start with a slash", e.getMessage());
        }
    }

    private Map<String, Set<ActionItem>> parseActionMap(final String yaml) throws ParserException {
        return parseActionMap(IOUtils.toInputStream(yaml, StandardCharsets.UTF_8));
    }

    private Map<String, Set<ActionItem>> parseActionMap(final InputStream inputStream) throws ParserException {
        ActionListParser parser = new ActionListParser();
        GroupImpl group = new GroupImpl("group");
        ProjectImpl project = new ProjectImpl("project", group);
        ModuleImpl module = new ModuleImpl("module", project);

        parser.parse(inputStream, "test", module);

        return module.getActionsMap();
    }

}
