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
package com.google.code.inject.jaxrs.internal;

import org.apache.cxf.message.Exchange;
import org.apache.cxf.service.invoker.Invoker;

/**
 * Default invoker marker class. Do not use for actual operation.
 */
public final class DefaultInvoker implements Invoker {

    @Override
    public Object invoke(Exchange arg0, Object arg1) {
        throw new UnsupportedOperationException();
    }

    /**
     * Is an invoker a default marker.
     *
     * @param invoker instance to check
     * @return true if invoker is an instance of DefaultInvoker
     */
    public static boolean isDefault(Invoker invoker) {
        return invoker instanceof DefaultInvoker;
    }

}