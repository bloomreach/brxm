/*
 *  Copyright 2008 Hippo.
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
package org.hippoecm.hst.tags;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.BodyTagSupport;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.hippoecm.hst.core.component.HstResponse;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class ResponsePropertyTag extends BodyTagSupport {

    private static final long serialVersionUID = 1L;
    
    protected String name;
    
    protected DocumentBuilderFactory dbf;
    protected DocumentBuilder db;

    //*********************************************************************
    // Constructor and initialization

    public ResponsePropertyTag() {
        super();
        init();
    }

    protected void init() {
        dbf = null;
        db = null;
    }

    public int doEndTag() throws JspException {
        HstResponse hstResponse = null;

        if (pageContext.getResponse() instanceof HstResponse) {
            hstResponse = (HstResponse) pageContext.getResponse();
        }

        if (hstResponse == null) {
            return SKIP_BODY;
        }
        
        if (hstResponse.containsProperty(this.name)) {
            return SKIP_BODY;
        }
        
        Reader reader = null;
        
        try {
            // set up our DocumentBuilder
            if (dbf == null) {
                dbf = DocumentBuilderFactory.newInstance();
                dbf.setNamespaceAware(true);
                dbf.setValidating(false);
            }

            db = dbf.newDocumentBuilder();

            String xmlText = "";

            if (bodyContent != null && bodyContent.getString() != null)
                xmlText = bodyContent.getString().trim();

            reader = new StringReader(xmlText);
            Document d = db.parse(new InputSource(reader));
            
            if (pageContext.getResponse() instanceof HstResponse) {
                hstResponse.addProperty(this.name, d.getDocumentElement());
            }

            return EVAL_PAGE;
        } catch (SAXException ex) {
            throw new JspException(ex);
        } catch (IOException ex) {
            throw new JspException(ex);
        } catch (ParserConfigurationException ex) {
            throw new JspException(ex);
        } finally {
            if (reader != null) try { reader.close(); } catch (Exception ce) { }
        }
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getName() {
        return this.name;
    }

    // Releases any resources we may have (or inherit)
    public void release() {
        init();
    }

}
