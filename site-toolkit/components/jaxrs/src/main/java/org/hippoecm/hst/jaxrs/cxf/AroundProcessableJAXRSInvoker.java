/*
 *  Copyright 2010 Hippo.
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

import org.apache.cxf.jaxrs.JAXRSInvoker;
import org.apache.cxf.message.Exchange;

/**
 * AroundProcessableJAXRSInvoker
 * 
 * <P>
 * AroundProcessableJAXRSInvoker can do pre/post- processing before/after the default JAXRSInvoker works.
 * Multiple {@link InvokerPreprocessor}s and {@link InvokerPostprocessor}s can be plugged into this.
 * </P>
 * 
 * @version $Id$
 * @see {@link InvokerPreprocessor}
 * @see {@link InvokerPostprocessor}
 */
public class AroundProcessableJAXRSInvoker extends JAXRSInvoker {

    private InvokerPreprocessor [] invokerPreprocessors;
    private InvokerPostprocessor [] invokerPostprocessors;

    public AroundProcessableJAXRSInvoker() {
        super();
    }

    public InvokerPreprocessor[] getInvokerPreprocessors() {
        if (this.invokerPreprocessors == null) {
            return null;
        } else {
            InvokerPreprocessor [] invokerPreprocessors = new InvokerPreprocessor[this.invokerPreprocessors.length];
            System.arraycopy(this.invokerPreprocessors, 0, invokerPreprocessors, 0, this.invokerPreprocessors.length);
            return invokerPreprocessors;
        }
    }

    public void setInvokerPreprocessors(InvokerPreprocessor[] invokerPreprocessors) {
        if (invokerPreprocessors == null) {
            this.invokerPreprocessors = null;
        } else {
            this.invokerPreprocessors = new InvokerPreprocessor[invokerPreprocessors.length];
            System.arraycopy(invokerPreprocessors, 0, this.invokerPreprocessors, 0, invokerPreprocessors.length);
        }
    }

    public InvokerPostprocessor[] getInvokerPostprocessors() {
        if (this.invokerPostprocessors == null) {
            return null;
        } else {
            InvokerPostprocessor [] invokerPostprocessors = new InvokerPostprocessor[this.invokerPostprocessors.length];
            System.arraycopy(this.invokerPostprocessors, 0, invokerPostprocessors, 0, this.invokerPostprocessors.length);
            return invokerPostprocessors;
        }
    }

    public void setInvokerPostprocessors(InvokerPostprocessor[] invokerPostprocessors) {
        if (invokerPostprocessors == null) {
            this.invokerPostprocessors = null;
        } else {
            this.invokerPostprocessors = new InvokerPostprocessor[invokerPostprocessors.length];
            System.arraycopy(invokerPostprocessors, 0, this.invokerPostprocessors, 0, invokerPostprocessors.length);
        }
    }

    @Override
    public Object invoke(Exchange exchange, Object requestParams, Object resourceObject) {
        if (invokerPreprocessors != null) {
            Object preprocessedObject = null;

            for (InvokerPreprocessor preprocessor : invokerPreprocessors) {
                preprocessedObject = preprocessor.preprocoess(exchange, requestParams);

                if (preprocessedObject != null) {
                    return preprocessedObject;
                }
            }
        }

        Object result = super.invoke(exchange, requestParams, resourceObject);

        if (invokerPostprocessors != null) {
            for (InvokerPostprocessor postprocessor : invokerPostprocessors) {
                result = postprocessor.postprocoess(exchange, requestParams, result);
            }
        }

        return result;
    }
}
