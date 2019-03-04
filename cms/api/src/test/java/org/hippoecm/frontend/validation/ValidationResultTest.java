/*
 *  Copyright 2019 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.validation;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.model.Model;
import org.hippoecm.frontend.types.MockFieldDescriptor;
import org.hippoecm.frontend.types.MockTypeDescriptor;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Tests with various document setup for counting the number of affected fields based on all generated violations.
 * Fields can be stored as property (String, Date etc) but also as nodes (RichText, ImageLink etc). Violations can be
 * for fields or for 'node fields'. Documents can have a hierarchical structure with fields or 'node fields' in child
 * nodes. These are compounds or content block fields. A path to a field that is stored in a Violation is assumed to be
 * an array of elements representing:
 * <ul>
 * <li>all nodes (for 'node fields')</li>
 * <li>all nodes and a field as last element (for property fields)</li>
 * </ul>
 */
public class ValidationResultTest {

    @Test
    public void oneViolationOneSingleField() {
        Set<Violation> violations = new HashSet<>();
        violations.add(getTestViolation("test", "message1"));

        ValidationResult validationResult = new ValidationResult(violations);

        assertEquals(validationResult.getAffectedFields(), 1);
    }

    @Test
    public void twoViolationsOneField() {
        Set<Violation> violations = new HashSet<>();
        violations.add(getTestViolation("test", "message1"));
        violations.add(getTestViolation("test", "message2"));

        ValidationResult validationResult = new ValidationResult(violations);

        assertEquals(validationResult.getAffectedFields(), 1);
    }

    @Test
    public void oneViolationOneMultivalueField() {
        Set<Violation> violations = new HashSet<>();
        violations.add(getTestViolation("test[0]", "message1"));
        violations.add(getTestViolation("test[1]", "message1"));

        ValidationResult validationResult = new ValidationResult(violations);

        assertEquals(validationResult.getAffectedFields(), 2);
    }

    @Test
    public void twoViolationsOneMultivalueField() {
        Set<Violation> violations = new HashSet<>();
        violations.add(getTestViolation("test[0]", "message1"));
        violations.add(getTestViolation("test[0]", "message2"));
        violations.add(getTestViolation("test[1]", "message1"));
        violations.add(getTestViolation("test[1]", "message2"));

        ValidationResult validationResult = new ValidationResult(violations);

        assertEquals(validationResult.getAffectedFields(), 2);
    }

    @Test
    public void variousViolationsOneMultivalueField() {
        Set<Violation> violations = new HashSet<>();
        violations.add(getTestViolation("test[0]", "message1"));
        violations.add(getTestViolation("test[1]", "message1"));
        violations.add(getTestViolation("test[1]", "message2"));

        ValidationResult validationResult = new ValidationResult(violations);

        assertEquals(validationResult.getAffectedFields(), 2);
    }

    @Test
    public void twoViolationsTwoFields() {
        Set<Violation> violations = new HashSet<>();
        violations.add(getTestViolation("test", "message1"));
        violations.add(getTestViolation("test2", "message1"));

        ValidationResult validationResult = new ValidationResult(violations);

        assertEquals(validationResult.getAffectedFields(), 2);
    }

    @Test
    public void twoViolationsTwoMultiValueFields() {
        Set<Violation> violations = new HashSet<>();
        violations.add(getTestViolation("firstField[0]", "message1"));
        violations.add(getTestViolation("firstField[0]", "message2"));
        violations.add(getTestViolation("firstField[1]", "message1"));
        violations.add(getTestViolation("firstField[1]", "message2"));
        violations.add(getTestViolation("secondField[0]", "message1"));
        violations.add(getTestViolation("secondField[0]", "message2"));
        violations.add(getTestViolation("secondField[1]", "message1"));
        violations.add(getTestViolation("secondField[1]", "message2"));

        ValidationResult validationResult = new ValidationResult(violations);

        assertEquals(validationResult.getAffectedFields(), 4);
    }

    @Test
    public void oneViolationOneNodeField() {
        Set<Violation> violations = new HashSet<>();
        violations.add(getTestViolationNode("test", "message1"));

        ValidationResult validationResult = new ValidationResult(violations);

        assertEquals(validationResult.getAffectedFields(), 1);
    }

    @Test
    public void oneViolationOneFieldInChildNode() {
        Set<Violation> violations = new HashSet<>();
        violations.add(getTestViolation("compound/test", "message1"));

        ValidationResult validationResult = new ValidationResult(violations);

        assertEquals(validationResult.getAffectedFields(), 1);
    }

    @Test
    public void oneViolationOneNodeFieldInChildNode() {
        Set<Violation> violations = new HashSet<>();
        violations.add(getTestViolationNode("compound/test", "message1"));

        ValidationResult validationResult = new ValidationResult(violations);

        assertEquals(validationResult.getAffectedFields(), 1);
    }

    @Test
    public void twoViolationsOneMultipleNodeField() {
        Set<Violation> violations = new HashSet<>();
        violations.add(getTestViolationNode("compound[0]/test", "message1"));
        violations.add(getTestViolationNode("compound[1]/test", "message1"));

        ValidationResult validationResult = new ValidationResult(violations);

        assertEquals(validationResult.getAffectedFields(), 2);
    }

    @Test
    public void twoViolationsInCompoundNodeAndRegularField() {
        Set<Violation> violations = new HashSet<>();
        violations.add(getTestViolation("compound[0]/field", "message1"));
        violations.add(getTestViolationNode("compound[0]/nodeField", "message1"));

        ValidationResult validationResult = new ValidationResult(violations);

        assertEquals(validationResult.getAffectedFields(), 2);
    }

    @Test
    public void fourViolationsInVariousSetup() {
        Set<Violation> violations = new HashSet<>();
        violations.add(getTestViolation("field", "message1"));
        violations.add(getTestViolationNode("nodeField", "message1"));
        violations.add(getTestViolation("compound[0]/field", "message2"));
        violations.add(getTestViolationNode("compound[0]/nodeField", "message2"));

        ValidationResult validationResult = new ValidationResult(violations);

        assertEquals(validationResult.getAffectedFields(), 4);
    }

    /**
     * Violation where all path elements are nodes except the last one, which will be a field.
     */
    private Violation getTestViolation(final String path, final String message) {
        return getTestViolation(path, message, false);
    }

    /**
     * Violation where also the last path element is a Node, not a field.
     */
    private Violation getTestViolationNode(final String path, final String message) {
        return getTestViolation(path, message, true);
    }

    private Violation getTestViolation(final String path, final String message, final boolean isNode) {
        Set<ModelPath> paths = new HashSet<>();

        final String[] pathElements = StringUtils.split(path, "/");
        ModelPathElement[] modelPathElements = new ModelPathElement[pathElements.length];
        int count = 0;
        for (String pathelement : pathElements) {
            if (isNode && count == pathElements.length - 1) {
                modelPathElements[count++] = new ModelPathElement(getNodeFieldDescriptor(pathelement), pathelement, 0);
            } else {
                modelPathElements[count++] = new ModelPathElement(new MockFieldDescriptor(pathelement), pathelement, 0);
            }
        }

        paths.add(new ModelPath(modelPathElements));
        return new Violation(paths, Model.of(message));

    }

    private MockFieldDescriptor getNodeFieldDescriptor(final String name) {
        MockFieldDescriptor mockFieldDescriptor = new MockFieldDescriptor(name);
        MockTypeDescriptor mockTypeDescriptor = new MockTypeDescriptor();
        mockTypeDescriptor.setIsNode(true);
        mockFieldDescriptor.setMockTypeDescriptor(mockTypeDescriptor);
        return mockFieldDescriptor;
    }

}