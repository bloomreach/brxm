/*
 *  Copyright 2011-2017 Hippo B.V. (http://www.onehippo.com)
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
import java.net.URL;
import java.net.URLEncoder;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.ValueFactory;

import org.apache.commons.io.IOUtils;
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

    private static final Logger log = LoggerFactory.getLogger(TranslateWorkflowImpl.class);
    private static final String GOOGLE_TRANSLATE_URL_V2 = "https://www.googleapis.com/language/translate/v2";
    private static final Map<String, Serializable> AVAILABLE_HINTS = new HashMap<String, Serializable>(1) {{
        put("translate", true);
    }};

    private final Session session;
    private final Node subject;

    private String googleKey = null;
    private int timeout = 10 * 1000; // milliseconds

    public TranslateWorkflowImpl(WorkflowContext context, Session userSession, Session rootSession, Node subject)
            throws RemoteException, RepositoryException {
        this.session = rootSession;
        this.subject = session.getNodeByIdentifier(subject.getIdentifier());

        RepositoryMap config = context.getWorkflowConfiguration();
        googleKey = (String) config.get("google.key");

        String timeoutParameter = (String) config.get("timeout");
        if (timeoutParameter != null) {
            timeout = Integer.parseInt(timeoutParameter);
        }
    }

    @Override
    public Map<String, Serializable> hints() throws WorkflowException, RemoteException, RepositoryException {
        if (googleKey != null) {
            return AVAILABLE_HINTS;
        }
        return Collections.emptyMap();
    }

    /**
     * The hints method is not an actual workflow call, but a method by which information can be retrieved from the
     * workflow.  All implementations must implement this call as a pure function, no modification may be made, nor no
     * state may be maintained and and in principle no additional lookups of data is allowed.  This allows for caching
     * the result as long as the document on which the workflow operates isn't modified. By convention, keys that are
     * names or signatures of methods implemented by the workflow provide information to the application program whether
     * the workflow method is available this time, or will result in a WorkflowException.  The value for these keys will
     * often be a {@link Boolean} to indicate the enabled status of the method.<p/> Non-standard keys in this map should
     * be prefixed with the implementation package name using dot seperations.
     *
     * @param initializationPayload a map containing user context information relevant for the workflow
     * @return a map containing hints given by the workflow, the data in this map may be considered valid until the
     * document itself changes
     * @throws WorkflowException   thrown in case the implementing workflow encounters an error, this exception should
     *                             normally never be thrown by implementations for the hints method.
     * @throws RemoteException     a connection error with the repository
     * @throws RepositoryException a generic error communicating with the repository
     */
    @Override
    public Map<String, Serializable> hints(final Map<String, Serializable> initializationPayload) throws WorkflowException, RemoteException, RepositoryException {
        return hints();
    }

    @Override
    public void translate(String sourceLanguage, String targetLanguage, Set<String> fields) throws WorkflowException, RepositoryException, RemoteException {
        for (TranslatedProperty property : getPropertiesToTranslate(fields)) {
            property.translate(sourceLanguage, targetLanguage);
        }
        session.save();
    }

    protected void translate(String sourceLanguage, String targetLanguage, List<String> texts) throws WorkflowException, RepositoryException {
        try {
            final String parameters = createParamters(texts, targetLanguage, sourceLanguage);
            if (log.isDebugEnabled()) {
                log.debug("request to google translate \"{}\" with parameters \"{}\"", GOOGLE_TRANSLATE_URL_V2, parameters);
            }
            URL url = new URL(GOOGLE_TRANSLATE_URL_V2);
            HttpURLConnection connection = null;
            PrintWriter out = null;
            Reader in = null;
            try {
                connection = (HttpURLConnection) url.openConnection();
                connection.setConnectTimeout(timeout);
                connection.setReadTimeout(timeout);

                connection.setDoOutput(true);
                connection.setRequestMethod("POST");
                connection.addRequestProperty("X-HTTP-Method-Override", "GET");
                out = new PrintWriter(connection.getOutputStream());
                out.write(parameters);
                out.flush();

                final int status = connection.getResponseCode();
                if (status == 200) {
                    String charset = getCharset(connection.getHeaderField("Content-Type"));
                    in = new InputStreamReader(connection.getInputStream(), charset);
                    final String response = IOUtils.toString(in);
                    if (log.isDebugEnabled()) {
                        log.debug("response from google translate reads \"{}\"", response);
                    }
                    JSONObject jsonObject = new JSONObject(response);
                    JSONArray jsonArray = jsonObject.getJSONObject("data").getJSONArray("translations");
                    for (int i = 0; i < jsonArray.length(); i++) {
                        String translatedText = jsonArray.getJSONObject(i).getString("translatedText");
                        if (log.isDebugEnabled()) {
                            log.debug("translated \"{}\" to \"{}\"", texts.get(i), translatedText);
                        }
                        texts.set(i, translatedText);
                    }
                } else {
                    in = new InputStreamReader(connection.getErrorStream());
                    log.warn("Failed to translate field, google translate responded with status {}", status);
                    log.warn("Google response: {}", IOUtils.toString(in));
                }
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
                IOUtils.closeQuietly(in);
                IOUtils.closeQuietly(out);
            }
        } catch (JSONException | IOException ex) {
            session.refresh(false);
            throw new WorkflowException("Failed to translate document", ex);
        }
    }

    private String createParamters(final List<String> texts, final String targetLanguage, final String sourceLanguage) throws UnsupportedEncodingException {
        StringBuilder parameters = new StringBuilder();
        parameters.append("prettyprint=false&format=html&key=").append(googleKey);
        if (sourceLanguage != null && !sourceLanguage.isEmpty()) {
            parameters.append("&source=").append(sourceLanguage);
        }
        parameters.append("&target=").append(targetLanguage);

        for (String text : texts) {
            parameters.append("&q=").append(URLEncoder.encode(text, "UTF-8"));
        }
        return parameters.toString();
    }

    private String getCharset(final String contentType) {
        String charset = "UTF-8";
        for (String param : contentType.split(";")) {
            if (param.trim().startsWith("charset=")) {
                charset = param.split("=", 2)[1];
                break;
            }
        }
        return charset;
    }

    private Iterable<TranslatedProperty> getPropertiesToTranslate(Collection<String> fields) throws RepositoryException {
        List<TranslatedProperty> result = new ArrayList<>();
        for (String field : fields) {
            if (subject.hasProperty(field)) {
                result.add(new TranslatedProperty(subject.getProperty(field)));
            }
        }
        return result;
    }

    private final class TranslatedProperty {

        private final Property property;

        private TranslatedProperty(final Property property) {
            this.property = property;
        }

        private void translate(final String sourceLanguage, final String targetLanguage) throws RepositoryException, WorkflowException {
            List<String> values = getValues();
            if (!values.isEmpty()) {
                TranslateWorkflowImpl.this.translate(sourceLanguage, targetLanguage, values);
                setValues(values);
            }
        }

        private List<String> getValues() throws RepositoryException {
            if (property.isMultiple()) {
                final List<String> values = new ArrayList<>();
                for (Value value : property.getValues()) {
                    values.add(value.getString());
                }
                return values;
            }
            else {
                return Arrays.asList(property.getString());
            }
        }

        private void setValues(final List<String> values) throws RepositoryException {
            if (property.isMultiple()) {
                property.setValue(toValues(values));
            }
            else {
                property.setValue(values.get(0));
            }
        }

        private Value[] toValues(List<String> strings) throws RepositoryException {
            final List<Value> values = new ArrayList<>(strings.size());
            final ValueFactory valueFactory = property.getSession().getValueFactory();
            for (String string : strings) {
                values.add(valueFactory.createValue(string));
            }
            return values.toArray(new Value[values.size()]);
        }
    }
}
