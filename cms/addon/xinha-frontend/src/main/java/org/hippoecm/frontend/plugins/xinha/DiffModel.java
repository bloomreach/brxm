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
package org.hippoecm.frontend.plugins.xinha;

import java.io.ByteArrayOutputStream;
import java.io.StringReader;

import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;

import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.outerj.daisy.diff.DaisyDiff;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.InputSource;
import org.xml.sax.helpers.AttributesImpl;

public class DiffModel extends LoadableDetachableModel<String> {
    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(DiffModel.class);

    IModel<String> original;
    IModel<String> current;

    public DiffModel(IModel<String> orig, IModel<String> current) {
        this.original = orig;
        this.current = current;
    }

    @Override
    protected String load() {
        if (original == null || current == null) {
            return null;
        }
        try {
            InputSource oldSource = new InputSource();
            oldSource.setCharacterStream(new StringReader(original.getObject()));

            InputSource newSource = new InputSource();
            newSource.setCharacterStream(new StringReader(current.getObject()));

            SAXTransformerFactory tf = (SAXTransformerFactory) TransformerFactory.newInstance();
            TransformerHandler handler = tf.newTransformerHandler();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            StreamResult dr = new StreamResult(baos);
            handler.setResult(dr);
            handler.startDocument();
            handler.startElement(null, "html", "html", new AttributesImpl());
            DaisyDiff.diffHTML(oldSource, newSource, handler, null, null);
            handler.endElement(null, "html", "html");
            handler.endDocument();

            return baos.toString();
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        return null;
    }

    @Override
    public void detach() {
        original.detach();
        current.detach();
    }
}
