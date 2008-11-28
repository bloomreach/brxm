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
package org.hippoecm.hst.components.modules.rss.impl;

import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.hippoecm.hst.components.modules.rss.RssFeed;
import org.hippoecm.hst.components.modules.rss.RssItem;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 * Responsible for reading RSS feeds from URLs.
 *
 */
public class SimpleRssReader {


  
  /**
   * Reads the RSS feed at the specified URL and returns an RssFeed instance
   * representing it.
   *
   * @param url   the URL of the RSS feed as a String
   * @return  an RssFeed instance representing the feed
   */
  public RssFeed read(String url) {
    RssFeed rssFeed = new SimpleRssFeed();
 
    try {
       URI uri = new URI(url);
      // TODO replace the simple URI impl with HttpClient
      //HttpClient httpClient = new HttpClient();
      //GetMethod getMethod = new GetMethod(url);
      //int responseCode = httpClient.executeMethod(getMethod);

      //if (responseCode != 200) {
      //  return rssFeed;
      //}
      //InputStream in = getMethod.getResponseBodyAsStream();
    	
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      factory.setValidating(false);
      factory.setNamespaceAware(true);
      factory.setIgnoringElementContentWhitespace(true);
      factory.setIgnoringComments(true);
      DocumentBuilder builder = factory.newDocumentBuilder();
      builder.setErrorHandler(new ErrorHandler() {
        public void warning(SAXParseException e) throws SAXException {
          System.out.println(e);
          throw e;
        }

        public void error(SAXParseException e) throws SAXException {
          System.out.println(e);
          throw e;
        }

        public void fatalError(SAXParseException e) throws SAXException {
          System.out.println(e);
          throw e;
        }
      });

      
      InputStream in = uri.toURL().openStream();
      Document doc = builder.parse(in);
    
      // this section supports RSS
      NodeList channels = doc.getElementsByTagName("channel");
      for (int i = 0; i < channels.getLength(); i++) {
        NodeList nodes = channels.item(i).getChildNodes();
        for (int j = 0; j < nodes.getLength(); j++) {
          Node n = nodes.item(j);

          if (n.getNodeName().equals("item")) {
            RssItem rssItem = loadRssItem(n);
            rssFeed.addItem(rssItem);
          }
        }
      }

      // this section supports RDF (a variation of RSS)
      // ideally RSS and RDF parsing would be separated
      NodeList items = doc.getElementsByTagName("item");
      for (int i = 0; i < items.getLength(); i++) {
    	  RssItem rssItem = loadRssItem(items.item(i));
        rssFeed.addItem(rssItem);
      }
    } catch (URISyntaxException e) {
		RssFeed.log.warn("Not a valid URI " + e.getMessage() +  " for '" + url +"'");
	} catch (Exception e) {
		RssFeed.log.warn("Exception reading Rss Feed " + e.getMessage() +  " for '" + url +"'");
    }

    return rssFeed;
  }

  /**
   * Helper method to load an RSS item.
   *
   * @param root          the root Node describing the item
   * @throws Exception    if the item can't be loaded
   */
  private RssItem loadRssItem(Node root) throws Exception {
    String title = null;
    String link = null;

    NodeList nodes = root.getChildNodes();
    for (int i = 0; i < nodes.getLength(); i++) {
      Node n = nodes.item(i);

      if (n.getNodeName().equals("title")) {
        title = getTextValue(n);
      }

      if (n.getNodeName().equals("link")) {
        link = getTextValue(n);
      }
    }

    RssItem item = new SimpleRssItem();
    item.setTitle(title);
    item.setLink(link);
    return item;
  }

  /**
   * Helper method to extract the text value from a given node.
   *
   * @param node    a Node
   * @return    the text value, or an empty string if no text value available
   */
  private String getTextValue(Node node) {
    if (node.hasChildNodes()) {
      return node.getFirstChild().getNodeValue();
    } else {
      return "";
    }
  }

}
