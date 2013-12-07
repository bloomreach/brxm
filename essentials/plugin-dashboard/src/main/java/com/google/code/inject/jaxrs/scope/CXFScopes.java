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
package com.google.code.inject.jaxrs.scope;

import static com.google.code.inject.jaxrs.scope.CXFScopes.Marker.NULL;
import static com.google.code.inject.jaxrs.scope.GuiceInterceptorWrapper.getExchange;
import static com.google.code.inject.jaxrs.util.ScopeUtils.isCircularProxy;
import static com.google.inject.internal.util.$Preconditions.checkArgument;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static java.util.Collections.unmodifiableSet;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.Providers;

import org.apache.cxf.jaxrs.ext.MessageContext;
import org.apache.cxf.jaxrs.ext.MessageContextImpl;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.service.Service;

import com.google.code.inject.jaxrs.util.ScopeUtils;
import com.google.inject.AbstractModule;
import com.google.inject.Binding;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.OutOfScopeException;
import com.google.inject.Provider;
import com.google.inject.Provides;
import com.google.inject.Scope;
import com.google.inject.ScopeAnnotation;

public class CXFScopes {
	/**
	 * Binds the scope annotation and provides @Context instances
	 */
	public static class Module extends AbstractModule {

		@SuppressWarnings("unchecked")
		@Override
		protected void configure() {
			bindScope(RequestScope.class, REQUEST);

			for (final Key<?> key : EXCHANGE_CONTEXT_KEYS) {
				bind(key).toProvider(DUMMY_PROVIDER).in(REQUEST);
			}

		}

		@Provides
		@RequestScope
		protected MessageContext provideMessageContext(Exchange ex) {
			return new MessageContextImpl(ex.getInMessage());
		}

		@Provides
		@RequestScope
		protected Service provideService(MessageContext messageContext) {
			return (Service) messageContext
					.getContextualProperty(Service.class);
		}

		@Provides
		@RequestScope
		protected HttpHeaders provideHttpHeaders(MessageContext messageContext) {
			return messageContext.getHttpHeaders();
		}

		@Provides
		@RequestScope
		protected Providers provideProviders(MessageContext messageContext) {
			return messageContext.getProviders();
		}

		@Provides
		@RequestScope
		protected UriInfo provideUriInfo(MessageContext messageContext) {
			return messageContext.getUriInfo();
		}

		@Provides
		@RequestScope
		protected Request provideRequest(MessageContext messageContext) {
			return messageContext.getRequest();
		}

		@Provides
		@RequestScope
		protected SecurityContext provideSecurityContext(
				MessageContext messageContext) {
			return messageContext.getSecurityContext();
		}

	}

	@Target({ TYPE, METHOD })
	@Retention(RUNTIME)
	@ScopeAnnotation
	public @interface RequestScope {
	}

	/**
	 * Dummy marker to bind {@value #EXCHANGE_CONTEXT_KEYS} to
	 */
	@SuppressWarnings("rawtypes")
	private static final Provider DUMMY_PROVIDER = new Provider<Void>() {

		@Override
		public Void get() {
			throw new OutOfScopeException("This should never happen -- check "
					+ CXFScopes.class + ".getKeyFromExchange(Exchange, Key)");
		}
	};

	private CXFScopes() {
	}

	/**
	 * This keys will be retrieved from exchange via
	 * {@link #getKeyFromExchange(org.apache.cxf.message.Exchange, com.google.inject.Key)}
	 */
	private static final Set<Key<?>> EXCHANGE_CONTEXT_KEYS = unmodifiableSet(new HashSet<Key<?>>(
			Arrays.<Key<?>> asList(Key.get(Exchange.class))));

	/** Marker for @Nullable providers */
	enum Marker {
		NULL
	}

	/**
	 * CXF exchange scope.
	 */
	public static final Scope REQUEST = new Scope() {
		public <T> Provider<T> scope(final Key<T> key, final Provider<T> creator) {
			final String name = key.toString();
			if (EXCHANGE_CONTEXT_KEYS.contains(key))
				return new Provider<T>() {
					public T get() {
						final Exchange exchange = getExchange();
						synchronized (exchange) {
							return getKeyFromExchange(exchange, key);
						}
					}

					@Override
					public String toString() {
						return String.format("%s[%s-ex]", creator, REQUEST);
					}
				};
			else
				return new Provider<T>() {
					public T get() {
						final Exchange exchange = getExchange();
						synchronized (exchange) {

							final Object obj = exchange.get(name);
							if (NULL == obj)
								return null;

							@SuppressWarnings("unchecked")
							T t = (T) obj;
							if (t == null) {
								t = creator.get();
								if (!isCircularProxy(t)) {
									exchange.put(name, (t != null) ? t : NULL);
								}
							}

							return t;
						}
					}

					@Override
					public String toString() {
						return String.format("%s[%s]", creator, REQUEST);
					}
				};
		}

		@Override
		public String toString() {
			return "CXFScopes.REQUEST";
		}
	};

	/**
	 * Retrieve an existing instance from exchange.
	 * <p>
	 * This method will be called for all keys in {@link #EXCHANGE_CONTEXT_KEYS}
	 *
	 * @param exchange
	 *            request exchange
	 * @param key
	 *            key to retrieve
	 * @return instance of T, never null
	 */
	private static <T> T getKeyFromExchange(Exchange exchange, Key<T> key) {
		checkArgument(key.getAnnotationType() == null,
				"Annotated keys not allowed");
		final Class<? super T> rt = key.getTypeLiteral().getRawType();

		if (Exchange.class.equals(rt)) {
			@SuppressWarnings("unchecked")
			final T t = (T) exchange;
			return t;
		}

		throw new UnsupportedOperationException("Key " + key);
	}

	/**
	 * Returns true if {@code binding} is exchange-scoped. If the binding is a
	 * {@link com.google.inject.spi.LinkedKeyBinding linked key binding} and
	 * belongs to an injector (i. e. it was retrieved via
	 * {@link com.google.inject.Injector#getBinding Injector.getBinding()}), then this method will
	 * also return true if the target binding is exchange-scoped.
	 */
	public static boolean isRequestScoped(Binding<?> binding) {
		return ScopeUtils.isScoped(binding, REQUEST, RequestScope.class);
	}

}
