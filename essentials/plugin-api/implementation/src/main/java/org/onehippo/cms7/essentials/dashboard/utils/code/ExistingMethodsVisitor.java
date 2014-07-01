/*
 * Copyright 2014 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.cms7.essentials.dashboard.utils.code;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Method visitor which collects all methods that are annotated by {@code HippoEssentialsGenerated}  annotation
 *
 * @version "$Id: ExistingMethodsVisitor.java 173285 2013-08-09 10:59:41Z mmilicevic $"
 * @see HippoEssentialsGenerated
 */
public class ExistingMethodsVisitor extends ASTVisitor {

    private static Logger log = LoggerFactory.getLogger(ExistingMethodsVisitor.class);
    private List<MethodDeclaration> methods = new ArrayList<>();
    private List<String> methodsNames = new ArrayList<>();
    private Set<String> internalNames = new HashSet<>();
    private List<String> generatedMethodNames = new ArrayList<>();
    private List<MethodDeclaration> getterMethods = new ArrayList<>();
    private List<EssentialsGeneratedMethod> generatedMethods = new ArrayList<>();

    @Override
    public boolean visit(MethodDeclaration node) {
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
                    String internalName = null;
                    final List<?> values = annotation.values();
                    for (Object value : values) {
                        if (value instanceof MemberValuePair) {
                            final MemberValuePair mvp = (MemberValuePair) value;
                            final String n = mvp.getName().getIdentifier();
                            final Expression myValue = mvp.getValue();
                            if (myValue instanceof StringLiteral) {
                                final StringLiteral lit = (StringLiteral) myValue;
                                final String v = lit.getLiteralValue();
                                if ("internalName".equals(n)) {
                                    internalName = v;
                                }
                            }
                        }
                    }
                    final EssentialsGeneratedMethod genMethod = new EssentialsGeneratedMethod(node, identifier, internalName);
                    log.debug("# found generated method:  {}", genMethod);
                    generatedMethods.add(genMethod);
                    internalNames.add(internalName);
                }
            }
        }

        return super.visit(node);
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
