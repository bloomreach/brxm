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
package org.onehippo.cm.engine.parser;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.onehippo.cm.api.model.action.ActionItem;
import org.onehippo.cm.api.model.action.ActionType;
import org.onehippo.cm.engine.AbstractBaseTest;
import org.onehippo.cm.impl.model.GroupImpl;
import org.onehippo.cm.impl.model.ModuleImpl;
import org.onehippo.cm.impl.model.ProjectImpl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


public class ActionListParserTest extends AbstractBaseTest {

    @Test
    public void testLoad() throws ParserException {
        ActionListParser parser = new ActionListParser(true);
        GroupImpl group = new GroupImpl("group");
        ProjectImpl project = new ProjectImpl("project", group);
        ModuleImpl module = new ModuleImpl("module", project);

        InputStream stream = this.getClass().getResourceAsStream("/parser/value_test/hcm-actions.yaml");
        parser.parse(stream, "test", module);

        Map<Double, List<ActionItem>> actionsMap = module.getActionsMap();
        assertTrue(actionsMap.size() == 3);
        List<ActionItem> actionItemsV1 = actionsMap.get(1.0d);
        assertTrue(actionItemsV1.size() == 2);
        assertEquals(ActionType.APPEND, actionItemsV1.get(0).getType());

    }

}