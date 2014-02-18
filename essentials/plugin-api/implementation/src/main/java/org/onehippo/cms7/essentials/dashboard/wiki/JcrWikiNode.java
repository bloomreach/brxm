/**
 * This file is part of the JCROM project.
 * Copyright (C) 2008-2013 - All rights reserved.
 * Authors: Olafur Gauti Gudmundsson, Nicolas Dos Santos
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onehippo.cms7.essentials.dashboard.wiki;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.jcr.nodetype.NodeType;

/**
 * This annotation is applied to types (classes or interfaces) that implement
 * JcrEntity, and provides the ability to specify what JCR node type to use
 * when creating a JCR node from the object being mapped, mixin types, and more.
 *
 * @author Olafur Gauti Gudmundsson
 * @author Nicolas Dos Santos
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface JcrWikiNode {

    /**
     * The node type to be applied for this JCR object.
     * Defaults to "nt:unstructured" ( {@link javax.jcr.nodetype.NodeType#NT_UNSTRUCTURED} ).
     *
     * @return the node type to use when creating a JCR node for this object
     */
    String nodeType() default NodeType.NT_UNSTRUCTURED;

    /**
     * Mixin types to be added to all new nodes created
     * from this class.
     *
     * @return the mixin types for new nodes created from the class
     */
    String[] mixinTypes() default {};

}
