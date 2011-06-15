/*
 *  Copyright 2011 Hippo.
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

import org.dom4j.Element;

/**
 * Represents an instruction element (a hippo:initializeitem) inside hippoecm-extension.xml file.
 */
interface Instruction {

    final String SVN_ID = "$Id: $";

    /**
     * @return  The name of this instruction
     */
    String getName();
    
    Double getSequence();

    /**
     * @return  xml representation of this instruction.
     */
    Element createInstructionElement();
}
