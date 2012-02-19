/*
 *  Copyright 2011 Hippo.
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
package org.onehippo.translate;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLEncoder;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;

import org.hippoecm.repository.HippoStdNodeType;
import org.hippoecm.repository.api.MappingException;
import org.hippoecm.repository.api.RepositoryMap;
import org.hippoecm.repository.api.WorkflowContext;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.ext.InternalWorkflow;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TranslateWorkflowImpl implements TranslateWorkflow, InternalWorkflow {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static Logger log = LoggerFactory.getLogger(TranslateWorkflowImpl.class);

    private final Session session;
    private final Node subject;

    private boolean useGet = false;
    private boolean useAlwaysDetect = true;
    private boolean useGoogleSecond = false;
    private String googleTranslateURL1 = "https://ajax.googleapis.com/ajax/services/language/translate";
    private String googleTranslateURL2 = "https://www.googleapis.com/language/translate/v2";
    private String googleKey = null;
    private int timeout = 10 * 1000; // milliseconds

    public TranslateWorkflowImpl(WorkflowContext context, Session userSession, Session rootSession, Node subject)
            throws RemoteException, RepositoryException {
        this.session = rootSession;
        this.subject = session.getNodeByIdentifier(subject.getIdentifier());

        RepositoryMap config = context.getWorkflowConfiguration();
        if (config.get("google.key") == null) {
            useGoogleSecond = false;
            googleKey = null;
        } else {
            googleKey = (String)config.get("google.key");
            if (config.get("google.version") != null && (config.get("google.version").equals("1") || config.get("google.version").equals("1.0") || config.get("google.version").equals(Integer.valueOf(1)))) {
                useGoogleSecond = false;
            } else {
                useGoogleSecond = true;
            }
        }
        String timeoutParameter = (String)config.get("timeout");
        if (timeoutParameter != null) {
            timeout = Integer.parseInt(timeoutParameter);
        }
    }

    public Map<String, Serializable> hints() throws WorkflowException, RemoteException, RepositoryException {
        return new TreeMap<String, Serializable>();
    }
    
    public void translate(String language, Set<String> fields) throws WorkflowException, MappingException, RepositoryException, RemoteException {
        ArrayList<String> paths = new ArrayList<String>();
        paths.addAll(fields);
        List<String> texts = new LinkedList<String>();
        for (String path : paths) {
            for (Property property : getProperties(subject, path.split("/"))) {
                if (property.isMultiple()) {
                    Value[] values = property.getValues();
                    for (Value value : values) {
                        texts.add(value.getString());
                    }
                } else {
                    texts.add(property.getString());
                }
            }
        }

        try {
            if(subject.hasProperty(HippoStdNodeType.HIPPOSTD_LANGUAGE)) {
                translate(texts, language, subject.getProperty(HippoStdNodeType.HIPPOSTD_LANGUAGE).getString());
            } else {
                translate(texts, language);
            }
        } catch(IOException ex) {
            throw new WorkflowException("backoffice service unavailable", ex);
        }

        Iterator<String> iter = texts.iterator();
        for (String path : paths) {
            for (Property property : getProperties(subject, path.split("/"))) {
                if (property.isMultiple()) {
                    int numValues = property.getValues().length;
                    String[] values = new String[numValues];
                    for (int i = 0; i < numValues; i++) {
                        values[i] = iter.next();
                    }
                    property.setValue(values);
                } else {
                    property.setValue(iter.next());
                }
            }
        }

        if(subject.isNodeType(HippoStdNodeType.NT_LANGUAGEABLE)) {
            subject.setProperty(HippoStdNodeType.HIPPOSTD_LANGUAGE, language);
        }
        session.save();
    }

    protected void translate(List<String> texts, String targetLanguage) throws IOException, MappingException {
        translate(texts, targetLanguage, null);
    }

    protected void translate(List<String> texts, String targetLanguage, String sourceLanguage) throws IOException, MappingException {
        try {
            StringBuilder parameters = new StringBuilder();
            StringBuilder urlSpec;
            if (!useGoogleSecond) {
                urlSpec = new StringBuilder(googleTranslateURL1);
                parameters.append("v=1.0&format=html");
                if (!useAlwaysDetect && sourceLanguage != null && !sourceLanguage.trim().equals("")) {
                    parameters.append("&langpair=").append(sourceLanguage).append("%7C").append(targetLanguage);
                } else {
                    parameters.append("&langpair=%7C").append(targetLanguage);
                }
            } else {
                urlSpec = new StringBuilder(googleTranslateURL2);
                parameters.append("prettyprint=false&format=html&key=").append(googleKey);
                if (sourceLanguage != null && !sourceLanguage.trim().equals("")) {
                    parameters.append("&source=").append(sourceLanguage);
                }
                parameters.append("&target=").append(targetLanguage);
            }
            for (String text : texts) {
                parameters.append("&q=").append(URLEncoder.encode(text, "UTF-8"));
            }
            if (useGet) {
                urlSpec.append("?").append(parameters);
            }
            if (log.isDebugEnabled()) {
                log.debug("request to google translate \"{}\" with parameters \"{}\"", urlSpec.toString(), parameters.toString());
            }
            URL url = new URL(urlSpec.toString());
            HttpURLConnection connection = (HttpURLConnection)url.openConnection();
            connection.setConnectTimeout(timeout);
            connection.setReadTimeout(timeout);
            PrintWriter parametersWriter = null;
            if (useGet) {
                connection.setRequestMethod("GET");
            } else {
                connection.setDoOutput(true);
                connection.setRequestMethod("POST");
                connection.addRequestProperty("X-HTTP-Method-Override", "GET");
                parametersWriter = new PrintWriter(connection.getOutputStream());
                parametersWriter.write(parameters.toString());
                parametersWriter.flush();
            }
            try {
                StringBuilder sb = new StringBuilder();
                String contentType = connection.getHeaderField("Content-Type");
                String charset = "UTF-8";
                for (String param : contentType.replace(" ", "").split(";")) {
                    if (param.startsWith("charset=")) {
                        charset = param.split("=", 2)[1];
                        break;
                    }
                }
                Reader responseReader = new InputStreamReader(connection.getInputStream(), charset);
                char[] buffer = new char[1024];
                int len;
                do {
                    len = responseReader.read(buffer);
                    if (len > 0) {
                        sb.append(buffer, 0, len);
                    }
                } while (len >= 0);
                if (log.isDebugEnabled()) {
                    log.debug("response from google translate reads \"{}\"", sb.toString());
                }
                JSONObject jsonObject = new JSONObject(sb.toString());
                if (!useGoogleSecond) {
                    JSONArray jsonArray = jsonObject.optJSONArray("responseData");
                    if (jsonArray != null) {
                        for (int i = 0; i < jsonArray.length(); i++) {
                            if (jsonArray.getJSONObject(i).getString("responseStatus").equals("200")) {
                                String translatedText = jsonArray.getJSONObject(i).getJSONObject("responseData").getString("translatedText");
                                if (log.isDebugEnabled()) {
                                    log.debug("translated \"{}\" to \"{}\"", texts.get(i), translatedText);
                                }
                                texts.set(i, translatedText);
                            }
                        }
                    } else if (jsonObject.optJSONObject("responseData") != null) {
                        if (jsonObject.getString("responseStatus").equals("200")) {
                            String translatedText = jsonObject.getJSONObject("responseData").getString("translatedText");
                            if (log.isDebugEnabled()) {
                                log.debug("translated \"{}\" to \"{}\"", texts.get(0), translatedText);
                            }
                            texts.set(0, translatedText);
                        }
                    }
                } else {
                    JSONArray jsonArray = jsonObject.getJSONObject("data").getJSONArray("translations");
                    for (int i = 0; i < jsonArray.length(); i++) {
                        String translatedText = jsonArray.getJSONObject(i).getString("translatedText");
                        if (log.isDebugEnabled()) {
                            log.debug("translated \"{}\" to \"{}\"", texts.get(i), translatedText);
                        }
                        texts.set(i, translatedText);
                    }
                }
            } finally { // http://java.sun.com/j2se/1.5.0/docs/guide/net/http-keepalive.html
                connection.getInputStream().close();
                if (connection.getErrorStream() != null) {
                    connection.getErrorStream().close();
                    if (parametersWriter != null) {
                        parametersWriter.close();
                    }
                }
            }
        } catch (UnsupportedEncodingException ex) {
            throw new MappingException("malformed workflow configuration for "+getClass().getName(), ex);
        } catch (MalformedURLException ex) {
            throw new MappingException("malformed workflow configuration for "+getClass().getName(), ex);
        } catch (ProtocolException ex) {
            throw new MappingException("malformed workflow configuration for "+getClass().getName(), ex);
        } catch (JSONException ex) {
            throw new IOException("json protocol error", ex);
        }
    }

    private static List<Property> getProperties(Node node, String[] elements) throws RepositoryException {
        List<Property> properties = new LinkedList<Property>();
        if (elements.length > 1) {
            String[] subElements = Arrays.copyOfRange(elements, 1, elements.length);
            NodeIterator children = node.getNodes(elements[0]);
            while (children.hasNext()) {
                Node child = children.nextNode();
                properties.addAll(getProperties(child, subElements));
            }
        } else {
            if (node.hasProperty(elements[0])) {
                properties.add(node.getProperty(elements[0]));
            }
        }
        return properties;
    }
}
