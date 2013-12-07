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
package com.google.code.inject.jaxrs;

import static com.google.code.inject.jaxrs.util.BindingProvider.provideBinding;
import static com.google.code.inject.jaxrs.util.Matchers.resourceMethod;
import static com.google.inject.Scopes.SINGLETON;
import static com.google.inject.internal.util.$Preconditions.checkNotNull;
import static com.google.inject.internal.util.$Preconditions.checkState;
import static com.google.inject.matcher.Matchers.any;
import static com.google.inject.multibindings.Multibinder.newSetBinder;
import static com.google.inject.name.Names.named;

import java.lang.reflect.Type;

import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;

import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.cxf.jaxrs.ext.RequestHandler;
import org.apache.cxf.jaxrs.ext.ResponseHandler;
import org.apache.cxf.jaxrs.lifecycle.ResourceProvider;
import org.apache.cxf.service.invoker.Invoker;

import com.google.code.inject.jaxrs.internal.DefaultInvoker;
import com.google.code.inject.jaxrs.internal.JaxRsProvider;
import com.google.code.inject.jaxrs.internal.SubresourceInterceptor;
import com.google.code.inject.jaxrs.scope.CXFScopes;
import com.google.code.inject.jaxrs.scope.GuiceInterceptorWrapper;
import com.google.code.inject.jaxrs.util.ParametrizedType;
import com.google.inject.Binder;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.Multibinder;

/**
 * CXF EDSL Module.
 * <p>
 *
 * <b>Example usage:</b>
 *
 * <pre>
 * protected void configureResources() {
 * 	serve().atAddress(&quot;/rest&quot;);
 *
 * 	publish(MyResource.class);
 *
 * 	readAndWriteBody(JAXBElementProvider.class);
 * 	readAndWriteBody(JSONProvider.class);
 *
 * 	mapExceptions(ApplicationExceptionMapper.class);
 * }
 * </pre>
 *
 * then do
 *
 * <pre>
 * injector.getInstance(JAXRSServerFactoryBean.class).create();
 * </pre>
 *
 * <p>
 *
 * <h3>Language elements</h3>
 * <p>
 * Use <tt>publish()</tt> to register a resource class - a custom
 * <tt>ResourceProvider</tt> will be bound for each resource class. It's a
 * 'per-exchange' type and will get a new instance for each incoming exchange.
 * </p>
 * <p>
 * Use <tt>serve()</tt> to configure server, e.g. set the root address.
 * </p>
 * <p>
 * The following methods let you register JAX-RS <tt>@Provider</tt>s:
 * </p>
 * <ul>
 * <li><tt>handleRequest()</tt> - register a <tt>RequestHandler</tt>;</li>
 * <li><tt>handleResponse()</tt> - register a <tt>ResponseHandler</tt>;</li>
 * <li><tt>mapExceptions()</tt> - register an <tt>ExceptionMapper</tt>;</li>
 * <li><tt>readBody()</tt> - register a <tt>MessageBodyReader</tt>;</li>
 * <li><tt>writeBody()</tt> - register a <tt>MessageBodyWriter</tt>;</li>
 * <li><tt>writeAndReadBody()</tt> - register a class that is both a
 * <tt>MessageBodyReader</tt> and a <tt>MessageBodyWriter</tt> (e.g.
 * <tt>JAXBElementProvider</tt> or <tt>JSONProvider</tt>);</li>
 * <li><tt>provide()</tt> - generic method to register any JAX-RS
 * <tt>@Provider</tt>, it's best to use specific methods if available, since
 * they are type safe;</li>
 * </ul>
 * <p>
 * Above methods return a standard <tt>ScopedBindingBuilder</tt> so that you can
 * use all the regular Guice constructs.
 * </p>
 * <p>
 * <i>Please note that a single instance of each <tt>@Provider</tt> class will
 * be passed to the <tt>ServerFactoryBean</tt>, regardless of the scope.</i>
 * </p>
 * <p>
 * Use <tt>invokeVia()</tt> to register custom invoker.
 * </p>
 * <h3>Binding resources and providers</h3>
 * <p>
 * It is possible to bind concrete classes, but a very nice feature is the
 * ability to bind interfaces.
 * </p>
 *
 * <pre>
 * protected void configureResources() {
 * 	publish(ResourceInterface.class);
 * }
 * </pre>
 * <p>
 * Then in a separate module you can do something like
 * <tt>bind(ResourceInterface.class).to(ResourceImpl.class)</tt> to define the
 * concrete implementation. This let's you easily register mock objects for
 * testing.
 * </p>
 * <p>
 * Another use of indirect binding is configuring the <tt>@Provider</tt>s, see
 * here an example configuration of the <tt>JSONProvider</tt>.
 * </p>
 *
 * <pre>
 * &#064;Provides
 * public JSONProvider provdeJsonProvider(
 * 		&#064;Named(&quot;ignoreNamespaces&quot;) boolean ignoreNamespaces) {
 * 	final JSONProvider json = new JSONProvider();
 * 	json.setIgnoreNamespaces(ignoreNamespaces);
 * 	return json;
 * }
 * </pre>
 * <p>
 * <i>Of course if you implement your own <tt>@Provider</tt>s it's best to use
 * constructor/method injections directly on them.</i>
 *
 * <h3>Bindings</h3>
 *
 * The most important binding provided by the CXFServerModule is the
 * <tt>JAXRSServerFactoryBean</tt>. You can use it to easily create a server by
 * doing <tt>injector.getInstance(JAXRSServerFactoryBean.class).create()</tt>
 * </p>
 * <p>
 * A set of <tt>ResourceProvider</tt>s will be bound using the multibinder. For
 * each resource class a <tt>{@link GuicePerRequestResourceProvider}</tt>
 * parametrized with the resource type will be registered.
 * </p>
 * <p>
 * A <tt>Set&lt;Object&gt;</tt> annotated with <tt>{@link com.google.code.inject.jaxrs.internal.JaxRsProvider}</tt>
 * will be bound containting an instance of each registered JAX-RS Provider
 * </p>
 * <p>
 * A singleton instance of <tt>{@link com.google.code.inject.jaxrs.CXFServerModule.ServerConfiguration}</tt> will be bound
 * with configuration options.
 * </p>
 * <p>
 * If a custom <tt>Invoker</tt> was configured it will be bound, otherwise a
 * <tt>{@link com.google.code.inject.jaxrs.internal.DefaultInvoker}</tt> will be bound.
 * </p>
 * <p>
 * With the exception of <tt>{@link com.google.code.inject.jaxrs.CXFServerModule.ServerConfiguration}</tt> bean no instances
 * of business classes are created during binding.
 * </p>
 */
public abstract class CXFServerModule implements Module {

	protected final class InterceptorBuilder {
		private String direction;

		public InterceptorBuilder inMessages() {
			setDirection(DIRECTION_IN);
			return this;
		}

		public InterceptorBuilder outMessages() {
			setDirection(DIRECTION_OUT);
			return this;
		}

		private void setDirection(String string) {
			checkState(null == this.direction, "Direction already set");
			this.direction = string;
		}

		public void with(Class<? extends Interceptor<?>> type) {
			with(Key.get(type));
		}

		public void with(Key<? extends Interceptor<?>> key) {
			checkState(null != direction, "Direction must be set");

			if (DIRECTION_IN.equals(direction))
				inInterceptors.addBinding().to(key);
			else if (DIRECTION_OUT.equals(direction))
				outInterceptors.addBinding().to(key);
		}

		public void with(TypeLiteral<? extends Interceptor<?>> type) {
			with(Key.get(type));
		}

	}

	private final class ServerConfig implements ServerConfiguration,
			ServerConfigurationBuilder {
		private String address = "/";
		private boolean staticResourceResolution = false;
		private boolean scopesEnabled = false;
		private boolean subinjectionEnabled = false;

		@Override
		public ServerConfigurationBuilder atAddress(String address) {
			this.address = address;
			return this;
		}

		@Override
		public ServerConfigurationBuilder enableCustomScopes() {
			checkState(!scopesEnabled, "Custom scopes already enabled");
			binder().install(new CXFScopes.Module());
			inInterceptors.addBinding().to(GuiceInterceptorWrapper.class);
			scopesEnabled = true;
			return this;
		}

		@Override
		public String getAddress() {
			return address;
		}

		@Override
		public boolean isStaticResourceResolution() {
			return staticResourceResolution;
		}

		@Override
		public ServerConfigurationBuilder withStaticResourceResolution() {
			this.staticResourceResolution = true;
			return this;
		}

		public ServerConfigurationBuilder withSubresourcesInjection() {
			checkState(!subinjectionEnabled,
					"Subresource injection already enabled");
			final SubresourceInterceptor interceptor = new SubresourceInterceptor();
			binder().bindInterceptor(any(), resourceMethod(Injected.class),
					interceptor);

			binder().requestInjection(interceptor);
			subinjectionEnabled = true;
			return enableCustomScopes();
		}
	}

	/**
	 * Server config bean
	 */
	public interface ServerConfiguration {

		String getAddress();

		boolean isStaticResourceResolution();

	}

	/**
	 * Server config builder
	 */
	protected interface ServerConfigurationBuilder {

		/**
		 * Server root address. Defaults to "/".
		 *
		 * @param address
		 *            root address
		 * @return self
		 */
		ServerConfigurationBuilder atAddress(String address);

		/**
		 * Enable CXF-specifix scopes
		 *
		 * @return self
		 */
		ServerConfigurationBuilder enableCustomScopes();

		/**
		 * Use static resource resolution
		 *
		 * @return self
		 */
		ServerConfigurationBuilder withStaticResourceResolution();

		/**
		 * Enable AOP based sub-resource injection.
		 * <p>
		 * Implies {@link #enableCustomScopes()}
		 *
		 * @return self
		 */
		ServerConfigurationBuilder withSubresourcesInjection();

	}

	static final String DIRECTION_IN = "in";
	static final String DIRECTION_OUT = "out";

	private ServerConfig config;

	private boolean customInvoker;

	private Multibinder<Interceptor<?>> inInterceptors;
	private Multibinder<Interceptor<?>> outInterceptors;

	private Multibinder<Object> providers;

	private Multibinder<ResourceProvider> resourceProviders;
	private Binder binder;

	private Binder binder() {
		return binder;
	}

	@Override
	public final void configure(Binder binder) {
		checkState(this.binder == null, "Re-entry is not allowed.");
		checkState(this.resourceProviders == null, "Re-entry is not allowed.");
		checkState(this.inInterceptors == null, "Re-entry is not allowed.");
		checkState(this.outInterceptors == null, "Re-entry is not allowed.");
		checkState(this.providers == null, "Re-entry is not allowed.");
		checkState(this.config == null, "Re-entry is not allowed.");

		this.binder = binder.skipSources(CXFServerModule.class);

		resourceProviders = newSetBinder(binder(), ResourceProvider.class);
		inInterceptors = newSetBinder(binder(),
				new TypeLiteral<Interceptor<?>>() {
				}, named(DIRECTION_IN));
		outInterceptors = newSetBinder(binder(),
				new TypeLiteral<Interceptor<?>>() {
				}, named(DIRECTION_OUT));
		providers = newSetBinder(binder(), Object.class, JaxRsProvider.class);

		config = new ServerConfig();
		customInvoker = false;

		try {
			configure();

			binder().bind(ServerConfiguration.class).toInstance(config);
			binder().bind(JAXRSServerFactoryBean.class)
					.toProvider(JaxRsServerFactoryBeanProvider.class)
					.in(Singleton.class);

			if (!customInvoker)
				binder().bind(Invoker.class).to(DefaultInvoker.class)
						.in(SINGLETON);

		} finally {
			binder = null;
			resourceProviders = null;
			inInterceptors = null;
			outInterceptors = null;
			outInterceptors = null;
			providers = null;
			config = null;
		}
	}

	/**
	 * Override this method to configure CXF
	 */
	protected abstract void configure();

	protected final void handleRequest(Class<? extends RequestHandler> type) {
		provide(type);
	}

	protected final void handleRequest(Key<? extends RequestHandler> key) {
		provide(key);
	}

	protected final void handleRequest(
			TypeLiteral<? extends RequestHandler> type) {
		provide(type);
	}

	protected final void handleResponse(Class<? extends ResponseHandler> type) {
		provide(type);
	}

	protected final void handleResponse(Key<? extends ResponseHandler> key) {
		provide(key);
	}

	protected final void handleResponse(
			TypeLiteral<? extends ResponseHandler> type) {
		provide(type);
	}

	protected final InterceptorBuilder intercept() {
		return new InterceptorBuilder();
	}

	/**
	 * Bind custom invoker
	 *
	 * @param type
	 *            type to bind
	 * @return binding builder for the invoker
	 */
	protected final void invokeVia(Class<? extends Invoker> type) {
		invokeVia(Key.get(type));
	}

	/**
	 * Bind custom invoker
	 *
	 * @param type
	 *            type to bind
	 * @return binding builder for the invoker
	 */
	protected final void invokeVia(Key<? extends Invoker> type) {
		checkState(!customInvoker, "Custom invoker bound twice");
		this.customInvoker = true;
		binder().bind(Invoker.class).to(type).in(SINGLETON);
	}

	/**
	 * Bind custom invoker
	 *
	 * @param type
	 *            type to bind
	 * @return binding builder for the invoker
	 */
	protected final void invokeVia(TypeLiteral<? extends Invoker> type) {
		invokeVia(Key.get(type));
	}

	protected final void mapExceptions(Class<? extends ExceptionMapper<?>> type) {
		provide(type);
	}

	protected final void mapExceptions(Key<? extends ExceptionMapper<?>> key) {
		provide(key);
	}

	protected final void mapExceptions(
			TypeLiteral<? extends ExceptionMapper<?>> type) {
		provide(type);
	}

	protected final void provide(Class<?> type) {
		provide(Key.get(type));
	}

	protected final void provide(Key<?> key) {
		providers.addBinding().to(key).in(SINGLETON);
	}

	protected final void provide(TypeLiteral<?> type) {
		provide(Key.get(type));
	}

	/**
	 * Bind a resource class
	 *
	 * @param resourceKey
	 *            to bind
	 */
	private final <T> void publish(final Key<T> resourceKey) {
		checkNotNull(resourceKey);
		final Type[] arguments = new Type[] { resourceKey.getTypeLiteral()
				.getType() };

		final Key<? extends ResourceProvider> providerKey = new ParametrizedType(
				GuicePerRequestResourceProvider.class) {

			public Type[] getActualTypeArguments() {
				return arguments;
			}
		}.asKey();

		resourceProviders.addBinding().to(providerKey).in(Singleton.class);

		provideBinding(binder(), resourceKey);
	}

	/**
	 * Bind a resource class
	 *
	 * @param type
	 *            to bind
	 */
	protected final void publish(final Type type) {
		checkNotNull(type);
		publish(Key.get(type));
	}

	/**
	 * Bind a resource class
	 *
	 * @param type
	 *            to bind
	 */
	protected final <T> void publish(final TypeLiteral<T> type) {
		checkNotNull(type);
		publish(Key.get(type));
	}

	protected final void readBody(Class<? extends MessageBodyReader<?>> type) {
		provide(type);
	}

	protected final void readBody(Key<? extends MessageBodyReader<?>> key) {
		provide(key);
	}

	protected final <T extends MessageBodyReader<?> & MessageBodyWriter<?>> void readBody(
			TypeLiteral<T> type) {
		provide(type);
	}

	/**
	 * Configure server
	 *
	 * @return configuration builder
	 */
	protected final ServerConfigurationBuilder serve() {
		return this.config;
	}

	protected final <T extends MessageBodyReader<?> & MessageBodyWriter<?>> void writeAndReadBody(
			Class<T> type) {
		provide(type);
	}

	protected final <T extends MessageBodyReader<?> & MessageBodyWriter<?>> void writeAndReadBody(
			Key<T> key) {
		provide(key);
	}

	protected final void writeAndReadBody(
			TypeLiteral<? extends MessageBodyReader<?>> type) {
		provide(type);
	}

	protected final void writeBody(Class<? extends MessageBodyWriter<?>> type) {
		provide(type);
	}

	protected final void writeBody(Key<? extends MessageBodyWriter<?>> key) {
		provide(key);
	}

	protected final void writeBody(
			TypeLiteral<? extends MessageBodyWriter<?>> type) {
		provide(type);
	}

}
