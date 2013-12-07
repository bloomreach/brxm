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

import static com.google.code.inject.jaxrs.CXFServerModule.DIRECTION_IN;
import static com.google.code.inject.jaxrs.CXFServerModule.DIRECTION_OUT;
import static com.google.code.inject.jaxrs.internal.DefaultInvoker.isDefault;
import static org.apache.cxf.jaxrs.utils.ResourceUtils.isValidResourceClass;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.ext.Provider;

import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.cxf.jaxrs.lifecycle.ResourceProvider;
import org.apache.cxf.service.invoker.Invoker;

import com.google.code.inject.jaxrs.CXFServerModule.ServerConfiguration;
import com.google.code.inject.jaxrs.internal.JaxRsProvider;
import com.google.inject.Inject;
import com.google.inject.ProvisionException;
import com.google.inject.name.Named;

class JaxRsServerFactoryBeanProvider implements
		com.google.inject.Provider<JAXRSServerFactoryBean> {

	private static void verifySingletons(Iterable<Object> singletons) {
		final Set<String> set = new HashSet<String>();
		for (final Object s : singletons) {
			final Class<? extends Object> type = s.getClass();

			if (!isValidResourceClass(type))
				throw new ProvisionException("Type " + type + " is not valid");

			if (type.getAnnotation(Provider.class) == null)
				throw new ProvisionException("Type " + type
						+ " is not annoatated with @Provider");

			if (!set.add(type.getName())) {
				throw new ProvisionException(
						"More than one instance of the same singleton class "
								+ type.getName() + " is available");
			}
		}
	}

	private final JAXRSServerFactoryBean bean;

	@Inject
	protected JaxRsServerFactoryBeanProvider(ServerConfiguration config,
			Set<ResourceProvider> resourceProviders,
			@Named(DIRECTION_IN) Set<Interceptor<?>> inInterceptors,
			@Named(DIRECTION_OUT) Set<Interceptor<?>> outInterceptors,
			@JaxRsProvider Set<Object> providers, Invoker invoker) {

		final Class<?>[] resourceClasses = new Class<?>[resourceProviders
				.size()];
		final Map<Class<?>, ResourceProvider> map = new HashMap<Class<?>, ResourceProvider>();

		verifySingletons(providers);

		int i = 0;
		for (final ResourceProvider rp : resourceProviders) {
			final Class<?> c = rp.getResourceClass();
			if (!isValidResourceClass(c))
				throw new ProvisionException(c
						+ " is not a valid resource class");
			resourceClasses[i++] = c;
			map.put(c, rp);
		}

		bean = new JAXRSServerFactoryBean();
		bean.setAddress(config.getAddress());
		bean.setStaticSubresourceResolution(config.isStaticResourceResolution());

		bean.setResourceClasses(resourceClasses);
		for (final Map.Entry<Class<?>, ResourceProvider> entry : map.entrySet()) {
			bean.setResourceProvider(entry.getKey(), entry.getValue());
		}

		bean.setProviders(new ArrayList<Object>(providers));

		if (!inInterceptors.isEmpty())
			bean.setInInterceptors(new ArrayList<Interceptor<?>>(inInterceptors));

		if (!outInterceptors.isEmpty())
			bean.setOutInterceptors(new ArrayList<Interceptor<?>>(
					outInterceptors));

		if (!isDefault(invoker))
			bean.setInvoker(invoker);

	}

	@Override
	public JAXRSServerFactoryBean get() {
		return bean;
	}
}
