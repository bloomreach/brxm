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
 * InvokerPostprocessor
 * <P>
 * Components implementing this interface can be plugged into {@link AroundProcessableJAXRSInvoker},
 * which postprocess Exchange, request object and the returned message object after the default <CODE>JAXRSInvoker</CODE> returns a message.
 * </P>
 * 
 * @version $Id$
 * @see {@link AroundProcessableJAXRSInvoker}
 */
public interface InvokerPostprocessor {

    /**
     * If this postprocessor is able to process the returned message after the default JAXRSInvoker returns
     * a message object.
     * This method should always return a message object.
     * If there's any next InvokerPostprocessor, then the next InvokerPostprocessor will 
     * receive the return message object from the current InvokerPostprocessor.
     * @param exchange
     * @param request
     * @param result the return object from the default JAXRSInvoker or the previous InvokerPostprocessor
     * @return non-null message object should be returned.
     */
    Object postprocoess(Exchange exchange, Object request, Object result);

}
