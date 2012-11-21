/*
 *  Copyright 2012 Hippo.
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
package org.hippoecm.hst.demo.components;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.hippoecm.hst.component.support.bean.BaseHstComponent;
import org.hippoecm.hst.core.component.HstComponentException;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.hst.demo.beans.GoGreenProductBean;
import org.hippoecm.hst.site.HstServices;
import org.hippoecm.hst.solr.HippoSolrClient;

public class GoGreenExternalProductIndexer extends BaseHstComponent{


    public static final String SOLR_MODULE_NAME = "org.hippoecm.hst.solr";


    @Override
    public void doAction(final HstRequest request, final HstResponse response) throws HstComponentException {


        // do actual import of the found urls now!!

        List<GoGreenProductBean> gogreenBeans = new ArrayList<GoGreenProductBean>();

        InputStream is = null;
        try {
            URL url = new URL(request.getParameter("url"));
            //url = new URL("http://www.demo.onehippo.com/restapi/products/food/2011/11./documents?_type=xml");
            is = url.openConnection().getInputStream();

            XMLInputFactory factory = XMLInputFactory.newFactory();
            XMLEventReader xmlEventReader = factory.createXMLEventReader(is);

            boolean record = false;
            List<String> docURLs = new ArrayList<String>();
            while(xmlEventReader.hasNext()){
                XMLEvent event = xmlEventReader.nextEvent();
                if (event.isStartElement()) {
                    if (event.asStartElement().getName().getLocalPart().equals("documents")) {
                        record = true;
                    }
                }
                if (event.isEndElement()) {
                    if (event.asEndElement().getName().getLocalPart().equals("documents")) {
                        record = false;
                    }
                }

                if (record) {
                    if (event.isStartElement()) {
                        StartElement startElement = event.asStartElement();
                        if (startElement.getName().getLocalPart().equals("link")) {
                            QName name = new QName("href");
                            Attribute attr = startElement.getAttributeByName(name);
                            if (attr != null) {
                              docURLs.add(attr.getValue()+"?_type=xml");
                            }
                        }
                    }
                }

            }

            for (String docUrl: docURLs) {
                is.close();
                url = new URL(docUrl);
                is = url.openConnection().getInputStream();
                factory = XMLInputFactory.newFactory();
                xmlEventReader = factory.createXMLEventReader(is);

                GoGreenProductBean gogreenBean = new GoGreenProductBean();
                while(xmlEventReader.hasNext()){
                    XMLEvent event = xmlEventReader.nextEvent();
                    if (event.isStartElement()) {
                        StartElement element = event.asStartElement();
                        if (element.getName().getLocalPart().equals("links")) {
                            // we need the second link

                            boolean second = false;
                            int count = 0;
                            XMLEvent next = xmlEventReader.nextTag();
                            while (!second) {
                                // TODO this is very brittle quick and dirty . Good enough for testsuite now
                                if(next.isStartElement() && next.asStartElement().getName().getLocalPart().equals("link")) {
                                    element = next.asStartElement();
                                    count++;
                                    if (count == 2) {
                                        break;
                                    }
                                }
                                next = xmlEventReader.nextTag();
                            }
                            if (element.getName().getLocalPart().equals("link")) {
                                QName name = new QName("href");
                                Attribute attr = element.getAttributeByName(name);
                                if (attr != null) {
                                    gogreenBean.setIdentifier(attr.getValue());

                                    // once we have set the mandatory path, add to beans

                                    gogreenBeans.add(gogreenBean);

                                }
                            }
                        }
                        if (element.getName().getLocalPart().equals("title")) {
                            if(xmlEventReader.peek().isCharacters()) {
                                gogreenBean.setTitle(xmlEventReader.nextEvent().asCharacters().getData());
                            }
                        } else if(element.getName().getLocalPart().equals("summary")) {
                            if(xmlEventReader.peek().isCharacters()) {
                                gogreenBean.setSummary(xmlEventReader.nextEvent().asCharacters().getData());
                            }
                        } else if (element.getName().getLocalPart().equals("price")) {
                            if(xmlEventReader.peek().isCharacters()) {
                                try {
                                    gogreenBean.setPrice(Double.valueOf(xmlEventReader.nextEvent().asCharacters().getData()));
                                } catch (NumberFormatException e ) {
                                    gogreenBean.setPrice(0.0);
                                }
                            }
                        } else if (element.getName().getLocalPart().equals("description")) {
                            if(xmlEventReader.peek().isCharacters()) {
                                gogreenBean.setDescription(xmlEventReader.nextEvent().asCharacters().getData());
                            }
                        } else if (element.getName().getLocalPart().equals("categories")) {
                            // loop through categories
                        }

                    }
                }
            }

        } catch (MalformedURLException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (XMLStreamException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                }
            }
        }

        if (!gogreenBeans.isEmpty()) {
            // add them now to solr
            HippoSolrClient solrClient = HstServices.getComponentManager().getComponent(HippoSolrClient.class.getName(), SOLR_MODULE_NAME);
            try {
                solrClient.getSolrServer().addBeans(gogreenBeans);
                UpdateResponse commit =  solrClient.getSolrServer().commit();
            } catch (IOException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            } catch (SolrServerException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
        }


    }
}
