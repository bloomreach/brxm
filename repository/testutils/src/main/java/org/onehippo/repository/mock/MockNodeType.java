/*
 *  Copyright 2012 Hippo.
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
package org.onehippo.repository.mock;

import javax.jcr.Value;
import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeIterator;
import javax.jcr.nodetype.PropertyDefinition;

/**
 * Mock version of a {@link NodeType}. It only returns the name of the type. All other methods throw an
 * {@link UnsupportedOperationException}.
 */
public class MockNodeType implements NodeType {

    private final String name;

    public MockNodeType(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    // REMAINING METHODS ARE NOT IMPLEMENTED

    @Override
    public NodeType[] getSupertypes() {
        throw new UnsupportedOperationException();
    }

    @Override
    public NodeType[] getDeclaredSupertypes() {
        throw new UnsupportedOperationException();
    }

    @Override
    public NodeTypeIterator getSubtypes() {
        throw new UnsupportedOperationException();
    }

    @Override
    public NodeTypeIterator getDeclaredSubtypes() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isNodeType(final String nodeTypeName) {
        throw new UnsupportedOperationException();
    }

    @Override
    public PropertyDefinition[] getPropertyDefinitions() {
        throw new UnsupportedOperationException();
    }

    @Override
    public NodeDefinition[] getChildNodeDefinitions() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean canSetProperty(final String propertyName, final Value value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean canSetProperty(final String propertyName, final Value[] values) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean canAddChildNode(final String childNodeName) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean canAddChildNode(final String childNodeName, final String nodeTypeName) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean canRemoveItem(final String itemName) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean canRemoveNode(final String nodeName) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean canRemoveProperty(final String propertyName) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String[] getDeclaredSupertypeNames() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isAbstract() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isMixin() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean hasOrderableChildNodes() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isQueryable() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getPrimaryItemName() {
        throw new UnsupportedOperationException();
    }

    @Override
    public PropertyDefinition[] getDeclaredPropertyDefinitions() {
        throw new UnsupportedOperationException();
    }

    @Override
    public NodeDefinition[] getDeclaredChildNodeDefinitions() {
        throw new UnsupportedOperationException();
    }

}
