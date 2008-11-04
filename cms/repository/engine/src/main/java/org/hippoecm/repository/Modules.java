/*
 *  Copyright 2008 Hippo.
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
package org.hippoecm.repository;

import java.io.IOException;
import java.net.URL;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeSet;
import java.util.jar.Manifest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Modules<T extends Object> implements Iterable<T> {

    final Logger log = LoggerFactory.getLogger(Modules.class);
    Set modules;

    public Modules() {
        modules = new TreeSet<T>();
    }

    public Modules(Collection<T> modules) {
        this();
        this.modules.addAll(modules);
    }

    public Modules(T module) {
        this();
    }

    public Modules(Modules modules, Class<T> clazz) {
        this();
        for (Object instance : modules) {
            if (clazz.isInstance(instance)) {
                this.modules.add((T) instance);
            }
        }
    }

    public Modules(ClassLoader loader) {
        this();
        try {
            for (Enumeration<URL> e = loader.getResources("/META-INF/MANIFEST.MF"); e.hasMoreElements(); ) {
                URL url = e.nextElement();
                try {
                    Manifest manifest = new Manifest(url.openStream());
                    String modulesString = manifest.getMainAttributes().getValue("Hippo-Modules");
                    if (modulesString != null) {
                        for (StringTokenizer tok = new StringTokenizer(modulesString); tok.hasMoreTokens();) {
                            String moduleClassName = tok.nextToken().trim();
                            if (!moduleClassName.equals("")) {
                                try {
                                    Object instance = Class.forName(moduleClassName, true, loader).newInstance();
                                    modules.add(instance);
                                } catch (ClassNotFoundException ex) {
                                } catch (InstantiationException ex) {
                                } catch (IllegalAccessException ex) {
                                }
                            }
                        }
                    }
                } catch (IOException ex) {
                    log.error("Cannot access manifest " + url.toString(), ex);
                }
            }
        } catch (IOException ex) {
            log.error("Cannot access any manifest", ex);
        }
    }

    public Iterator<T> iterator() {
        return modules.iterator();
    }
    
    private static Modules allModules;

    public static Modules getModules() {
        return allModules;
    }

    static void setModules(Modules modules) {
        allModules = modules;
    }
}
