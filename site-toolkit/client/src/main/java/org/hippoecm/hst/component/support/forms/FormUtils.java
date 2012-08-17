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
package org.hippoecm.hst.component.support.forms;

import java.util.Calendar;
import java.util.List;
import java.util.Map;
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

import org.hippoecm.hst.core.component.HstComponentException;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.hst.site.HstServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FormUtils {
    
    static Logger log = LoggerFactory.getLogger(FormUtils.class);

    public final static String DEFAULT_UUID_NAME = "u_u_i_d";
    public final static String DEFAULT_STORED_FORMS_LOCATION = "formdata";
    public final static String DEFAULT_FORMDATA_CONTAINER = "hst:formdatacontainer";
    public final static String DEFAULT_FORMDATA_TYPE = "hst:formdata";

    public final static String HST_CREATIONTIME =  "hst:creationtime";
    public final static String HST_PREDECESSOR =  "hst:predecessor";
    public final static String HST_FORM_DATA_NODE =  "hst:formfieldvalue";
    public static final String HST_FORM_FIELD_DATA = "hst:formfielddata";
    public static final String HST_FORM_FIELD_MESSAGES = "hst:formfieldmessages";
    public static final String HST_FORM_FIELD_NAME = "hst:formfieldname";

    public static final String HST_SEALED = "hst:sealed";

    private final static Object mutex = new Object();


    /**
     *
     * This method tries to repopulate an earlier posted form that was stored in the repository.
     *
     * Only when there is a request parameter containing the correct uuid, it can be re-populated.
     *
     * @param request the {@link HttpServletRequest}
     * @param formMap the formMap object to fill
     */
    public static void populate(HttpServletRequest request, FormMap formMap) {
        if(formMap == null) {
            log.warn("FormMap is null so can not be populated");
            return;
        }
        if(request.getParameter(DEFAULT_UUID_NAME) != null) {
            String uuid = request.getParameter(DEFAULT_UUID_NAME);
            Session session = null;
            try {
                validateId(uuid);
                session = getWritableSession();
                Node persistedFormData = session.getNodeByIdentifier(uuid);
                // check if form is sealed
                if(persistedFormData.hasProperty(HST_SEALED) && persistedFormData.getProperty(HST_SEALED).getBoolean()){
                    log.debug("From is sealed, not allowed to read data");
                    formMap.setSealed(true);
                    return;
                }

                if(persistedFormData.hasProperty(HST_PREDECESSOR)) {
                    formMap.setPrevious(persistedFormData.getProperty(HST_PREDECESSOR).getString());
                }
                // fetch previously stored form field data (nodes)
                if(persistedFormData.hasNodes()){
                    NodeIterator fieldIterator = persistedFormData.getNodes(HST_FORM_DATA_NODE);
                    while (fieldIterator.hasNext()) {
                        Node fieldNode = fieldIterator.nextNode();
                        if (fieldNode == null) {
                            continue;
                        }

                        // sanity check (property is mandatory)
                        if(fieldNode.hasProperty(HST_FORM_FIELD_NAME)){
                            // create field (even if we do not have values)hstt
                            String fieldName = fieldNode.getProperty(HST_FORM_FIELD_NAME).getString();
                            FormField field = new FormField(fieldName);
                            formMap.addFormField(field);
                            if(fieldNode.hasProperty(HST_FORM_FIELD_DATA)){
                                Value[] values = fieldNode.getProperty(HST_FORM_FIELD_DATA).getValues();
                                for (Value value : values) {
                                    field.addValue(value.getString());
                                }
                            }
                            if(fieldNode.hasProperty(HST_FORM_FIELD_MESSAGES)){
                                Value[] values = fieldNode.getProperty(HST_FORM_FIELD_MESSAGES).getValues();
                                for (Value value : values) {
                                    field.addMessage(value.getString());
                                }
                            }
                        }
                    }
                }

            } catch(IllegalArgumentException e){
                log.warn("Not a valid uuid. Return");
            } catch (LoginException e) {
                log.warn("LoginException '{}'. Return" , e.getMessage());
            } catch (ItemNotFoundException e) {
                log.warn("ItemNotFoundException '{}' while trying to retrieve persisted formdata. Return" , e.getMessage());
            } catch (RepositoryException e) {
                log.warn("RepositoryException '{}'. Return" , e.getMessage());
            } finally {
                if(session != null) {
                    session.logout();
                }
            }
        } else {
            log.debug("No uuid in request parameter. No form to populate");
        }
    }
    
    /**
     * This method tries to repopulate an earlier posted form that was stored in the repository.
     *
     * Only when there is a request parameter containing the correct uuid, you can re-populate it.
     *
     * @param request the current hstRequest
     * @param formMap the formMap that will be populated
     * @see #populate(javax.servlet.http.HttpServletRequest, FormMap) rather use {@link #populate(javax.servlet.http.HttpServletRequest, FormMap)} instead
     * of this method
     */
    public static void populate(HstRequest request, FormMap formMap) {
        populate((HttpServletRequest)request, formMap);
    }

    /**
     * Facility to temporarily store submitted form data which needs to be accessed in the rendering phase again. This method
     * add the uuid of the newly created jcr node on the response as a render parameter
     * @param request the request
     * @param response the response
     * @param formMap the form names + values to temporarily store
     * @param storeFormResult an object to store some result of the data persisting, for example the uuid of the created node
     * @throws HstComponentException when the storing of the formdata fails
     */

    public static void persistFormMap(HstRequest request, HstResponse response, FormMap formMap, StoreFormResult storeFormResult) throws HstComponentException {
            Session session = null;
            try {
                session = getWritableSession();
                Node rootNode = session.getRootNode();
                Node formData;
                if(!rootNode.hasNode(DEFAULT_STORED_FORMS_LOCATION)) {
                    synchronized (mutex) {
                        formData = rootNode.addNode(DEFAULT_STORED_FORMS_LOCATION, DEFAULT_FORMDATA_CONTAINER);
                        addInitialStructure(formData);
                    }
                } else {
                    formData = rootNode.getNode(DEFAULT_STORED_FORMS_LOCATION);
                }
                Node randomNode = createRandomNode(formData);
                Node postedFormDataNode = randomNode.addNode("tick_"+System.currentTimeMillis(), DEFAULT_FORMDATA_TYPE );
                postedFormDataNode.setProperty(HST_CREATIONTIME, Calendar.getInstance());
                postedFormDataNode.setProperty(HST_SEALED, formMap.isSealed());
                // if there is a previously stored node of this form, set this uuid as predecessor
                // TODO the  colon ':' is configurable, so must not be hardcoded here
                if(request.getParameter(request.getReferenceNamespace()+":"+DEFAULT_UUID_NAME) != null) {
                    postedFormDataNode.setProperty(HST_PREDECESSOR, request.getParameter(request.getReferenceNamespace()+":"+DEFAULT_UUID_NAME));
                }

                postedFormDataNode.addMixin("mix:referenceable");

                for(Entry<String, FormField> entry : formMap.getFormMap().entrySet() ) {
                    FormField field = entry.getValue();
                    Node fieldNode = postedFormDataNode.addNode(HST_FORM_DATA_NODE, HST_FORM_DATA_NODE);
                    fieldNode.setProperty(HST_FORM_FIELD_NAME, field.getName());
                    Map<String,String> values = field.getValues();
                    if(values.size() > 0){
                        fieldNode.setProperty(HST_FORM_FIELD_DATA, values.values().toArray(new String[values.size()]));
                    }
                    List<String> messages = field.getMessages();
                    if(messages.size() > 0){
                        fieldNode.setProperty(HST_FORM_FIELD_MESSAGES, messages.toArray(new String[messages.size()]));
                    }
                }
                session.save();
                response.setRenderParameter(DEFAULT_UUID_NAME, postedFormDataNode.getIdentifier());
                if(storeFormResult != null) {
                    storeFormResult.populateResult(postedFormDataNode);
                }
            } catch (LoginException e) {
               throw new HstComponentException("LoginException  during storing form data: ", e);
            } catch (RepositoryException e) {
               throw new HstComponentException("RepositoryException during storing form data: ", e);
            } finally {
                if(session != null) {
                    session.logout();
                }
            }
    }


    private static void addInitialStructure(Node formData) throws RepositoryException {
        char a = 'a';
        for(int i = 0; i < 26 ; i ++) {
          Node letter =  formData.addNode(Character.toString((char)(a+i)), DEFAULT_FORMDATA_CONTAINER);
          for(int j = 0; j < 26 ; j ++) {
              letter.addNode(Character.toString((char)(a+j)), DEFAULT_FORMDATA_CONTAINER);
            }
        }
    }

    private static Node createRandomNode(Node formData)throws RepositoryException {
        Node result = formData;
        char a = 'a';
        Random rand = new Random();
        boolean needCheck = true;
        for(int i = 0; i < 4 ; i++) {
            int r = rand.nextInt(26);
            if(needCheck && result.hasNode(Character.toString((char)(a+r)))) {
                result = result.getNode(Character.toString((char)(a+r)));
            } else {
                needCheck = false;
                result = result.addNode(Character.toString((char)(a+r)), DEFAULT_FORMDATA_CONTAINER);
            }
        }
        return result;
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

}
