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

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import com.google.inject.Key;

public abstract class ParametrizedType implements ParameterizedType {
	private final Type rawType;

	protected ParametrizedType(Type rawType) {
		super();
		this.rawType = rawType;
	}

	@Override
	public final Type getRawType() {
		return rawType;
	}

	@Override
	public Type getOwnerType() {
		return null;
	}

	@SuppressWarnings("unchecked")
	public <T> Key<T> asKey() {
		return (Key<T>) Key.get(this);
	}
}