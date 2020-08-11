/*
 *  Copyright 2012-2015 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.repository.update;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.codehaus.groovy.ast.expr.ClassExpression;
import org.codehaus.groovy.ast.expr.ConstructorCallExpression;
import org.codehaus.groovy.ast.expr.DeclarationExpression;
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.ast.expr.MethodCallExpression;
import org.codehaus.groovy.ast.expr.PropertyExpression;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.customizers.CompilationCustomizer;
import org.codehaus.groovy.control.customizers.ImportCustomizer;
import org.codehaus.groovy.control.customizers.SecureASTCustomizer;
import org.codehaus.groovy.syntax.Types;

import groovy.lang.GroovyClassLoader;

/**
 * {@link GroovyClassLoader} that is configured for updaters
 * <p>
 * While this custom Groovy ClassLoader protects against certain obvious and trivial mistakes and misuse of the Groovy
 * language, but is not assumed or even intended to provide a full blown and trusted Groovy execution sandbox.
 * See for more information: {@link #createCompilationCustomizer()}.
 * </p>
 */
public class GroovyUpdaterClassLoader extends GroovyClassLoader {

    private static final String[] defaultImports = {
            "org.onehippo.repository.update", "javax.jcr", "javax.jcr.nodetype",
            "javax.jcr.security", "javax.jcr.version"
    };
    private static final String[] importsBlacklist = {
            "java.io.File", "java.io.FileDescriptor", "java.io.FileInputStream",
            "java.io.FileOutputStream", "java.io.FileWriter", "java.io.FileReader"
    };
    private static final String[] starImportsBlacklist = {
            "java.nio.file", "java.net", "javax.net", "javax.net.ssl", "java.lang.reflect"
    };

    private static final Set<String> illegalClasses;
    static {
        final Set<String> s = new HashSet<>();
        s.add("java.lang.Runtime");
        s.add("java.lang.ProcessBuilder");
        illegalClasses = Collections.unmodifiableSet(s);
    }

    private static final Map<String, Collection<String>> illegalMethods;
    static {
        final Map<String, Collection<String>> s = new HashMap<>();
        s.put("java.lang.System", new HashSet<>(Arrays.asList("exit")));
        s.put("java.lang.Class", new HashSet<>(Arrays.asList("forName")));
        illegalMethods = Collections.unmodifiableMap(s);
    }

    private static final Set<String> illegalAssignmentClasses;
    static {
        final Set<String> s = new HashSet<>();
        s.add("java.lang.System");
        s.add("java.lang.Runtime");
        s.add("java.lang.ProcessBuilder");
        s.add("java.lang.Class");
        illegalAssignmentClasses = Collections.unmodifiableSet(s);
    }

    private static final Map<String, Collection<String>> illegalProperties;
    static {
        final Map<String, Collection<String>> m = new HashMap<>();
        Set<String> s = Collections.unmodifiableSet(new HashSet<>(Arrays.asList("java.lang.System", "java.lang.Class")));
        m.put("methods", s);
        illegalProperties = Collections.unmodifiableMap(m);
    }

    private GroovyUpdaterClassLoader(final ClassLoader classLoader, final CompilerConfiguration compilerConfiguration) {
        super(classLoader, compilerConfiguration);
    }

    public static GroovyUpdaterClassLoader createClassLoader() {
        return new GroovyUpdaterClassLoader(GroovyUpdaterClassLoader.class.getClassLoader(), createCompilerConfiguration());
    }

    private static CompilerConfiguration createCompilerConfiguration() {
        final CompilerConfiguration compilerConfiguration = new CompilerConfiguration();
        compilerConfiguration.addCompilationCustomizers(createImportCustomizer(), createCompilationCustomizer());
        return compilerConfiguration;
    }

    private static CompilationCustomizer createImportCustomizer() {
        final ImportCustomizer importCustomizer = new ImportCustomizer();
        importCustomizer.addStarImports(defaultImports);
        return importCustomizer;
    }

    /**
     * Creates a CompilationCustomizer which (only) checks and prevents obvious and trivial mistakes and misuse
     * of the full power of Groovy.
     * <p>
     * Certain typical class and package imports like java.io and java(x).net, java.lang.reflect etc. are blacklisted,
     * and direct usage of dangerous classes and methods like System, System.exit(), Runtime or ProcessBuilder are
     * also prevented.
     * <p>
     * These checks are however only on first/surface level, <em>not</em> assumed or even intended as a full blown and
     * trustable security sandbox.</p>
     * <p>It must be expected and anticipated that proper securing against misuse of the capabilities of Groovy
     * in the context of Groovy Updater scripts, relies on the usage and access to those scripts, e.g. limited to
     * trusted developers and administrators only, not in sandboxing the capabilities of the scripts themselves.
     * </p>
     * @return a custom Groovy CompilationCustomizer which (only) checks and prevents obvious and trivial mistakes and
     * misuse of the full power of Groovy.
     */
    private static CompilationCustomizer createCompilationCustomizer() {
        final SecureASTCustomizer compilationCustomizer = new SecureASTCustomizer();
        compilationCustomizer.setImportsBlacklist(Arrays.asList(importsBlacklist));
        compilationCustomizer.setStarImportsBlacklist(Arrays.asList(starImportsBlacklist));
        compilationCustomizer.setIndirectImportCheckEnabled(true);
        compilationCustomizer.addExpressionCheckers(new UpdaterExpressionChecker());
        return compilationCustomizer;
    }

    private static final class UpdaterExpressionChecker implements SecureASTCustomizer.ExpressionChecker {

        @Override
        public boolean isAuthorized(final Expression expression) {
            if (expression instanceof MethodCallExpression) {
                final Expression objectExpression = ((MethodCallExpression) expression).getObjectExpression();
                if (objectExpression instanceof ClassExpression) {
                    if (illegalClasses.contains(objectExpression.getType().getName())) {
                        return false;
                    }
                    if (illegalMethods.containsKey(objectExpression.getType().getName())) {
                        if (illegalMethods.get(objectExpression.getType().getName()).contains(((MethodCallExpression) expression).getMethodAsString())) {
                            return false;
                        }
                    }
                }
            }
            if (expression instanceof ConstructorCallExpression) {
                if (illegalClasses.contains(expression.getType().getName())) {
                    return false;
                }
            }
            if (expression instanceof DeclarationExpression) {
                DeclarationExpression declarationExpression = (DeclarationExpression) expression;
                if (declarationExpression.getOperation().getType() == Types.ASSIGN) {
                    Expression rightExpression = declarationExpression.getRightExpression();
                    if (rightExpression instanceof ClassExpression) {
                        if (illegalAssignmentClasses.contains(rightExpression.getType().getName())) {
                            return false;
                        }
                    }
                }
            }
            if (expression instanceof PropertyExpression) {
                PropertyExpression propertyExpression = (PropertyExpression) expression;
                if (illegalProperties.containsKey(propertyExpression.getPropertyAsString())) {
                    Collection<String> classes = illegalProperties.get(propertyExpression.getPropertyAsString());
                    if (classes.contains(propertyExpression.getObjectExpression().getType().getName())) {
                        return false;
                    }
                }
            }
            return true;
        }
    }
}
