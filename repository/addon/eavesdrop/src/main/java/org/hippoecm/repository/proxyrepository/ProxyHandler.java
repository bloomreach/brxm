/*
 *  Copyright 2009 Hippo.
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.hippoecm.repository.proxyrepository;

import java.io.EOFException;
import java.io.Externalizable;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.LinkedList;

import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RangeIterator;
import javax.jcr.Repository;
import javax.jcr.Session;
import javax.jcr.ValueFactory;
import javax.jcr.Workspace;
import javax.jcr.lock.Lock;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import javax.jcr.version.Version;
import javax.jcr.version.VersionHistory;
import javax.jcr.version.VersionIterator;

import org.hippoecm.repository.api.DocumentManager;
import org.hippoecm.repository.api.HierarchyResolver;
import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.HippoQuery;
import org.hippoecm.repository.api.HippoSession;
import org.hippoecm.repository.api.HippoWorkspace;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowManager;

class ProxyHandler implements InvocationHandler {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    static final Class[] proxiedInterfaces = new Class[] {
        DocumentManager.class,
        HierarchyResolver.class,
        WorkflowManager.class,
        HippoSession.class,
        HippoNode.class,
        HippoQuery.class,
        HippoWorkspace.class,
        Workflow.class,
        Item.class,
        Lock.class,
        Node.class,
        NodeIterator.class,
        Property.class,
        PropertyIterator.class,
        Query.class,
        QueryManager.class,
        QueryResult.class,
        RangeIterator.class,
        Repository.class,
        Session.class,
        ValueFactory.class,
        Version.class,
        VersionHistory.class,
        VersionIterator.class,
        Workspace.class
    };
    PositionMap references;
    ObjectOutputStream ostream;

    public ProxyHandler() {
        references = new PositionMap();
    }

    public ProxyHandler(OutputStream stream) throws IOException {
        this();
        ostream = new ContextObjectOutputStream<PositionMap>(stream, references);
    }

    public Object register(Object upstream) {
        Object proxied = proxy(upstream);
        return proxied;
    }

    public void close() throws IOException {
        if (ostream != null) {
            ostream.flush();
            ostream = null;
        }
    }

    public void play(InputStream stream) throws IOException {
        ObjectInputStream istream = new ContextObjectInputStream<PositionMap>(stream, references);
        try {
            for (;;) {
                Invocation invocation = (Invocation)istream.readObject();
                invocation.invoke();
            }
        } catch (EOFException ex) {
        } catch (ClassNotFoundException ex) {
            // impossible internal error
        } catch (IllegalAccessException ex) {
            // impossible internal error
        } catch (InvocationTargetException ex) {
            // impossible internal error
        }
    }

    protected Object proxy(Object upstream) {
        boolean needsProxying = false;
        if (references.containsKey(upstream)) {
            return references.get(upstream);
        }
        for (int i = 0; i < proxiedInterfaces.length; i++) {
            if (proxiedInterfaces[i].isInstance(upstream)) {
                needsProxying = true;
                break;
            }
        }
        if (needsProxying) {
            LinkedList<Class> interfacesSet = new LinkedList<Class>();
            for (int i = 0; i < proxiedInterfaces.length; i++) {
                if (proxiedInterfaces[i].isInstance(upstream)) {
                    interfacesSet.add(proxiedInterfaces[i]);
                }
            }
            if (upstream instanceof Workflow) {
                Class[] upstreamInterfaces = upstream.getClass().getInterfaces();
                for (int i = 0; i < upstreamInterfaces.length; i++) {
                    if (Workflow.class.isAssignableFrom(upstreamInterfaces[i])) {
                        interfacesSet.add(upstreamInterfaces[i]);
                    }
                }

            }
            Class[] interfaces = interfacesSet.toArray(new Class[interfacesSet.size()]);
            Object proxy = Proxy.newProxyInstance(ProxyHippoRepository.class.getClassLoader(), interfaces, this);
            references.put(upstream, proxy);
            upstream = proxy;
        }
        return upstream;
    }

    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        Object upstream = references.reverseGet(proxy);
        if (args != null) {
            for (int i = 0; i < args.length; i++) {
                Object argument = references.reverseGet(args[i]);
                if (argument != null) {
                    args[i] = argument;
                }
            }
        }
        Invocation invocation = new Invocation(upstream, method, args);
        Object result = invocation.invoke();
        invocation.print(System.err, references);
        result = proxy(result);
        if (ostream != null) {
            ostream.writeObject(invocation);
        }
        return result;
    }

    final static class Reference implements Externalizable {
        transient Object object;
        int index;

        public Reference() {
        }

        Reference(Object object) {
            this.object = object;
        }

        public void writeExternal(ObjectOutput out) throws IOException {
            index = ((ContextObjectOutputStream<PositionMap>)out).getContext().indexOf(object);
            out.writeInt(index);
        }

        public void readExternal(ObjectInput in) throws IOException {
            index = in.readInt();
            object = ((ContextObjectInputStream<PositionMap>)in).getContext().get(index);
        }

        private Object readResolve() throws ObjectStreamException {
            return object;
        }
    }

    static class Invocation implements Externalizable {
        Object object;
        Method method;
        Object[] arguments;
        Object result;

        public Invocation() {
        }

        Invocation(Object object, Method method, Object[] arguments) {
            this.object = object;
            this.method = method;
            this.arguments = arguments;
        }

        public void print(PrintStream out, PositionMap map) {
            print(out, map, object);
            out.print(".");
            out.print(method.getName());
            out.print("(");
            for (int i=0; arguments != null && i<arguments.length; i++) {
                print(out, map, arguments[i]);
                if(i>0) {
                    out.print(",");
                }
            }
            out.print(") -> ");
            print(out, map, result);
            out.println();
        }

        private static void print(PrintStream out, PositionMap map, Object object) {
            if(object == null) {
                out.print("null");
            } else if(map.containsKey(object)) {
                out.print("$"+map.indexOf(object));
            } else if(object instanceof String) {
                out.print("\""+((String)object)+"\"");
            } else {
                out.print(object.toString());
            }
        }
        
        public void writeExternal(ObjectOutput out) throws IOException {
            PositionMap<Object, Object> references = ((ContextObjectOutputStream<PositionMap>)out).getContext();
            out.writeObject(new Reference(object));
            out.writeObject(method.getDeclaringClass().getName());
            out.writeObject(method.getName());
            Class[] parms = method.getParameterTypes();
            out.writeInt(parms.length);
            for (int i = 0; i < parms.length; i++) {
                out.writeObject(parms[i].getName());
            }
            for (int i = 0; i < parms.length; i++) {
                if (references.containsKey(arguments[i])) {
                    out.writeObject(new Reference(arguments[i]));
                } else {
                    out.writeObject(arguments[i]);
                }
            }
            if (references.containsKey(result)) {
                out.writeObject(new Reference(result));
            } else {
                out.writeObject(result);
            }
        }

        public void readExternal(ObjectInput in) throws IOException {
            try {
                object = in.readObject();
                Class methodClass = Class.forName((String)in.readObject());
                String methodName = (String)in.readObject();
                int count = in.readInt();
                Class[] parms = new Class[count];
                for (int i = 0; i < count; i++) {
                    parms[i] = Class.forName((String)in.readObject());
                }
                arguments = new Object[count];
                for (int i = 0; i < count; i++) {
                    arguments[i] = in.readObject();
                }
                method = methodClass.getMethod(methodName, parms);
                result = in.readObject();
            } catch (NoSuchMethodException ex) {
                throw new IOException();
            } catch (ClassNotFoundException ex) {
                throw new IOException();
            }
        }

        Object invoke() throws IllegalAccessException, InvocationTargetException {
            return method.invoke(object, arguments);
        }
    }
}
