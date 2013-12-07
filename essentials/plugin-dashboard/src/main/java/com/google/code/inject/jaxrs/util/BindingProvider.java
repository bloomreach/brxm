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

import static com.google.inject.internal.util.$Preconditions.checkArgument;
import static com.google.inject.internal.util.$Preconditions.checkState;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Map;

import com.google.inject.Binder;
import com.google.inject.Binding;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.ProvisionException;
import com.google.inject.Scope;
import com.google.inject.spi.ConstructorBinding;
import com.google.inject.spi.DefaultBindingTargetVisitor;
import com.google.inject.spi.LinkedKeyBinding;
import com.google.inject.spi.ProviderBinding;
import com.google.inject.spi.UntargettedBinding;

public class BindingProvider<T> {

	private final Key<T> key;
	private Class<?> actualType;
	private Scope scope;

	public BindingProvider(Key<T> key) {
		this.key = key;
	}

	public Class<?> getActualType() {
		return actualType;
	}

	public Key<T> getKey() {
		return key;
	}

	public Scope getScope() {
		checkState(null != scope, "Scope was not resolved");
		return scope;
	}

	protected void setBinding(final Binding<? extends T> binding,
			final Map<Class<? extends Annotation>, Scope> scopeBindings) {
		this.scope = ScopeUtils.scopeOfBinding(binding, scopeBindings);

		this.actualType = binding
				.acceptTargetVisitor(new DefaultBindingTargetVisitor<T, Class<?>>() {
					@Override
					public Class<?> visit(ConstructorBinding<? extends T> b) {
						return b.getConstructor().getDeclaringType()
								.getRawType();
					}

					@Override
					public Class<?> visit(LinkedKeyBinding<? extends T> b) {
						return b.getLinkedKey().getTypeLiteral().getRawType();
					}

					@Override
					public Class<?> visit(
							ProviderBinding<? extends T> providerBinding) {
						try {
							// FIXME this will incorrectly return any first type variable,
							// not necessarily that of Provider<T>
							final Class<?> bound = getFirstTypeArgumentUpperBound(providerBinding
									.getProvider().getClass());

							checkArgument(key.getTypeLiteral().getRawType()
									.isAssignableFrom(bound));

							return bound;
						} catch (final RuntimeException e) {
							throw new ProvisionException(
									"Unable to resolve target class for " + key,
									e);
						}
					}

					public Class<?> visit(UntargettedBinding<? extends T> b) {
						return b.getKey().getTypeLiteral().getRawType();
					}

					@Override
					protected Class<?> visitOther(Binding<? extends T> binding) {
						throw new ProvisionException(
								"Unable to resolve target class for " + binding);
					}
				});
	}

	private static Class<?> getFirstTypeArgumentUpperBound(final Class<?> type) {
		final TypeVariable<?> providerType = type.getTypeParameters()[0];

		final Type[] bounds = providerType.getBounds();
		checkArgument(bounds.length == 1);

		final Class<?> bound = (Class<?>) bounds[0];

		return bound;
	}

	@Inject
	void setInjector(final Injector injector) {
		setBinding(injector.getBinding(key), injector.getScopeBindings());
	}

	public static <T> BindingProvider<T> provideBinding(final Binder binder,
			final Key<T> key) {
		final BindingProvider<T> binding = new BindingProvider<T>(key);
		binder.requestInjection(binding);

		binder.bind(new ParametrizedType(BindingProvider.class) {

			public Type[] getActualTypeArguments() {
				return new Type[] { key.getTypeLiteral().getType() };
			}
		}.asKey()).toInstance(binding);

		return binding;
	}
}
