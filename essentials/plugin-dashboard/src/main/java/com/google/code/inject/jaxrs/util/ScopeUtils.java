/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
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
package com.google.code.inject.jaxrs.util;

import java.lang.annotation.Annotation;
import java.util.Map;

import com.google.inject.Binding;
import com.google.inject.Injector;
import com.google.inject.Provider;
import com.google.inject.Scope;
import com.google.inject.Singleton;
import com.google.inject.internal.CircularDependencyProxy;
import com.google.inject.internal.LinkedBindingImpl;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.spi.BindingScopingVisitor;
import com.google.inject.spi.ConvertedConstantBinding;
import com.google.inject.spi.DefaultBindingScopingVisitor;
import com.google.inject.spi.ExposedBinding;
import com.google.inject.spi.ProviderInstanceBinding;
import com.google.inject.util.Providers;

import static com.google.inject.Scopes.NO_SCOPE;
import static com.google.inject.Scopes.SINGLETON;

public final  class ScopeUtils {

    private ScopeUtils() {
    }

    public static final Class<?> PROVIDERS_OF_CLASS = Providers.of(null)
            .getClass();

    private static final BindingScopingVisitor<Boolean> IS_SINGLETON_VISITOR = new BindingScopingVisitor<Boolean>() {
        public Boolean visitNoScoping() {
            return false;
        }

        public Boolean visitScopeAnnotation(
                Class<? extends Annotation> scopeAnnotation) {
            return scopeAnnotation == Singleton.class
                    || scopeAnnotation == javax.inject.Singleton.class;
        }

        public Boolean visitScope(Scope scope) {
            return scope == com.google.inject.Scopes.SINGLETON;
        }

        public Boolean visitEagerSingleton() {
            return true;
        }
    };

    /**
     * Returns true if {@code binding} is singleton-scoped. If the binding is a
     * {@link com.google.inject.spi.LinkedKeyBinding linked key binding} and
     * belongs to an injector (ie. it was retrieved via
     * {@link com.google.inject.Injector#getBinding Injector.getBinding()}), then this method will
     * also true if the target binding is singleton-scoped.
     *
     * @since 3.0
     */
    public static boolean isSingleton(Binding<?> binding) {
        do {
            final boolean singleton = binding
                    .acceptScopingVisitor(IS_SINGLETON_VISITOR);
            if (singleton) {
                return true;
            }

            if (binding instanceof LinkedBindingImpl) {
                final LinkedBindingImpl<?> linkedBinding = (LinkedBindingImpl<?>) binding;
                final Injector injector = linkedBinding.getInjector();
                if (injector != null) {
                    binding = injector.getBinding(linkedBinding.getLinkedKey());
                    continue;
                }
            } else if (binding instanceof ExposedBinding) {
                final ExposedBinding<?> exposedBinding = (ExposedBinding<?>) binding;
                final Injector injector = exposedBinding.getPrivateElements()
                        .getInjector();
                if (injector != null) {
                    binding = injector.getBinding(exposedBinding.getKey());
                    continue;
                }
            }

            return false;
        } while (true);
    }

    /**
     * Returns true if {@code binding} has the given scope. If the binding is a
     * {@link com.google.inject.spi.LinkedKeyBinding linked key binding} and
     * belongs to an injector (ie. it was retrieved via
     * {@link com.google.inject.Injector#getBinding Injector.getBinding()}), then this method will
     * also true if the target binding has the given scope.
     *
     * @param binding         binding to check
     * @param scope           scope implementation instance
     * @param scopeAnnotation scope annotation class
     */
    public static boolean isScoped(Binding<?> binding, final Scope scope,
                                   final Class<? extends Annotation> scopeAnnotation) {
        do {
            final boolean matches = binding
                    .acceptScopingVisitor(new BindingScopingVisitor<Boolean>() {
                        public Boolean visitNoScoping() {
                            return false;
                        }

                        public Boolean visitScopeAnnotation(
                                Class<? extends Annotation> visitedAnnotation) {
                            return visitedAnnotation == scopeAnnotation;
                        }

                        public Boolean visitScope(Scope visitedScope) {
                            return visitedScope == scope;
                        }

                        public Boolean visitEagerSingleton() {
                            return false;
                        }
                    });

            if (matches) {
                return true;
            }

            if (binding instanceof LinkedBindingImpl) {
                final LinkedBindingImpl<?> linkedBinding = (LinkedBindingImpl<?>) binding;
                final Injector injector = linkedBinding.getInjector();
                if (injector != null) {
                    binding = injector.getBinding(linkedBinding.getLinkedKey());
                    continue;
                }
            } else if (binding instanceof ExposedBinding) {
                final ExposedBinding<?> exposedBinding = (ExposedBinding<?>) binding;
                final Injector injector = exposedBinding.getPrivateElements()
                        .getInjector();
                if (injector != null) {
                    binding = injector.getBinding(exposedBinding.getKey());
                    continue;
                }
            }

            return false;
        } while (true);
    }

    /**
     * Returns true if the object is a proxy for a circular dependency,
     * constructed by Guice because it encountered a circular dependency. Scope
     * implementations should be careful to <b>not cache circular proxies</b>,
     * because the proxies are not intended for general purpose use. (They are
     * designed just to fulfill the immediate injection, not all injections.
     * Caching them can lead to IllegalArgumentExceptions or
     * ClassCastExceptions.)
     */
    public static boolean isCircularProxy(Object object) {
        return object instanceof CircularDependencyProxy;
    }

    public static Scope scopeOfBinding(final Binding<?> binding,
                                       final Map<Class<? extends Annotation>, Scope> scopeBindings) {

        if (binding instanceof ProviderInstanceBinding<?>) {
            final Provider<?> providerInstance = ((ProviderInstanceBinding<?>) binding)
                    .getProviderInstance();

            if (providerInstance instanceof Multibinder) {
                // multibinder scope is effectively equal to the scope of it's member bindings
                return null;
            }

            if (PROVIDERS_OF_CLASS.equals(providerInstance.getClass())) {
                return SINGLETON;
            }

        }

        if (binding instanceof ConvertedConstantBinding) {
            return SINGLETON;
        }

        return binding
                .acceptScopingVisitor(new DefaultBindingScopingVisitor<Scope>() {
                    @Override
                    public Scope visitEagerSingleton() {
                        return SINGLETON;
                    }

                    @Override
                    public Scope visitNoScoping() {
                        return NO_SCOPE;
                    }

                    @Override
                    protected Scope visitOther() {
                        return null;
                    }

                    @Override
                    public Scope visitScope(Scope scope) {
                        return scope;
                    }

                    @Override
                    public Scope visitScopeAnnotation(
                            Class<? extends Annotation> scopeAnnotation) {
                        return scopeBindings.get(scopeAnnotation);
                    }
                });
    }
}
