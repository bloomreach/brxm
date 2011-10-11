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
package org.hippoecm.hst.cmsrest;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that the annotated class implements the REST API specified by the referenced class.
 * "Implements" here means that:
 * <ul>
 * <li>the @Path annotation of the API class matches the @Path annotation of the implementation class</li>
 * <li>each API method has a corresponding implementation method with
 *   <ul>
 *     <li>the same name and return type</li>
 *     <li>all the exact same annotations as the API method (but possible more)</li>
 *     <li>the same parameters as the API method, ignoring all parameters annoted with {@link javax.ws.rs.core.Context}.</li>
 *   </ul>
 * </li>
 * </ul>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface Implements {

    /**
     * @return the API class implemented by the annotated class.
     */
    Class<?> value();

}
