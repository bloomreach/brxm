/*
 *  Copyright 2018 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.core;

import java.util.Map;

import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.request.HstRequestContext;

/**
 * Interface for abstraction that allows to contribute, retrieve and remove model objects.
 */
public interface ModelContributable {

    /**
     * Returns the model object associated with the given {@code name},
     * or <code>null</code> if no model object of the given {@code name} exists.
     *
     * @param name the name of the model object
     * @return the model object associated with the {@code name}, or
     *         <tt>null</tt> if the model object does not exist.
     */
    <T> T getModel(String name);

    /**
     * Returns an unmodifiable <code>Iterable</code> containing the
     * names of the model objects available to this.
     * This method returns an empty <code>Iterable</code>
     * if this has no model object available to it.
     *
     * @return an <code>Iterable</code> of strings containing the names 
     * of model objects of this.
     */
    Iterable<String> getModelNames();

    /**
     * Returns an unmodifiable map of model objects contributed by {@link #setModel(String, Object)}.
     * <P>
     * Note that the returned map contains only the pairs of model name and value objects contributed by {@link #setModel(String, Object)},
     * but it does not contain attributes set by <code>#setAttribute(String,Object)</code> API calls, whereas most
     * implementations of this interface (such as {@link HstRequest} and {@link HstRequestContext}) provides a
     * combined view for both <code>models</code> and other <code>attributes</code> through <code>#getAttribute(String)</code>,
     * <code>#getAttributeNames()</code> or <code>#getAttributeMap</code>.
     * </P>
     * @return an unmodifiable map of model objectscontributed by {@link #setModel(String, Object)}
     */
    Map<String, Object> getModelsMap();

    /**
     * Stores a model object in this.
     * <p>
     * Model objects are contributed by a controller component to this, in general.
     * And, the contributed model objects may be accessed in view rendering or special model
     * aggregation / serialization request pipeline processing.
     * </p>
     * <p>
     * If the model object passed in is null, the effect is the same as
     * calling {@link #removeModel}.
     *
     * </p>
     * @param name the name of the model object
     * @param model the model object to be stored
     * @return the previous model object associated with <tt>name</tt>, or
     *         <tt>null</tt> if there was no mapping for <tt>name</tt>.
     */
    Object setModel(String name, Object model);

    /**
     * Removes a model object from this.
     *
     * @param name a <code>String</code> specifying 
     * the name of the model object to remove
     */
   void removeModel(String name);

}
