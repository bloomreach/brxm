/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.util;

import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import javax.jcr.Node;
import javax.jcr.PropertyType;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.PropertyDefinition;

import org.junit.Before;
import org.junit.Test;

/**
 * TestPropertyDefinitionUtils
 * 
 * @version $Id$
 */
public class TestPropertyDefinitionUtils {
    
    // base document node type
    private NodeType baseDocNodeType;
    private PropertyDefinition authorPropDef;
    
    // base document #2 with residual property definition node type
    private NodeType baseDoc2NodeType;
    private PropertyDefinition baseDoc2ResidualPropDef;
    private PropertyDefinition baseDoc2BooleanResidualPropDef;
    
    // text page node type
    private NodeType textPageNodeType;
    private PropertyDefinition titlePropDef;
    private PropertyDefinition summaryPropDef;
    
    // text page #2 with residual property definition
    private NodeType textPage2NodeType;
    private PropertyDefinition textPage2ResidualPropDef;
    private PropertyDefinition textPage2BooleanResidualPropDef;
    
    @Before
    public void setUp() {
        baseDocNodeType = createNiceMock(NodeType.class);
        baseDoc2NodeType = createNiceMock(NodeType.class);
        textPageNodeType = createNiceMock(NodeType.class);
        textPage2NodeType = createNiceMock(NodeType.class);
        
        authorPropDef = createNiceMock(PropertyDefinition.class);
        baseDoc2ResidualPropDef = createNiceMock(PropertyDefinition.class);
        baseDoc2BooleanResidualPropDef = createNiceMock(PropertyDefinition.class);
        titlePropDef = createNiceMock(PropertyDefinition.class);
        summaryPropDef = createNiceMock(PropertyDefinition.class);
        textPage2ResidualPropDef = createNiceMock(PropertyDefinition.class);
        textPage2BooleanResidualPropDef = createNiceMock(PropertyDefinition.class);
        
        expect(authorPropDef.getName()).andReturn("author").anyTimes();
        expect(baseDoc2ResidualPropDef.getName()).andReturn("*").anyTimes();
        expect(baseDoc2BooleanResidualPropDef.getName()).andReturn("*").anyTimes();
        expect(baseDoc2BooleanResidualPropDef.getRequiredType()).andReturn(PropertyType.BOOLEAN).anyTimes();
        expect(titlePropDef.getName()).andReturn("title").anyTimes();
        expect(summaryPropDef.getName()).andReturn("summary").anyTimes();
        expect(textPage2ResidualPropDef.getName()).andReturn("*").anyTimes();
        expect(textPage2BooleanResidualPropDef.getName()).andReturn("*").anyTimes();
        expect(textPage2BooleanResidualPropDef.getRequiredType()).andReturn(PropertyType.BOOLEAN).anyTimes();
        
        expect(baseDocNodeType.getPropertyDefinitions()).andReturn(new PropertyDefinition [] { authorPropDef }).anyTimes();
        expect(baseDoc2NodeType.getPropertyDefinitions()).andReturn(new PropertyDefinition [] { authorPropDef, baseDoc2ResidualPropDef, baseDoc2BooleanResidualPropDef }).anyTimes();
        expect(textPageNodeType.getPropertyDefinitions()).andReturn(new PropertyDefinition [] { titlePropDef, summaryPropDef }).anyTimes();
        expect(textPage2NodeType.getPropertyDefinitions()).andReturn(new PropertyDefinition [] { titlePropDef, summaryPropDef, textPage2ResidualPropDef, textPage2BooleanResidualPropDef }).anyTimes();
        
        replay(authorPropDef);
        replay(baseDoc2ResidualPropDef);
        replay(baseDoc2BooleanResidualPropDef);
        replay(titlePropDef);
        replay(summaryPropDef);
        replay(textPage2ResidualPropDef);
        replay(textPage2BooleanResidualPropDef);
        
        replay(baseDocNodeType);
        replay(baseDoc2NodeType);
        replay(textPageNodeType);
        replay(textPage2NodeType);
    }
    
    @Test
    public void testBasicUsage() throws Exception {
        Node node = createNiceMock(Node.class);
        
        expect(node.getPrimaryNodeType()).andReturn(textPageNodeType).anyTimes();
        expect(node.getMixinNodeTypes()).andReturn(new NodeType [] { baseDocNodeType }).anyTimes();
        
        replay(node);
        
        assertEquals(titlePropDef, PropertyDefinitionUtils.getPropertyDefinition(node, "title"));
        assertEquals(summaryPropDef, PropertyDefinitionUtils.getPropertyDefinition(node, "summary"));
        assertEquals(authorPropDef, PropertyDefinitionUtils.getPropertyDefinition(node, "author"));
        assertNull(PropertyDefinitionUtils.getPropertyDefinition(node, "reader"));
    }
    
    @Test
    public void testUsageWithDirectResidualProp() throws Exception {
        Node node = createNiceMock(Node.class);
        
        expect(node.getPrimaryNodeType()).andReturn(textPage2NodeType).anyTimes();
        expect(node.getMixinNodeTypes()).andReturn(new NodeType [] { baseDocNodeType }).anyTimes();
        
        replay(node);
        
        assertEquals(titlePropDef, PropertyDefinitionUtils.getPropertyDefinition(node, "title"));
        assertEquals(summaryPropDef, PropertyDefinitionUtils.getPropertyDefinition(node, "summary"));
        assertEquals(authorPropDef, PropertyDefinitionUtils.getPropertyDefinition(node, "author"));
        assertEquals(textPage2BooleanResidualPropDef, PropertyDefinitionUtils.getPropertyDefinition(node, "editable", PropertyType.BOOLEAN));
        assertEquals(textPage2ResidualPropDef, PropertyDefinitionUtils.getPropertyDefinition(node, "reader"));
    }
    
    @Test
    public void testUsageWithBaseResidualProp() throws Exception {
        Node node = createNiceMock(Node.class);
        
        expect(node.getPrimaryNodeType()).andReturn(textPageNodeType).anyTimes();
        expect(node.getMixinNodeTypes()).andReturn(new NodeType [] { baseDoc2NodeType }).anyTimes();
        
        replay(node);
        
        assertEquals(titlePropDef, PropertyDefinitionUtils.getPropertyDefinition(node, "title"));
        assertEquals(summaryPropDef, PropertyDefinitionUtils.getPropertyDefinition(node, "summary"));
        assertEquals(authorPropDef, PropertyDefinitionUtils.getPropertyDefinition(node, "author"));
        assertEquals(baseDoc2BooleanResidualPropDef, PropertyDefinitionUtils.getPropertyDefinition(node, "publishable", PropertyType.BOOLEAN));
        assertEquals(baseDoc2ResidualPropDef, PropertyDefinitionUtils.getPropertyDefinition(node, "approver"));
    }
    
    @Test
    public void testUsageWithOverridingResidualProp() throws Exception {
        Node node = createNiceMock(Node.class);
        
        expect(node.getPrimaryNodeType()).andReturn(textPage2NodeType).anyTimes();
        expect(node.getMixinNodeTypes()).andReturn(new NodeType [] { baseDoc2NodeType }).anyTimes();
        
        replay(node);
        
        assertEquals(titlePropDef, PropertyDefinitionUtils.getPropertyDefinition(node, "title"));
        assertEquals(summaryPropDef, PropertyDefinitionUtils.getPropertyDefinition(node, "summary"));
        assertEquals(authorPropDef, PropertyDefinitionUtils.getPropertyDefinition(node, "author"));
        assertEquals(textPage2BooleanResidualPropDef, PropertyDefinitionUtils.getPropertyDefinition(node, "editable", PropertyType.BOOLEAN));
        assertEquals(textPage2ResidualPropDef, PropertyDefinitionUtils.getPropertyDefinition(node, "reader"));
    }
}
