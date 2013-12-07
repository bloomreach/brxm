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
package com.google.code.inject.jaxrs;

import static com.google.inject.Scopes.SINGLETON;
import static com.google.inject.internal.util.$Preconditions.checkState;
import static com.google.inject.multibindings.Multibinder.newSetBinder;
import static java.lang.reflect.Proxy.newProxyInstance;
import static java.util.Collections.unmodifiableSet;
import static org.apache.cxf.jaxrs.JAXRSBindingFactory.JAXRS_BINDING_ID;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.ext.MessageBodyReader;

import org.apache.cxf.Bus;
import org.apache.cxf.binding.BindingFactoryManager;
import org.apache.cxf.jaxrs.JAXRSBindingFactory;
import org.apache.cxf.jaxrs.client.JAXRSClientFactoryBean;

import com.google.code.inject.jaxrs.internal.JaxRsProvider;
import com.google.code.inject.jaxrs.util.ParametrizedType;
import com.google.inject.Binder;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.Provider;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import com.google.inject.binder.ScopedBindingBuilder;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Names;
import java.rmi.ServerException;
import org.apache.cxf.jaxrs.client.ClientWebApplicationException;

public abstract class CXFClientModule implements Module {

	protected final class UrlBindingBuilder {
		private final Key<String> url;

		private UrlBindingBuilder(Key<String> url) {
			this.url = url;
		}

		public <T> ScopedBindingBuilder to(final Class<T> resource) {
			final Type[] arguments = new Type[] { resource };

			binder.bind(new ParametrizedType(TypeAndUrl.class) {
				public Type getOwnerType() {
					return CXFClientModule.class;
				}

				public Type[] getActualTypeArguments() {
					return arguments;
				}
			}.asKey())
					.toInstance(new TypeAndUrl<T>(resource, url, wrapProxies));

			return binder.bind(resource).toProvider(
					new ParametrizedType(ClientProviderWrapper.class) {
						public Type getOwnerType() {
							return CXFClientModule.class;
						}

						public Type[] getActualTypeArguments() {
							return arguments;
						}
					}.<Provider<T>> asKey());

		}
	}

	public static final class JAXRSClientFactoryBeanProvider implements
			Provider<JAXRSClientFactoryBean> {
		private final Set<MessageBodyReader<?>> readers;

		@Inject
		protected JAXRSClientFactoryBeanProvider(
				@JaxRsProvider Set<MessageBodyReader<?>> readers) {
			this.readers = readers;
		}

		@Override
		public JAXRSClientFactoryBean get() {
			final JAXRSClientFactoryBean sf = new JAXRSClientFactoryBean();
			sf.setProviders(new ArrayList<Object>(readers));
			return sf;
		}
	}

	private static final class ClientWrapper<T> implements InvocationHandler {
		private final T resource;

		public ClientWrapper(T resource) {
			this.resource = resource;
		}

		@Override
		public Object invoke(Object proxy, Method method, Object[] args)
				throws Throwable {
			try {
				try {
					return method.invoke(resource, args);
				} catch (final InvocationTargetException e) {
					throw e.getCause();
				}
			} catch (final ClientWebApplicationException e) {
				final Class<?>[] types = method.getExceptionTypes();
				for (final Class<?> type : types) {
					final Throwable cause = e.getCause();
					if (type.isInstance(cause))
						throw cause;
				}
				throw e;
			} catch (final ServerException e) {
				// we need to wrap it otherwise CXF server might report them as this machine's fault
				throw new RuntimeException("Remote server error", e);
			}
		}

	}

	public static <T> T unwrapClient(T object) {
		final InvocationHandler handler = Proxy.getInvocationHandler(object);

		if (handler instanceof ClientWrapper) {
			@SuppressWarnings("unchecked")
			final ClientWrapper<T> wrapper = (ClientWrapper<T>) handler;
			return wrapper.resource;
		}
		return object;
	}

	public static class JAXRSClientProvider<T> implements Provider<T> {

		private final JAXRSClientFactoryBean sf;
		private final Class<T> type;
		private final boolean wrap;

		protected JAXRSClientProvider(Class<T> type, String url,
				JAXRSClientFactoryBean sf, boolean wrap) {
			this.type = type;
			this.sf = sf;
			this.wrap = wrap;

			sf.setResourceClass(type);
			sf.setAddress(url);

			final Bus bus = sf.getBus();

			final JAXRSBindingFactory factory = new JAXRSBindingFactory();
			factory.setBus(bus);

			bus.getExtension(BindingFactoryManager.class)
					.registerBindingFactory(JAXRS_BINDING_ID, factory);
		}

		@Override
		@SuppressWarnings("unchecked")
		public T get() {
			final T resource = sf.create(type);
			if (wrap)
				return (T) newProxyInstance(type.getClassLoader(),
						new Class<?>[] { type }, new ClientWrapper<T>(resource));
			else
				return resource;
		}
	}

	static class TypeAndUrl<T> {

		private final Class<T> type;
		private final Key<String> url;
		private final boolean wrap;

		public TypeAndUrl(Class<T> type, Key<String> url, boolean wrap) {
			super();
			this.type = type;
			this.url = url;
			this.wrap = wrap;
		}

		public Class<T> getType() {
			return type;
		}

		public Key<String> getUrl() {
			return url;
		}

		public boolean isWrapped() {
			return wrap;
		}
	}

	static class ClientProviderWrapper<T> extends JAXRSClientProvider<T> {

		@Inject
		protected ClientProviderWrapper(TypeAndUrl<T> key,
				JAXRSClientFactoryBean sf, Injector i) {
			super(key.getType(), i.getInstance(key.getUrl()), sf, key
					.isWrapped());
		}

	}

	private Binder binder;
	private Multibinder<Object> readers;
	private boolean bindJAXRSClientFactoryBean = true;
	private boolean wrapProxies = false;

	@Override
	public final void configure(Binder binder) {
		checkState(null == this.binder, "Re-entry not allowed");
		checkState(null == this.readers, "Re-entry not allowed");

		this.binder = binder;
		this.readers = newSetBinder(binder, new TypeLiteral<Object>() {
		}, JaxRsProvider.class);

		try {
			configure();

			if (bindJAXRSClientFactoryBean)
				binder.bind(JAXRSClientFactoryBean.class).toProvider(
						JAXRSClientFactoryBeanProvider.class);
		} finally {
			this.binder = null;
			this.readers = null;
		}
	}

	@SuppressWarnings("unused")
	@Provides
	@JaxRsProvider
	@Singleton
	private Set<MessageBodyReader<?>> provideReaders(
			@JaxRsProvider Set<Object> providers) {
		final HashSet<MessageBodyReader<?>> set = new HashSet<MessageBodyReader<?>>();
		for (final Object provider : providers) {
			if (provider instanceof MessageBodyReader)
				set.add((MessageBodyReader<?>) provider);
		}

		return unmodifiableSet(set);
	}

	public final CXFClientModule dontBindJAXRSClientFactoryBean() {
		checkState(null == this.binder, "Re-entry not allowed");
		checkState(null == this.readers, "Re-entry not allowed");
		this.bindJAXRSClientFactoryBean = false;
		return this;
	}

	public final CXFClientModule wrapProxies() {
		checkState(null == this.binder, "Re-entry not allowed");
		checkState(null == this.readers, "Re-entry not allowed");
		this.wrapProxies = true;
		return this;
	}

	protected final void read(Class<? extends MessageBodyReader<?>> reader) {
		readers.addBinding().to(reader).in(SINGLETON);
	}

	protected final UrlBindingBuilder bind(Key<String> url) {
		return new UrlBindingBuilder(url);
	}

	protected final UrlBindingBuilder bind(Class<? extends Annotation> a) {
		return bind(Key.get(String.class, a));
	}

	protected final UrlBindingBuilder bind(Annotation a) {
		return bind(Key.get(String.class, a));
	}

	protected final UrlBindingBuilder bind(String name) {
		return bind(Names.named(name));
	}

	protected abstract void configure();

}
