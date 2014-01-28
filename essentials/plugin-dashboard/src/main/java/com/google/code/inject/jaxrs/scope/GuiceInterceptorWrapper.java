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
package com.google.code.inject.jaxrs.scope;

import java.util.Iterator;
import java.util.concurrent.Callable;

import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.interceptor.InterceptorChain;
import org.apache.cxf.interceptor.ServiceInvokerInterceptor;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.PhaseInterceptor;

import com.google.inject.OutOfScopeException;

import static com.google.inject.internal.util.$Preconditions.checkState;
import static java.lang.Thread.currentThread;
import static java.util.Collections.singleton;
import static org.apache.cxf.phase.Phase.INVOKE;

public class GuiceInterceptorWrapper extends AbstractPhaseInterceptor<Message> {

    private static final class Context {
       private final Exchange exchange;
       private volatile Thread owner;

        private Context(Exchange exchange) {
            this.exchange = exchange;
        }

        <T> T call(Callable<T> callable) throws Exception {
            final Thread oldOwner = owner;
            final Thread newOwner = currentThread();
            checkState(oldOwner == null || oldOwner == newOwner,
                    "Trying to transfer exchange scope but original scope is still active");
            owner = newOwner;
            final Context previous = localContext.get();
            localContext.set(this);
            try {
                return callable.call();
            } finally {
                owner = oldOwner;
                localContext.set(previous);
            }
        }

        public Exchange getExchange() {
            return exchange;
        }
    }

    private static final ThreadLocal<Context> localContext = new ThreadLocal<Context>();

    private static Context getContext() {
        final Context context = localContext.get();
        if (context == null) {
            throw new OutOfScopeException(
                    "Cannot access scoped object. Either we"
                            + " are not currently inside a exchange, or you may"
                            + " have forgotten to apply "
                            + GuiceInterceptorWrapper.class.getName()
                            + " as an interceptor for this endpoint.");
        }
        return context;
    }

    static Exchange getExchange() {
        return getContext().getExchange();
    }

    private final PhaseInterceptor<Message> delegate;

    public GuiceInterceptorWrapper() {
        this(new ServiceInvokerInterceptor());
    }

    public GuiceInterceptorWrapper(PhaseInterceptor<Message> delegate) {
        super(INVOKE);
        setBefore(singleton(delegate.getClass().getName()));
        this.delegate = delegate;
    }

    @Override
    public void handleMessage(final Message m) throws Fault {
        // remove delegate from chain
        final InterceptorChain chain = m.getInterceptorChain();
        final Iterator<Interceptor<? extends Message>> it = chain.iterator();
        while (it.hasNext()) {
            final Interceptor<? extends Message> next = it.next();
            if (delegate.getClass().isInstance(next)) {
                chain.remove(next);
            }
        }

        // process in scope
        final Context previous = localContext.get();
        final Exchange exchange = (previous != null) ? previous.getExchange()
                : m.getExchange();

        try {
            new Context(exchange).call(new Callable<Void>() {
                @Override
                public Void call() throws Exception {
                    delegate.handleMessage(m);
                    return null;
                }
            });
        } catch (final Fault e) {
            throw e;
        } catch (final Exception e) {
            throw new Fault(e);
        }
    }
}