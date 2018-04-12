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
package org.hippoecm.hst.pagemodelapi.v09.content.beans.jackson;

import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.container.RequestContextProvider;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.cfg.MapperConfig;
import com.fasterxml.jackson.databind.introspect.AnnotatedClass;
import com.fasterxml.jackson.databind.introspect.BeanPropertyDefinition;
import com.fasterxml.jackson.databind.ser.VirtualBeanPropertyWriter;
import com.fasterxml.jackson.databind.util.Annotations;

import static org.hippoecm.hst.core.container.ContainerConstants.PAGE_MODEL_PIPELINE_NAME;

/**
 * Abstract base class to add extra properties dynamically.
 * @param <S> the type of the original source bean type
 * @param <T> the type of the target extra property object type
 */
abstract public class AbstractVirtualBeanPropertyWriter<S, T> extends VirtualBeanPropertyWriter {

    private static final long serialVersionUID = 1L;

    private static Logger log = LoggerFactory.getLogger(AbstractVirtualBeanPropertyWriter.class);

    protected AbstractVirtualBeanPropertyWriter() {
        super();
    }

    protected AbstractVirtualBeanPropertyWriter(BeanPropertyDefinition propDef, Annotations contextAnnotations,
                                                    JavaType type) {
        super(propDef, contextAnnotations, type);
    }

    @SuppressWarnings("unchecked")
    @Override
    protected final Object value(Object item, JsonGenerator gen, SerializerProvider prov) throws Exception {
        return createValue(RequestContextProvider.get(), (S) item);
    }

    @Override
    public final VirtualBeanPropertyWriter withConfig(MapperConfig<?> config, AnnotatedClass declaringClass,
            BeanPropertyDefinition propDef, JavaType type) {
        // Ref: jackson-databind-master/src/test/java/com/fasterxml/jackson/databind/ser/TestVirtualProperties.java
        return createInstanceWithConfig(config, declaringClass, propDef, type);
    }

    /**
     * Create a value or return null if no extra property is not needed to add.
     * @param requestContext request context
     * @param item bean item
     * @return a links map or null
     * @throws Exception if any exception occurs
     */
    abstract protected T createValue(final HstRequestContext requestContext, final S item) throws Exception;

    /**
     * Create a new instance with the configuration objects.
     * Normally need to create a new instance invoking another constructor with the arguments.
     * <p>
     * <em>Note:</em> implementation classes must implement this method by instantiating a new instance with another
     * constructor of its own with the arguments, simply invoking super class' constructor with the same arguments.
     * <p>
     * Reference: jackson-databind-master/src/test/java/com/fasterxml/jackson/databind/ser/TestVirtualProperties.java
     *
     * @param config mapper config
     * @param declaringClass declaring class
     * @param propDef bean property defintion
     * @param type java type
     * @return a new instance with the configuration objects
     */
    abstract protected VirtualBeanPropertyWriter createInstanceWithConfig(MapperConfig<?> config, AnnotatedClass declaringClass,
            BeanPropertyDefinition propDef, JavaType type);

    /**
     * Return the site mount for the resolved mount for the current page model api request, or null if something's wrong.
     * @param requestContext request context
     * @return the site mount for the resolved mount for the current page model api request or null if something's wrong
     */
    protected Mount getSiteMountForCurrentPageModelApiRequest(final HstRequestContext requestContext) {
        Mount curMount = requestContext.getResolvedMount().getMount();

        if (PAGE_MODEL_PIPELINE_NAME.equals(curMount.getNamedPipeline())) {
            Mount siteMount = curMount.getParent();

            if (siteMount != null) {
                return siteMount;
            } else {
                log.info("Expected a 'PageModelPipeline' always to be nested below a parent site mount. This is not the case for '{}'. "
                        + "Could not find a proper site mount.",
                        curMount);
            }
        } else {
            log.warn("Expected request mount have named pipeline '{}' but was '{}'. Could not find a proper site mount.",
                    PAGE_MODEL_PIPELINE_NAME, curMount.getNamedPipeline());
        }

        return null;
    }
}
