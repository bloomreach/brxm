/*
 *  Copyright 2010-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.plugins.standards.diff;

import org.apache.wicket.Session;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.hippoecm.htmldiff.DiffHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class HtmlDiffModel extends LoadableDetachableModel<String> {

    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(HtmlDiffModel.class);

    IModel<String> original;
    IModel<String> current;

    public HtmlDiffModel(IModel<String> original, IModel<String> current) {
        this.original = new ValidHtmlModel(original);
        this.current = new ValidHtmlModel(current);
    }

    @Override
    protected String load() {
        if (original == null || current == null) {
            return null;
        }

        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DiffHelper.diffHtml(original.getObject(), current.getObject(), new StreamResult(baos),
                    Session.get().getLocale());
            return baos.toString("UTF-8");
        } catch (TransformerConfigurationException e) {
            log.error(e.getMessage(), e);
        } catch (SAXException e) {
            log.info(e.getMessage(), e);
        } catch (IOException e) {
            log.error(e.getMessage());
        }
        return null;
    }

    @Override
    public void detach() {
        if (original != null) {
            original.detach();
        }
        if (current != null) {
            current.detach();
        }
        super.detach();
    }

    /**
     * This model ensures the value is surrounded with <html><body>..value..</body></html>
     * This is required by the DiffHelper.diffHtml method.
     */
    public static class ValidHtmlModel extends AbstractReadOnlyModel<String> {

        IModel<String> wrapped;

        ValidHtmlModel(IModel<String> wrapped) {
            this.wrapped = wrapped;
        }

        @Override
        public String getObject() {
            if (wrapped != null) {
                String value = wrapped.getObject();
                if (value != null) {
                    if (!value.trim().startsWith("<html>")) {
                        value =  "<html><body>" + value + "</body></html>";
                    }
                    //Replace the _blank so that the document opens in the same window
                    value = value.trim().replaceAll("<a target=\"_blank\"", "<a ");
                }
                return value;
            }
            return null;
        }

        @Override
        public void detach() {
            if (wrapped != null) {
                wrapped.detach();
            }
        }
    }
}
