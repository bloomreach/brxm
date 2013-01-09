/*
 *  Copyright 2012-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.jaxrs.cxf;

import org.apache.cxf.message.Exchange;

/**
 * InvokerPreprocessor
 * <P>
 * Components implementing this interface can be plugged into {@link AroundProcessableJAXRSInvoker},
 * which preprocess Exchange and request object before the default <CODE>JAXRSInvoker</CODE> works.
 * </P>
 * <P>
 * For example, you may check Exchange information such as operation info in order to check some
 * pre-conditions such as security annotation and return a custom HTTP error message
 * instead of continuing with the default <CODE>JAXRSInvoker</CODE>.
 * </P>
 * 
 * @version $Id$
 * @see {@link AroundProcessableJAXRSInvoker}
 */
public interface InvokerPreprocessor {

    /**
     * If this preprocessor is able to decide a return message before continuing with the default JAXRS processing,
     * then this method should return a message object.
     * If this method returns null, then the next InvokerPreprocessor will be called or the default JAXRSInvoker will
     * continue working.
     * @param exchange
     * @param request
     * @return null if it should continue with the next steps; non-null message object if processing should be stopped.
     */
    Object preprocoess(Exchange exchange, Object request);

}
