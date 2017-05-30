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
package org.onehippo.cm.model.mapper;

import java.util.List;

import org.junit.Test;
import org.onehippo.cm.model.JcrBinaryValueImpl;
import org.onehippo.cm.model.Value;
import org.onehippo.cm.model.ValueType;
import org.onehippo.cm.model.impl.ConfigDefinitionImpl;
import org.onehippo.cm.model.impl.ConfigSourceImpl;
import org.onehippo.cm.model.impl.DefinitionNodeImpl;
import org.onehippo.cm.model.impl.GroupImpl;
import org.onehippo.cm.model.impl.ModuleImpl;
import org.onehippo.cm.model.impl.ProjectImpl;
import org.onehippo.cm.model.impl.SourceImpl;
import org.onehippo.cm.model.impl.ValueImpl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ValueFileMapperProviderTest {


    private static final String DUMMY_VALUE = "Dummy value";

    @Test
    public void testLoadPlugins() {
        ValueFileMapperProvider provider = ValueFileMapperProvider.getInstance();
        List<ValueFileMapper> list = provider.valueFileMappers;
        assertEquals(3, list.size());
        assertTrue(list.stream().anyMatch(x -> x.getClass().equals(DummyValueFileMapper.class)));
        assertTrue(list.stream().anyMatch(x -> x.getClass().equals(OtherValueFileMapper.class)));

        GroupImpl group = new GroupImpl("dummyGroup");
        ProjectImpl project = new ProjectImpl("dummyProject", group);
        ModuleImpl module = new ModuleImpl("dummyModule", project);
        SourceImpl source = new ConfigSourceImpl("somePath", module);
        ConfigDefinitionImpl definition = new ConfigDefinitionImpl(source);
        DefinitionNodeImpl definitionNode = new DefinitionNodeImpl("/path/to/", "dummyNode", definition);

        ValueImpl value = new ValueImpl(DUMMY_VALUE);
        definitionNode.addProperty("dummyProperty", value);

        String smartName = provider.generateName(value);
        assertEquals(DUMMY_VALUE, smartName);
    }
}

class DummyValueFileMapper implements ValueFileMapper{

    @Override
    public String apply(Value value) {
        return value.getType() == ValueType.BINARY && value instanceof JcrBinaryValueImpl ? null : value.getString();
    }
}

class OtherValueFileMapper implements ValueFileMapper{

    @Override
    public String apply(Value value) {
        return null;
    }
}