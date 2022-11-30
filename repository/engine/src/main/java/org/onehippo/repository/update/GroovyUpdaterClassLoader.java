/*
 *  Copyright 2012-2022 Hippo B.V. (http://www.onehippo.com)
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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import groovy.lang.GroovyClassLoader;
import static java.util.Collections.unmodifiableList;

/**
 * {@link GroovyClassLoader} that is configured for updaters
 * <p>
 * While this custom Groovy ClassLoader protects against certain obvious and trivial mistakes and misuse of the Groovy
 * language, but is not assumed or even intended to provide a full blown and trusted Groovy execution sandbox.
 * See for more information: {@link #createDefaultCompilationCustomizer()}.
 * </p>
 */
public class GroovyUpdaterClassLoader extends GroovyClassLoader {

    public static final Logger log = LoggerFactory.getLogger(GroovyUpdaterClassLoader.class.getName());

    public static final List<String> defaultStarImports = unmodifiableList(
            Stream.of("org.onehippo.repository.update", "javax.jcr", "javax.jcr.nodetype",
                    "javax.jcr.security", "javax.jcr.version", "org.slf4j")
            .collect(Collectors.toList()));

    public static final List<String> defaultImportsBlocklist = unmodifiableList(
            Stream.of("java.io.File", "java.io.FileDescriptor", "java.io.FileInputStream",
            "java.io.FileOutputStream", "java.io.FileWriter", "java.io.FileReader")
                    .collect(Collectors.toList()));

    public static final List<String> defaultStarImportsBlocklist = unmodifiableList(
            Stream.of("java.nio.file", "java.net", "javax.net", "javax.net.ssl", "java.lang.reflect")
                    .collect(Collectors.toList()));

    public static final Set<String> defaultIllegalClasses;
    static {
        final Set<String> s = new HashSet<>();
        s.add("java.lang.Runtime");
        s.add("java.lang.ProcessBuilder");
        defaultIllegalClasses = Collections.unmodifiableSet(s);
    }

    public static final Map<String, Collection<String>> defaultIllegalMethods;
    static {
        final Map<String, Collection<String>> s = new HashMap<>();
        s.put("java.lang.System", new HashSet<>(Arrays.asList("exit")));
        s.put("java.lang.Class", new HashSet<>(Arrays.asList("forName")));
        defaultIllegalMethods = Collections.unmodifiableMap(s);
    }

    public static final Set<String> defaultIllegalAssignmentClasses;
    static {
        final Set<String> s = new HashSet<>();
        s.add("java.lang.System");
        s.add("java.lang.Runtime");
        s.add("java.lang.ProcessBuilder");
        s.add("java.lang.Class");
        defaultIllegalAssignmentClasses = Collections.unmodifiableSet(s);
    }

    public static final Map<String, Collection<String>> defaultIllegalProperties;
    static {
        final Map<String, Collection<String>> m = new HashMap<>();
        Set<String> s = Collections.unmodifiableSet(new HashSet<>(Arrays.asList("java.lang.System", "java.lang.Class")));
        m.put("methods", s);
        defaultIllegalProperties = Collections.unmodifiableMap(m);
    }

    private GroovyUpdaterClassLoader(final ClassLoader classLoader, final CompilerConfiguration compilerConfiguration) {
        super(classLoader, compilerConfiguration);
    }

    public static GroovyUpdaterClassLoader createClassLoader() {
        return new GroovyUpdaterClassLoader(GroovyUpdaterClassLoader.class.getClassLoader(), createCompilerConfiguration());
    }

    private static CompilerConfiguration createCompilerConfiguration() {
        final CompilerConfiguration compilerConfiguration = new CompilerConfiguration();
        compilerConfiguration.addCompilationCustomizers(createCompilationCustomizers());
        return compilerConfiguration;
    }

    private static CompilationCustomizer[] createCompilationCustomizers() {
        final CompilationCustomizerFactory service = HippoServiceRegistry.getService(CompilationCustomizerFactory.class);
        if (service != null) {
            log.debug("Using custom CompilationCustomizerFactory '{}' for creating compilation compiler(s)", service);
            return service.createCompilationCustomizers();
        }
        return new CompilationCustomizer[]{createDefaultImportCustomizer(), createDefaultCompilationCustomizer()};
    }

    public static CompilationCustomizer createDefaultImportCustomizer() {
        final ImportCustomizer importCustomizer = new ImportCustomizer();
        importCustomizer.addStarImports(defaultStarImports.toArray(new String[0]));
        return importCustomizer;
    }

    /**
     * Creates a CompilationCustomizer which (only) checks and prevents obvious and trivial mistakes and misuse
     * of the full power of Groovy.
     * <p>
     * Certain typical class and package imports like java.io and java(x).net, java.lang.reflect etc. are blocked,
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
    public static CompilationCustomizer createDefaultCompilationCustomizer() {
        final SecureASTCustomizer compilationCustomizer = new SecureASTCustomizer();
        compilationCustomizer.setImportsBlacklist(defaultImportsBlocklist);
        compilationCustomizer.setStarImportsBlacklist(defaultStarImportsBlocklist);
        compilationCustomizer.setIndirectImportCheckEnabled(true);
        compilationCustomizer.addExpressionCheckers(new DefaultUpdaterExpressionChecker());
        return compilationCustomizer;
    }

    public static final class DefaultUpdaterExpressionChecker implements SecureASTCustomizer.ExpressionChecker {

        @Override
        public boolean isAuthorized(final Expression expression) {
            if (expression instanceof MethodCallExpression) {
                final Expression objectExpression = ((MethodCallExpression) expression).getObjectExpression();
                if (objectExpression instanceof ClassExpression) {
                    if (defaultIllegalClasses.contains(objectExpression.getType().getName())) {
                        return false;
                    }
                    if (defaultIllegalMethods.containsKey(objectExpression.getType().getName())) {
                        if (defaultIllegalMethods.get(objectExpression.getType().getName()).contains(((MethodCallExpression) expression).getMethodAsString())) {
                            return false;
                        }
                    }
                }
            }
            if (expression instanceof ConstructorCallExpression) {
                if (defaultIllegalClasses.contains(expression.getType().getName())) {
                    return false;
                }
            }
            if (expression instanceof DeclarationExpression) {
                DeclarationExpression declarationExpression = (DeclarationExpression) expression;
                if (declarationExpression.getOperation().getType() == Types.ASSIGN) {
                    Expression rightExpression = declarationExpression.getRightExpression();
                    if (rightExpression instanceof ClassExpression) {
                        if (defaultIllegalAssignmentClasses.contains(rightExpression.getType().getName())) {
                            return false;
                        }
                    }
                }
            }
            if (expression instanceof PropertyExpression) {
                PropertyExpression propertyExpression = (PropertyExpression) expression;
                if (defaultIllegalProperties.containsKey(propertyExpression.getPropertyAsString())) {
                    Collection<String> classes = defaultIllegalProperties.get(propertyExpression.getPropertyAsString());
                    if (classes.contains(propertyExpression.getObjectExpression().getType().getName())) {
                        return false;
                    }
                }
            }
            return true;
        }
    }
}
