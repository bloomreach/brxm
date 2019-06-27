/*
 * Copyright 2014-2019 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.cms7.essentials.plugin.sdk.utils.code;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.MemberValuePair;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.NormalAnnotation;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.onehippo.cms7.essentials.dashboard.annotations.HippoEssentialsGenerated;

/**
 * Method visitor which collects all methods that are annotated by {@code HippoEssentialsGenerated} annotation
 *
 * @see HippoEssentialsGenerated
 */
public class ExistingMethodsVisitor extends ASTVisitor {

    private List<MethodDeclaration> methods = new ArrayList<>();
    private List<String> methodsNames = new ArrayList<>();
    private Set<String> internalNames = new HashSet<>();
    private List<String> generatedMethodNames = new ArrayList<>();
    private List<MethodDeclaration> getterMethods = new ArrayList<>();
    private List<EssentialsGeneratedMethod> generatedMethods = new ArrayList<>();
    private List<MethodDeclaration> generatedMethodDeclarations = new ArrayList<>();

    @Override
    public boolean visit(final MethodDeclaration node) {
        final String identifier = node.getName().getIdentifier();
        methods.add(node);
        methodsNames.add(identifier);
        if (identifier.startsWith("get")) {
            getterMethods.add(node);
        }

        final List<?> modifiers = node.modifiers();
        for (Object modifier : modifiers) {
            if (modifier instanceof NormalAnnotation) {
                final NormalAnnotation annotation = (NormalAnnotation) modifier;
                final String fullyQualifiedName = annotation.getTypeName().getFullyQualifiedName();
                if (HippoEssentialsGenerated.class.getSimpleName().equals(fullyQualifiedName)) {
                    generatedMethodNames.add(identifier);
                    final String internalName = getInternalName(annotation);
                    final EssentialsGeneratedMethod generatedMethod = 
                            new EssentialsGeneratedMethod(node, identifier, internalName);
                    generatedMethods.add(generatedMethod);
                    generatedMethodDeclarations.add(node);
                    internalNames.add(internalName);
                }
            }
        }

        return super.visit(node);
    }

    private String getInternalName(final NormalAnnotation annotation) {
        String internalName = null;
        final List<?> values = annotation.values();
        for (Object value : values) {
            if (value instanceof MemberValuePair) {
                final MemberValuePair memberValuePair = (MemberValuePair) value;
                final String identifier = memberValuePair.getName().getIdentifier();
                final Expression memberValuePairValue = memberValuePair.getValue();
                if (memberValuePairValue instanceof StringLiteral) {
                    final StringLiteral literal = (StringLiteral) memberValuePairValue;
                    final String literalValue = literal.getLiteralValue();
                    if ("internalName".equals(identifier)) {
                        internalName = literalValue;
                    }
                }
            }
        }
        return internalName;
    }

    public List<MethodDeclaration> getGeneratedMethodDeclarations() {
        return generatedMethodDeclarations;
    }

    public List<String> getGeneratedMethodNames() {
        return generatedMethodNames;
    }

    public List<EssentialsGeneratedMethod> getGeneratedMethods() {
        return generatedMethods;
    }

    public List<MethodDeclaration> getMethods() {
        return methods;
    }

    public List<String> getMethodsNames() {
        return methodsNames;
    }

    public Set<String> getMethodInternalNames() {
        return internalNames;
    }

    public List<MethodDeclaration> getGetterMethods() {
        return getterMethods;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("ExistingMethodsVisitor{");
        sb.append("methods=").append(methods);
        sb.append(", methodsNames=").append(methodsNames);
        sb.append(", generatedMethodNames=").append(generatedMethodNames);
        sb.append(", generatedMethods=").append(generatedMethods);
        sb.append('}');
        return sb.toString();
    }
}
