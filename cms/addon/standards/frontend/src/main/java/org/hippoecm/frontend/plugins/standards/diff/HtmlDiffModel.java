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
package org.hippoecm.frontend.plugins.standards.diff;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;

import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;

import org.apache.wicket.Session;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.hippoecm.htmldiff.DiffHelper;
import org.outerj.daisy.diff.DaisyDiff;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

public class HtmlDiffModel extends LoadableDetachableModel<String> {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(HtmlDiffModel.class);

    IModel<String> original;
    IModel<String> current;

    public HtmlDiffModel(IModel<String> original, IModel<String> current) {
        this.original = original;
        this.current = current;
    }

    @Override
    protected String load() {
        if (original == null || current == null) {
            return null;
        }

        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DiffHelper.diffHtml(original.getObject(), current.getObject(), new StreamResult(baos), Session.get().getLocale());
            return baos.toString();
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
        if(original != null) {
            original.detach();
        }
        if(current != null) {
            current.detach();
        }
        super.detach();
    }
}
