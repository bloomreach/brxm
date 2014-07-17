/*
 *  Copyright 2012-2013 Hippo B.V. (http://www.onehippo.com)
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

import org.codehaus.groovy.ast.expr.ClassExpression;
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.ast.expr.MethodCallExpression;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.customizers.CompilationCustomizer;
import org.codehaus.groovy.control.customizers.ImportCustomizer;
import org.codehaus.groovy.control.customizers.SecureASTCustomizer;

import groovy.lang.GroovyClassLoader;

/**
 * {@link GroovyClassLoader} that is configured for updaters
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

    private GroovyUpdaterClassLoader(final ClassLoader classLoader, final CompilerConfiguration compilerConfiguration) {
        super(classLoader, compilerConfiguration);
    }

    public static GroovyUpdaterClassLoader createClassLoader() {
        return new GroovyUpdaterClassLoader(GroovyUpdaterClassLoader.class.getClassLoader(), createCompilerConfiguration());
    }

    private static CompilerConfiguration createCompilerConfiguration() {
        final CompilerConfiguration compilerConfiguration = new CompilerConfiguration();
        compilerConfiguration.addCompilationCustomizers(createImportCustomizer(), createSecurityCustomizer());
        return compilerConfiguration;
    }

    private static CompilationCustomizer createImportCustomizer() {
        final ImportCustomizer importCustomizer = new ImportCustomizer();
        importCustomizer.addStarImports(defaultImports);
        return importCustomizer;
    }

    private static CompilationCustomizer createSecurityCustomizer() {
        final SecureASTCustomizer securityCustomizer = new SecureASTCustomizer();
        securityCustomizer.setImportsBlacklist(Arrays.asList(importsBlacklist));
        securityCustomizer.setStarImportsBlacklist(Arrays.asList(starImportsBlacklist));
        securityCustomizer.setIndirectImportCheckEnabled(true);
        securityCustomizer.addExpressionCheckers(new UpdaterExpressionChecker());
        return securityCustomizer;
    }

    private static final class UpdaterExpressionChecker implements SecureASTCustomizer.ExpressionChecker {

        @Override
        public boolean isAuthorized(final Expression expression) {
            if (isSystemExitCall(expression)) {
                return false;
            }
            if (isRuntimeCall(expression)) {
                return false;
            }
            if (isClassForNameCall(expression)) {
                return false;
            }
            return true;
        }

        private boolean isSystemExitCall(final Expression expression) {
            if (expression instanceof MethodCallExpression) {
                final Expression objectExpression = ((MethodCallExpression) expression).getObjectExpression();
                if (objectExpression instanceof ClassExpression) {
                    if (objectExpression.getType().getName().equals(System.class.getName())) {
                        if (((MethodCallExpression) expression).getMethodAsString().equals("exit")) {
                            return true;
                        }
                    }
                }
            }
            return false;
        }

        private boolean isRuntimeCall(final Expression expression) {
            if (expression instanceof MethodCallExpression) {
                final Expression objectExpression = ((MethodCallExpression) expression).getObjectExpression();
                if (objectExpression instanceof ClassExpression) {
                    if (objectExpression.getType().getName().equals(Runtime.class.getName())) {
                        return true;
                    }
                }
            }
            return false;
        }

        private boolean isClassForNameCall(final Expression expression) {
            if (expression instanceof MethodCallExpression) {
                final Expression objectExpression = ((MethodCallExpression) expression).getObjectExpression();
                if (objectExpression instanceof ClassExpression) {
                    if (objectExpression.getType().getName().equals(Class.class.getName())) {
                        if (((MethodCallExpression) expression).getMethodAsString().equals("forName")) {
                            return true;
                        }
                    }
                }
            }
            return false;
        }
    }
}
