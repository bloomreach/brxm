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

import org.apache.cxf.jaxrs.lifecycle.ResourceProvider;
import org.apache.cxf.message.Message;

import com.google.code.inject.jaxrs.util.BindingProvider;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.ProvisionException;
import com.google.inject.Scope;

import static com.google.code.inject.jaxrs.scope.CXFScopes.REQUEST;
import static com.google.inject.Scopes.NO_SCOPE;

class GuicePerRequestResourceProvider<T> implements ResourceProvider {

    private final Provider<T> provider;
    private final Class<?> actualType;

    @Inject
    protected GuicePerRequestResourceProvider(BindingProvider<T> binding,
                                              Provider<T> provider) {
        final Scope scope = binding.getScope();
        if (NO_SCOPE != scope && REQUEST != scope) {
            throw new ProvisionException("Invalid scope " + scope + " of "
                    + binding.getKey());
        }
        this.actualType = binding.getActualType();
        this.provider = provider;
    }

    @Override
    public Object getInstance(Message m) {
        return provider.get();
    }

    @Override
    public void releaseInstance(Message m, Object o) {
        // NOOP
    }

    @Override
    public Class<?> getResourceClass() {
        return actualType;
    }

    @Override
    public boolean isSingleton() {
        return false;
    }

}