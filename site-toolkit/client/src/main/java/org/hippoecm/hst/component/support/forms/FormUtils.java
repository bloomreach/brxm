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
package org.hippoecm.hst.component.support.forms;

import java.io.IOException;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Random;
import java.util.UUID;

import javax.jcr.Credentials;
import javax.jcr.ItemNotFoundException;
import javax.jcr.LoginException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.servlet.http.HttpServletRequest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.commons.lang.StringUtils;
import org.hippoecm.hst.core.component.HstComponentException;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.hst.site.HstServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hippoecm.hst.core.component.HstRequest.RESOURCE_PHASE;
import static org.hippoecm.hst.site.HstServices.getComponentManager;
import static org.onehippo.repository.util.JcrConstants.MIX_REFERENCEABLE;

public class FormUtils {

    static Logger log = LoggerFactory.getLogger(FormUtils.class);

    public static final String PATH_SEPARATOR = "/";

    public static final String FORM_DATA_FLAT_STORAGE_CONFIG_PROP = "form.data.flat.storage";

    public final static String DEFAULT_UUID_NAME = "u_u_i_d";
    public final static String DEFAULT_STORED_FORMS_LOCATION = "formdata";
    public final static String DEFAULT_FORMDATA_CONTAINER = "hst:formdatacontainer";
    public final static String DEFAULT_FORMDATA_TYPE = "hst:formdata";

    public static final String HST_FORM_ID = "hst:formid";
    public final static String HST_CREATIONTIME = "hst:creationtime";
    public final static String HST_PREDECESSOR = "hst:predecessor";
    public final static String HST_FORM_DATA_NODE = "hst:formfieldvalue";
    public static final String HST_FORM_FIELD_DATA = "hst:formfielddata";
    public static final String HST_FORM_FIELD_MESSAGES = "hst:formfieldmessages";
    public static final String HST_FORM_FIELD_NAME = "hst:formfieldname";

    public static final String MIXIN_FORM_DATA_PAYLOAD = "hst:formdatapayload";
    public static final String PROPERTY_FORM_DATA_PAYLOAD = "hst:payload";

    public static final String HST_SEALED = "hst:sealed";

    private final static Object mutex = new Object();


    /**
     * Returns a populated FormaMap for some form data node if that node can be found with the u_u_i_d parameter
     * on the <code>request</code>. If no such form data node can be found, an empty {@link FormMap} will be returned
     *
     * @param request the {@link HttpServletRequest}
     * @return a populated {@link FormMap} for the formdata node belonging to the UUID from request parameter u_u_i_d
     * If there is no formdata for the UUID or no u_u_i_d parameter, just an empty {@link FormMap} object will be
     * returned
     * @see {@link #populate(javax.servlet.http.HttpServletRequest, FormMap)}
     */
    public static FormMap getFormMap(HttpServletRequest request) {
        FormMap formMap = new FormMap();
        populate(request, formMap);
        return formMap;
    }

    /**
     * This method tries to repopulate an earlier posted form that was stored in the repository.
     * <p>
     * Only when there is a request parameter containing the correct uuid, it can be re-populated.
     *
     * @param request the {@link HttpServletRequest}
     * @param formMap a not yet populated FormMap object (just new FormMap())
     */
    public static void populate(final HttpServletRequest request, final FormMap formMap) {
        if (formMap == null) {
            log.warn("FormMap is null so can not be populated");
            return;
        }
        if (request.getParameter(DEFAULT_UUID_NAME) != null) {
            final String uuid = request.getParameter(DEFAULT_UUID_NAME);
            Session session = null;
            try {
                validateId(uuid);
                session = getWritableSession();
                final Node persistedFormData = session.getNodeByIdentifier(uuid);
                // check if form is sealed
                if (persistedFormData.hasProperty(HST_SEALED) && persistedFormData.getProperty(HST_SEALED).getBoolean()) {
                    log.debug("From is sealed, not allowed to read data");
                    formMap.setSealed(true);
                    return;
                }

                if (persistedFormData.hasProperty(HST_PREDECESSOR)) {
                    formMap.setPrevious(persistedFormData.getProperty(HST_PREDECESSOR).getString());
                }
                // fetch previously stored form field data (nodes)
                if (persistedFormData.isNodeType(MIXIN_FORM_DATA_PAYLOAD)) {
                    if (persistedFormData.hasProperty(PROPERTY_FORM_DATA_PAYLOAD)) {
                        MapStringFormField mapStringFormField = new ObjectMapper().readValue(persistedFormData.getProperty(PROPERTY_FORM_DATA_PAYLOAD).getString(), MapStringFormField.class);
                        // do not set the FormMap.formMap because we do not know what invocations for this #populate method already
                        // did add. Instead loop through the mapStringFormField items and populate the form map
                        for (FormField formField : mapStringFormField.values()) {
                            formMap.addFormField(formField);
                        }
                    }
                } else {
                    // old format
                    if (persistedFormData.hasNodes()) {
                        NodeIterator fieldIterator = persistedFormData.getNodes(HST_FORM_DATA_NODE);
                        while (fieldIterator.hasNext()) {
                            Node fieldNode = fieldIterator.nextNode();

                            // sanity check (property is mandatory)
                            if (fieldNode.hasProperty(HST_FORM_FIELD_NAME)) {
                                // create field (even if we do not have values)
                                String fieldName = fieldNode.getProperty(HST_FORM_FIELD_NAME).getString();
                                FormField field = new FormField(fieldName);
                                formMap.addFormField(field);
                                if (fieldNode.hasProperty(HST_FORM_FIELD_DATA)) {
                                    Value[] values = fieldNode.getProperty(HST_FORM_FIELD_DATA).getValues();
                                    for (Value value : values) {
                                        field.addValue(value.getString());
                                    }
                                }
                                if (fieldNode.hasProperty(HST_FORM_FIELD_MESSAGES)) {
                                    Value[] values = fieldNode.getProperty(HST_FORM_FIELD_MESSAGES).getValues();
                                    for (Value value : values) {
                                        field.addMessage(value.getString());
                                    }
                                }
                            }
                        }
                    }
                }

            } catch (IllegalArgumentException e) {
                log.warn("Not a valid uuid. Return");
            } catch (LoginException e) {
                log.warn("LoginException '{}'. Return", e.getMessage());
            } catch (ItemNotFoundException e) {
                log.warn("ItemNotFoundException '{}' while trying to retrieve persisted formdata. Return", e.getMessage());
            } catch (RepositoryException e) {
                log.warn("RepositoryException '{}'. Return", e.getMessage());
            } catch (IOException e) {
                throw new HstComponentException("IOException while trying to retrieve persisted formdata. Return ", e);
            } finally {
                if (session != null) {
                    session.logout();
                }
            }
        } else {
            log.debug("No uuid in request parameter. No form to populate");
        }
    }

    /**
     * This method tries to repopulate an earlier posted form that was stored in the repository.
     * <p>
     * Only when there is a request parameter containing the correct uuid, you can re-populate it.
     *
     * @param request the current hstRequest
     * @param formMap the formMap that will be populated
     * @see #populate(javax.servlet.http.HttpServletRequest, FormMap) rather use {@link
     * #populate(javax.servlet.http.HttpServletRequest, FormMap)} instead
     * of this method
     */
    @SuppressWarnings("unused")
    public static void populate(HstRequest request, FormMap formMap) {
        populate((HttpServletRequest)request, formMap);
    }

    /**
     * @see {@link #persistFormMap(String, String, HstRequest, HstResponse, FormMap, StoreFormResult, boolean)} with
     * {@code formDataNodePath} = null, {@code formId} = null annd {@code includeRenderParameter} = true
     */
    @SuppressWarnings("unused")
    public static void persistFormMap(HstRequest request, HstResponse response, FormMap formMap, StoreFormResult storeFormResult) throws HstComponentException {
        persistFormMap(null, null, request, response, formMap, storeFormResult, true);
    }

    /**
     * Facility to temporarily store submitted form data which needs to be accessed in the rendering phase again. This
     * method
     * add the uuid of the newly created jcr node on the response as a render parameter
     *
     * @param formDataNodePath       form data node path relative to the root form data node {@code
     *                               FormUtils#DEFAULT_STORED_FORMS_LOCATION}
     *                               to store the result in. If {@code null}, a random location will be picked
     * @param formId                 optional value for a formId. If {@code null}, no formId will be stored
     * @param request                the request
     * @param response               the response
     * @param formMap                the form names + values to temporarily store
     * @param storeFormResult        an object to store some result of the data persisting, for example the uuid of the
     *                               created node
     * @param includeRenderParameter if {@code true}, {@link HstResponse#setRenderParameter(String, String)} will be
     *                               invoked with
     *                               param {@link #DEFAULT_UUID_NAME} and with value the identifier of the stored form
     *                               map
     * @throws HstComponentException when the storing of the formdata fails
     */
    public static void persistFormMap(final String formDataNodePath,
                                      final String formId,
                                      final HstRequest request,
                                      final HstResponse response,
                                      final FormMap formMap,
                                      final StoreFormResult storeFormResult,
                                      final boolean includeRenderParameter) throws HstComponentException {
        Session session = null;
        try {
            session = getWritableSession();
            final Node rootNode = session.getRootNode();
            final Node formData;
            if (rootNode.hasNode(DEFAULT_STORED_FORMS_LOCATION)) {
                formData = rootNode.getNode(DEFAULT_STORED_FORMS_LOCATION);
                if (formData.getNodes().getSize() < 10) {
                    synchronized (mutex) {
                        if (formData.getNodes().getSize() < 10) {
                            // only some initial structure seems to be there, add what is missing (just checking
                            // size == 0 is not ok because some downstream project may for example already add '/permanent'
                            addInitialStructure(formData);
                            session.save();
                        }
                    }
                }
            } else {
                synchronized (mutex) {
                    if (!rootNode.hasNode(DEFAULT_STORED_FORMS_LOCATION)) {
                        formData = rootNode.addNode(DEFAULT_STORED_FORMS_LOCATION, DEFAULT_FORMDATA_CONTAINER);
                        addInitialStructure(formData);
                        session.save();
                    } else {
                        formData = rootNode.getNode(DEFAULT_STORED_FORMS_LOCATION);
                    }
                }
            }

            final Node postedFormDataNode;
            if (formDataNodePath == null) {
                final Node randomNode = createRandomNode(formData);
                postedFormDataNode = randomNode.addNode("tick_" + System.currentTimeMillis(), DEFAULT_FORMDATA_TYPE);
            } else {
                postedFormDataNode = createFormDataNode(formData, formDataNodePath);
            }

            if (formId != null) {
                postedFormDataNode.setProperty(HST_FORM_ID, formId);
            }

            postedFormDataNode.setProperty(HST_CREATIONTIME, Calendar.getInstance());
            postedFormDataNode.setProperty(HST_SEALED, formMap.isSealed());
            // if there is a previously stored node of this form, set this uuid as predecessor
            // TODO the  colon ':' is configurable, so must not be hardcoded here
            if (request.getParameter(request.getReferenceNamespace() + ":" + DEFAULT_UUID_NAME) != null) {
                postedFormDataNode.setProperty(HST_PREDECESSOR, request.getParameter(request.getReferenceNamespace() + ":" + DEFAULT_UUID_NAME));
            }

            postedFormDataNode.addMixin(MIX_REFERENCEABLE);

            if (getComponentManager().getContainerConfiguration().getBoolean(FORM_DATA_FLAT_STORAGE_CONFIG_PROP, true)) {
                postedFormDataNode.addMixin(MIXIN_FORM_DATA_PAYLOAD);
                final String json = new ObjectMapper().writeValueAsString(formMap.getFormMap());
                postedFormDataNode.setProperty(PROPERTY_FORM_DATA_PAYLOAD, json);
            } else {
                for (Entry<String, FormField> entry : formMap.getFormMap().entrySet()) {
                    FormField field = entry.getValue();
                    Node fieldNode = postedFormDataNode.addNode(HST_FORM_DATA_NODE, HST_FORM_DATA_NODE);
                    fieldNode.setProperty(HST_FORM_FIELD_NAME, field.getName());
                    List<String> valueList = field.getValueList();
                    if (valueList.size() > 0) {
                        fieldNode.setProperty(HST_FORM_FIELD_DATA, valueList.toArray(new String[valueList.size()]));
                    }
                    List<String> messages = field.getMessages();
                    if (messages.size() > 0) {
                        fieldNode.setProperty(HST_FORM_FIELD_MESSAGES, messages.toArray(new String[messages.size()]));
                    }
                }
            }

            session.save();
            if (RESOURCE_PHASE.equals(request.getLifecyclePhase())) {
                log.debug("During {} a request does not (yet) support set render parameter. Skipping setting render parameter", RESOURCE_PHASE);
            } else {
                if (includeRenderParameter) {
                    response.setRenderParameter(DEFAULT_UUID_NAME, postedFormDataNode.getIdentifier());
                }
            }
            if (storeFormResult != null) {
                storeFormResult.populateResult(postedFormDataNode);
            }
        } catch (LoginException e) {
            throw new HstComponentException("LoginException  during storing form data: ", e);
        } catch (RepositoryException e) {
            throw new HstComponentException("RepositoryException during storing form data: ", e);
        } catch (JsonProcessingException e) {
            throw new HstComponentException("JsonProcessingException during storing form data: ", e);
        } finally {
            if (session != null) {
                session.logout();
            }
        }
    }

    private static void addInitialStructure(Node formData) throws RepositoryException {
        char a = 'a';
        for (int i = 0; i < 26; i++) {
            String firstLevelFolder = Character.toString((char)(a + i));
            final Node firstLevelFolderNode;
            if (formData.hasNode(firstLevelFolder)) {
                firstLevelFolderNode = formData.getNode(firstLevelFolder);
            } else {
                firstLevelFolderNode = formData.addNode(firstLevelFolder, DEFAULT_FORMDATA_CONTAINER);
            }
            for (int j = 0; j < 26; j++) {
                String secondLevelFolder = Character.toString((char)(a + j));
                if (!firstLevelFolderNode.hasNode(secondLevelFolder)) {
                    firstLevelFolderNode.addNode(secondLevelFolder, DEFAULT_FORMDATA_CONTAINER);
                }
            }
        }
    }

    private static Node createRandomNode(Node formData) throws RepositoryException {
        Node result = formData;
        char a = 'a';
        Random rand = new Random();
        boolean needCheck = true;
        for (int i = 0; i < 4; i++) {
            int r = rand.nextInt(26);
            if (needCheck && result.hasNode(Character.toString((char)(a + r)))) {
                result = result.getNode(Character.toString((char)(a + r)));
            } else {
                needCheck = false;
                result = result.addNode(Character.toString((char)(a + r)), DEFAULT_FORMDATA_CONTAINER);
            }
        }
        return result;
    }

    public static Node createFormDataNode(Node formDataBaseNode, String path) throws RepositoryException {
        String[] pathComps = StringUtils.split(path, PATH_SEPARATOR);

        if (pathComps == null || pathComps.length == 0) {
            throw new IllegalArgumentException("The path must not be empty: '" + path + "'.");
        }

        // construct containers for element but the last
        Node curNode = formDataBaseNode;
        for (int i = 0; i < pathComps.length - 1; i++) {

            // in case of SNS, use first node as storage (SNS is not supported within form data container)
            String currentPath = pathComps[i];
            if (currentPath.indexOf('[') != -1) {
                currentPath = currentPath.substring(0, currentPath.indexOf('['));
            }

            if (curNode.hasNode(currentPath)) {
                curNode = curNode.getNode(currentPath);
            } else {
                curNode = curNode.addNode(currentPath, FormUtils.DEFAULT_FORMDATA_CONTAINER);
            }
        }

        // construct form data node from last element (normally tick_<timestamp> but may be customized)
        String pathComp = pathComps[pathComps.length - 1];
        if (pathComp.indexOf('[') != -1) {
            pathComp = pathComp.substring(0, pathComp.indexOf('['));
        }
        Node formDataNode = curNode.addNode(pathComp, FormUtils.DEFAULT_FORMDATA_TYPE);
        return formDataNode;
    }

    /**
     * Validates an uuid. If invalid uuid is passed,  IllegalArgumentException exception will be thrown
     *
     * @param uuid uuid to validate
     * @throws IllegalArgumentException thrown on invalid uuid
     */
    public static void validateId(final String uuid) throws IllegalArgumentException {
        UUID.fromString(uuid);
    }

    public static Session getWritableSession() throws RepositoryException {
        if (HstServices.isAvailable()) {
            Credentials defaultCredentials = HstServices.getComponentManager().getComponent(Credentials.class.getName() + ".writable");
            Repository repository = HstServices.getComponentManager().getComponent(Repository.class.getName());
            Session session = null;
            if (repository != null) {
                if (defaultCredentials != null) {
                    session = repository.login(defaultCredentials);
                } else {
                    session = repository.login();
                }
            }
            return session;
        } else {
            throw new HstComponentException("Can not get a writable sessions because HstServices are not available");
        }


    }

    public static class MapStringFormField extends LinkedHashMap<String, FormField> {
    }
}
