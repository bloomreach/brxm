/*
 * Copyright 2012 Jakub Bocheński (kuba.bochenski@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.code.inject.jaxrs.util;

import static com.google.inject.Scopes.NO_SCOPE;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import com.google.inject.AbstractModule;
import com.google.inject.Binding;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.Scope;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.spi.ConstructorBinding;
import com.google.inject.spi.ConvertedConstantBinding;
import com.google.inject.spi.DefaultBindingTargetVisitor;
import com.google.inject.spi.Dependency;
import com.google.inject.spi.ExposedBinding;
import com.google.inject.spi.InstanceBinding;
import com.google.inject.spi.LinkedKeyBinding;
import com.google.inject.spi.ProviderBinding;
import com.google.inject.spi.ProviderInstanceBinding;
import com.google.inject.spi.ProviderKeyBinding;

public class ScopeValidator {

	private static final Key<Injector> INJECTOR_KEY = Key.get(Injector.class);

	private final static Logger logger = Logger.getLogger(ScopeValidator.class
			.getName());

	private static String formatAnnotation(final Annotation annotation) {
		if (annotation == null)
			return "";

		try {
			final Class<?> multibinderElement = Class
					.forName("com.google.inject.multibindings.Element");
			if (multibinderElement.isInstance(annotation)) {
				final Method setName = multibinderElement.getMethod("setName");
				setName.setAccessible(true);
				return (String) setName.invoke(annotation);
			}
		} catch (final Exception e) {
		}

		return annotation.toString();
	}

	private static String formatBinding(Binding<?> binding) {
		final Key<?> key = binding.getKey();
		return key.getTypeLiteral() + formatAnnotation(key.getAnnotation())
				+ " from " + binding.getSource();
	}

	private static boolean isMultibinder(Binding<?> binding) {
		if (binding instanceof ProviderInstanceBinding) {
			final ProviderInstanceBinding<?> pib = (ProviderInstanceBinding<?>) binding;
			return pib.getProviderInstance() instanceof Multibinder;
		}
		return false;

	}

	public static Module scopeValidatorModule(final Scope... scopes) {
		return new AbstractModule() {
			@Override
			protected void configure() {
				bind(ScopeValidator.class).asEagerSingleton();
				bind(new TypeLiteral<List<Scope>>() {
				}).toInstance(asList(scopes));
			}
		};
	}

	private final Map<Class<? extends Annotation>, Scope> scopeBindings;

	private final List<Scope> scopes;

	@Inject
	protected ScopeValidator(List<Scope> scopes, final Injector injector) {
		this.scopes = scopes;
		this.scopeBindings = injector.getScopeBindings();
		for (final Binding<?> binding : injector.getAllBindings().values()) {
			if (isMultibinder(binding))
				//Multibinder is always NO_SCOPE
				continue;

			checkBinding(injector, binding);
		}

	}

	private void checkBinding(final Injector injector, Binding<?> binding) {

		final Iterable<Dependency<?>> dependencies = binding
				.acceptTargetVisitor(new DefaultBindingTargetVisitor<Object, Iterable<Dependency<?>>>() {
					@Override
					public Iterable<Dependency<?>> visit(
							ConstructorBinding<? extends Object> constructorBinding) {
						return constructorBinding.getDependencies();
					}

					@Override
					public Iterable<Dependency<?>> visit(
							ConvertedConstantBinding<? extends Object> convertedConstantBinding) {
						// constants don't have dependencies
						return emptyList();
					}

					@Override
					public Iterable<Dependency<?>> visit(
							InstanceBinding<? extends Object> instanceBinding) {
						return instanceBinding.getDependencies();
					}

					@Override
					public Iterable<Dependency<?>> visit(
							LinkedKeyBinding<? extends Object> linkedKeyBinding) {
						return Collections.<Dependency<?>> singleton(Dependency
								.get(linkedKeyBinding.getLinkedKey()));

					}

					@Override
					public Iterable<Dependency<?>> visit(
							ProviderBinding<? extends Object> providerBinding) {

						return Collections.<Dependency<?>> singleton(Dependency
								.get(providerBinding.getProvidedKey()));
					}

					@Override
					public Iterable<Dependency<?>> visit(
							ProviderInstanceBinding<? extends Object> providerInstanceBinding) {
						return providerInstanceBinding.getDependencies();
					}

					@Override
					public Iterable<Dependency<?>> visit(
							ProviderKeyBinding<? extends Object> providerKeyBinding) {
						return Collections.<Dependency<?>> singleton(Dependency
								.get(providerKeyBinding.getProviderKey()));
					}

					@Override
					public Iterable<Dependency<?>> visit(
							ExposedBinding<? extends Object> b) {
						return b.getDependencies();
					}

					@Override
					protected Iterable<Dependency<?>> visitOther(
							Binding<? extends Object> binding) {
						logger.warning("Unable to resolve dependencies for "
								+ binding);
						return emptyList();
					}
				});

		final Scope scope = ScopeUtils.scopeOfBinding(binding, scopeBindings);
		for (final Dependency<?> dependency : dependencies)
			checkDependency(injector, binding, scope, dependency);
	}

	private void checkDependency(final Injector injector, Binding<?> binding,
			final Scope scope, final Dependency<?> dependency) {

		final Key<?> dependencyKey = dependency.getKey();

		// anything can depend on injector
		if (INJECTOR_KEY.equals(dependencyKey))
			return;

		final Binding<?> dependencyBinding = injector.getBinding(dependencyKey);

		//provider bindings are cross-scope safe
		if (dependencyBinding instanceof ProviderBinding) {
			return;
		}

		// ignore bindings bind(A.class).to(AImpl.class).in(SINGLETON)
		// ignore bindings bind(A.class).toProvider(AProviderImpl.class).in(SINGLETON)
		if (binding instanceof LinkedKeyBinding
				|| binding instanceof ProviderKeyBinding) {
			if (dependencyBinding instanceof ConstructorBinding
					&& NO_SCOPE.equals(ScopeUtils.scopeOfBinding(
							dependencyBinding, scopeBindings))) {
				return;
			}
		}

		// check multibinder dependency as dependency on it's members
		if (dependencyBinding instanceof ProviderInstanceBinding<?>) {
			final ProviderInstanceBinding<?> pib = (ProviderInstanceBinding<?>) dependencyBinding;
			if (isMultibinder(pib)) {
				for (final Dependency<?> multibinderDependency : pib
						.getDependencies())
					checkDependency(injector, binding, scope,
							multibinderDependency);
				return;
			}

		}

		// check dependency on linked key as dependency on its target
		if (dependencyBinding instanceof LinkedKeyBinding) {
			final LinkedKeyBinding<?> lkb = (LinkedKeyBinding<?>) dependencyBinding;
			checkDependency(injector, dependencyBinding, scope,
					Dependency.get(lkb.getLinkedKey()));
			return;
		}

		// this is wrong
		if (dependencyBinding instanceof ExposedBinding) {
			logger.warning("Exposed bindings are not checked.\n"
					+ formatBinding(binding) + " -> "
					+ formatBinding(dependencyBinding));
			final ExposedBinding<?> eb = (ExposedBinding<?>) dependencyBinding;
			for (final Dependency<?> dep : eb.getDependencies()) {
				checkDependency(injector, dependencyBinding, scope, dep);
			}
			return;
		}

		final Scope depencencyScope = ScopeUtils.scopeOfBinding(
				dependencyBinding, scopeBindings);

		if (!checkScopes(scope, depencencyScope)) {
			logger.severe("Illegal dependency " + scope + " -> "
					+ depencencyScope + "\n\t" + formatBinding(binding)
					+ "\n\tdepends on " + formatBinding(dependencyBinding));
		}

	}

	private boolean checkScopes(Scope scope, Scope childScope) {
		final int i1 = scopes.indexOf(scope);
		if (-1 == i1)
			throw new IllegalArgumentException(scope + "");
		final int i2 = scopes.indexOf(childScope);
		if (-1 == i2)
			throw new IllegalArgumentException(scope + "");
		return i1 <= i2;
	}
}
