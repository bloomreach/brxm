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
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.NormalAnnotation;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.onehippo.cms7.essentials.dashboard.annotations.HippoEssentialsGenerated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Visits a java file and returns all methods we can annotate which are not annotated by
 * {@code HippoEssentialsGenerated} annotation,
 *
 * @version "$Id$"
 * @see HippoEssentialsGenerated
 */
public class NoAnnotationMethodVisitor extends ASTVisitor {

    private static Logger log = LoggerFactory.getLogger(NoAnnotationMethodVisitor.class);
    private final List<EssentialsGeneratedMethod> modifiableMethods = new ArrayList<>();
    private final Set<String> modifiableMethodsInternalNames = new HashSet<>();

    @Override
    public boolean visit(MethodDeclaration node) {
        final List<?> modifiers = node.modifiers();
        for (Object modifier : modifiers) {
            if (modifier instanceof NormalAnnotation) {
                final NormalAnnotation annotation = (NormalAnnotation) modifier;
                final String fullyQualifiedName = annotation.getTypeName().getFullyQualifiedName();
                if (HippoEssentialsGenerated.class.getSimpleName().equals(fullyQualifiedName)) {
                    return super.visit(node);
                }
            }
        }
        // no HippoEssentialsGenerated annotation found, analyze method and see if we can annotate it:
        final Block body = node.getBody();
        @SuppressWarnings("rawtypes")
        final List statements = body.statements();
        processStatements(node, statements);
        return super.visit(node);
    }

    private void processStatements(final MethodDeclaration node, final Iterable<?> statements) {
        for (Object o : statements) {
            if (o instanceof ReturnStatement) {
                final ReturnStatement statement = (ReturnStatement) o;
                final Expression e = statement.getExpression();
                if (e instanceof MethodInvocation) {
                    final MethodInvocation methodInvocation = (MethodInvocation) e;
                    @SuppressWarnings("rawtypes")
                    final List arguments = methodInvocation.arguments();
                    if (arguments != null) {
                        for (Object arg : arguments) {
                            log.debug("arg {}", arg.getClass());
                            if (arg instanceof StringLiteral) {
                                final StringLiteral argument = (StringLiteral) arg;
                                final String value = argument.getLiteralValue();
                                log.debug("Found string argument {}", value);
                                // check if namespaces property
                                if (value.indexOf(':') != -1) {
                                    modifiableMethods.add(new EssentialsGeneratedMethod(node, node.getName().getIdentifier(), value));
                                    modifiableMethodsInternalNames.add(value);
                                }
                            } else {
                                log.debug("#NOT IMPLEMENTED PARSING OF ARGUMENT: {}", arg.getClass());
                                /*
                                else if (arg instanceof SimpleName) {
                                final SimpleName argument = (SimpleName) arg;
                                final Object val = argument.resolveConstantExpressionValue();
                                // TODO always null, upgrade to latest JDT and check if we can resolve this
                                // @see org.onehippo.cms7.essentials.dashboard.utils.JavaSourceUtils.getAnnotateMethods()
                                log.debug("identifier {}", val);

                                } else {
                                log.warn("#NOT IMPLEMENTED PARSING OF ARGUMENT: {}", arg.getClass());
                                log.debug("e {}", arg);
                                }
                            */

                            }
                        }
                    }
                } else {
                    log.debug("#NOT IMPLEMENTED PARSING OF: {}", e.getClass());
                }
            }
        }
    }

    public Set<String> getModifiableMethodsInternalNames() {
        return modifiableMethodsInternalNames;
    }

    public List<EssentialsGeneratedMethod> getModifiableMethods() {
        return modifiableMethods;
    }


}
