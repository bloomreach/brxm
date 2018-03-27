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
package org.hippoecm.hst.pagemodelapi09.core.container;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.container.RequestContextProvider;
import org.hippoecm.hst.content.beans.standard.HippoHtmlBean;
import org.hippoecm.hst.pagemodelapi09.core.content.HtmlContentRewriter;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hippoecm.hst.core.container.ContainerConstants.PAGE_MODEL_PIPELINE_NAME;

public class HippoHtmlBeanSerializer extends JsonSerializer<HippoHtmlBean> {

    private static Logger log = LoggerFactory.getLogger(HippoHtmlBeanSerializer.class);

    private final HtmlContentRewriter htmlContentRewriter;


    public HippoHtmlBeanSerializer(final HtmlContentRewriter htmlContentRewriter) {

        this.htmlContentRewriter = htmlContentRewriter;
    }

    @Override
    public void serialize(final HippoHtmlBean hippoHtmlBean, final JsonGenerator gen, final SerializerProvider serializers) throws IOException {

        final HstRequestContext requestContext = RequestContextProvider.get();

        Mount siteMount;
        Mount pageModelApiMount = requestContext.getResolvedMount().getMount();
        if (!PAGE_MODEL_PIPELINE_NAME.equals(pageModelApiMount.getNamedPipeline())) {
            log.warn("Expected request mount have named pipeline '{}' but was '{}'. Cannot do content rewriting properly.",
                    PAGE_MODEL_PIPELINE_NAME, pageModelApiMount.getNamedPipeline());
            gen.writeObject(new HtmlPojo("html", hippoHtmlBean.getContent()));
            return;
        } else {
            siteMount = pageModelApiMount.getParent();
            if (siteMount == null) {
                log.info("Expected a 'PageModelPipeline' always to be nested below a parent site mount. This is not the " +
                        "case for '{}'. Cannot do content rewriting properly.", pageModelApiMount);
                gen.writeObject(new HtmlPojo("html", hippoHtmlBean.getContent()));
                return;
            }
        }

        final String rewrite = htmlContentRewriter.rewrite(hippoHtmlBean.getContent(), hippoHtmlBean.getNode(), requestContext,
                siteMount);

        if (rewrite != null) {
            gen.writeObject(new HtmlPojo("html", rewrite));
        }
    }

    public static class HtmlPojo {

        private String type;
        private String value;

        @SuppressWarnings("unused")
        public HtmlPojo() {}

        public HtmlPojo(final String type, final String value) {
            this.type = type;
            this.value = value;
        }

        public String getType() {
            return type;
        }

        public void setType(final String type) {
            this.type = type;
        }

        public String getValue() {
            return value;
        }

        public void setValue(final String value) {
            this.value = value;
        }
    }
}
