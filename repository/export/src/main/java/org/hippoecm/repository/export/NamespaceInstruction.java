/*
 *  Copyright 2011 Hippo (www.hippo.nl).
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
package org.hippoecm.repository.export;

/**
 * A namespace instruction.
 */
interface NamespaceInstruction extends Instruction {

    /**
     * Checks if a namespace matches this namespace.
     * If this namespace instruction represents namespace uri
     * http://examples.com/example/1.0, http://example.com/example/1.1
     * would match this namespace.
     */
	boolean matchesNamespace(String namespace);
	
	/**
	 * Called if a namespace is updated. For instance from
	 * http://examples.com/example/1.0 to http://example.com/example/1.1
	 */
	void updateNamespace(String namespace);
	
}