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
package org.hippoecm.hst.component.support.forms;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Random;
import java.util.Map.Entry;

import javax.jcr.ItemNotFoundException;
import javax.jcr.LoginException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.jcr.Value;

import org.apache.jackrabbit.uuid.UUID;
import org.hippoecm.hst.component.support.ocm.BaseHstComponent;
import org.hippoecm.hst.core.component.HstComponentException;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.hst.core.container.ContainerConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BaseFormHstComponent extends BaseHstComponent{

    static Logger log = LoggerFactory.getLogger(BaseFormHstComponent.class);
    
    public final static String DEFAULT_UUID_NAME = "u_u_i_d";
    public final static String DEFAULT_STORED_FORMS_LOCATION = "formdata";
    public final static String DEFAULT_FORMDATA_TYPE = "hst:formdata";

    public final static String HST_PARAMETERNAMES = "hst:parameternames";
    public final static String HST_PARAMETERVALUES = "hst:parametervalues";
    public final static String HST_CREATIONTIME =  "hst:creationtime";
    public final static String HST_PREDECESSOR =  "hst:predecessor";

    public final static String DEFAULT_WRITABLE_USERNAME_PROPERTY = "writable.repository.user.name";
    public final static String DEFAULT_WRITABLE_PASSWORD_PROPERTY = "writable.repository.password";
    
   
    
    @Override
    public void doAction(HstRequest request, HstResponse response) throws HstComponentException {
        super.doAction(request, response);
        // remove the uuid from the renderparameter again
        response.setRenderParameter(DEFAULT_UUID_NAME, (String)null);
    }

    /**
     * This method tries to repopulate an earlier posted form that was stored in the repository.
     * 
     * Only when there is a request parameter containing the correct uuid, you can re-populate it.
     * 
     * @param request the current hstRequest
     * @param formMap the formMap that will be populated
     */
    protected void populate(HstRequest request, FormMap formMap){
        if(formMap == null) {
            log.warn("FormMap is null so can not be populated");
            return;
        }
        if(request.getParameter(DEFAULT_UUID_NAME) != null) {
            String uuid = request.getParameter(DEFAULT_UUID_NAME);
            try {
                // check whether it is a valid uuid. Although uuidObj is not used afterwards, keep it here for parsing the uuid, if it fails
                // an IllegalArgumentException is thrown
                @SuppressWarnings("unused")
                UUID uuidObj = UUID.fromString(uuid);
                Session session = request.getRequestContext().getSession();
                Node persistedFormData = session.getNodeByUUID(uuid);
                
                if(persistedFormData.hasProperty(HST_PREDECESSOR)) {
                    formMap.setPrevious(persistedFormData.getProperty(HST_PREDECESSOR).getString());
                }
                if(persistedFormData.hasProperty(HST_PARAMETERNAMES) && persistedFormData.hasProperty(HST_PARAMETERVALUES)) {
                    Value[] names =  persistedFormData.getProperty(HST_PARAMETERNAMES).getValues();
                    Value[] values =  persistedFormData.getProperty(HST_PARAMETERVALUES).getValues();
                    if(names.length != values.length) {
                        log.warn("Ambiguous stored formfields because there are unequal names & values. Return unpopulated map");
                        return;
                    } else {
                        for(int i = 0 ; i < names.length ; i ++ ){
                            formMap.addFormField(names[i].getString(), values[i].getString());
                        }
                        log.debug("Succesfully repopulated formerly persisted form");
                       
                    }
                }
                return;
            } catch(IllegalArgumentException e){
                log.warn("Not a valid uuid. Return");
            } catch (LoginException e) {
                log.warn("LoginException '{}'. Return" , e.getMessage());
            } catch (ItemNotFoundException e) {
                log.warn("ItemNotFoundException '{}' while trying to retrieve persisted formdata. Return" , e.getMessage());
            } catch (RepositoryException e) {
                log.warn("RepositoryException '{}'. Return" , e.getMessage());
            }
            return;
        } else {
            log.debug("No uuid in request parameter. No form to populate");
            return;
        }
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
    
    protected void persistFormMap(HstRequest request, HstResponse response, FormMap formMap, StoreFormResult storeFormResult) throws HstComponentException { 
            Session impersonated = null;
            try {
                Session session = request.getRequestContext().getSession();
                String writableUserProperty = getWritableUserName();
                String writablePasswordProperty = getWritablePassword();
                ContainerConfiguration config = request.getRequestContext().getContainerConfiguration();

                String username = config.getString(writableUserProperty);
                String password = config.getString(writablePasswordProperty);
                if(username == null || password == null) {
                    log.error("Cannot retrieve a writable user for '{}' and '{}'",writableUserProperty, writablePasswordProperty);
                    return;
                }
                try {
                    impersonated = session.impersonate(new SimpleCredentials(username, password.toCharArray()));
                } catch (RepositoryException e){
                    log.error("Cannot impersonate a session with '{}' and '{}'", username, password);
                    return;
                }
                    
                Node rootNode = impersonated.getRootNode();
                Node formData = null;
                if(!rootNode.hasNode(getFormDataNodeName())) {
                    formData = rootNode.addNode(getFormDataNodeName(), "nt:unstructured");
                    addInitialStructure(formData);
                } else {
                    formData = rootNode.getNode(getFormDataNodeName());
                }
                
                Node randomNode = createRandomNode(formData);
                Node postedFormDataNode = randomNode.addNode("tick_"+System.currentTimeMillis(), getFormDataNodeType() );
                postedFormDataNode.setProperty(HST_CREATIONTIME, Calendar.getInstance());
                // if there is a previously stored node of this form, set this uuid as predecessor
                // TODO the  colon ':' is configurable, so must not be hardcode here
                if(request.getParameter(request.getReferenceNamespace()+":"+DEFAULT_UUID_NAME) != null) {
                    postedFormDataNode.setProperty(HST_PREDECESSOR, request.getParameter(request.getReferenceNamespace()+":"+DEFAULT_UUID_NAME));
                }

                postedFormDataNode.addMixin("mix:referenceable");
                List<String> names = new ArrayList<String>();
                List<String> values = new ArrayList<String>();
                for(Entry<String, String> entry : formMap.getFormMap().entrySet() ) {
                    names.add(entry.getKey());
                    String value = entry.getValue() == null ? "" : entry.getValue();
                    values.add(value);
                }
                
                postedFormDataNode.setProperty(HST_PARAMETERNAMES, names.toArray(new String[names.size()]));
                postedFormDataNode.setProperty(HST_PARAMETERVALUES, values.toArray(new String[values.size()]));
                rootNode.save();
                response.setRenderParameter(DEFAULT_UUID_NAME, postedFormDataNode.getUUID());
                if(storeFormResult != null) {
                    storeFormResult.populateResult(postedFormDataNode);
                }
            } catch (LoginException e) {
               throw new HstComponentException("LoginExceptionExeption during storing form data: ", e);
            } catch (RepositoryException e) {
               throw new HstComponentException("RepositoryException during storing form data: ", e);
            } finally {
                if(impersonated != null) {
                    impersonated.logout();
                }
            }
    }
    
    /**
     * If you have a different property for you writable user instead of the default {@link #DEFAULT_WRITABLE_PASSWORD_PROPERTY}, then override
     * this method
     * @return the property name of the password of a writable user
     */
    protected String getWritablePassword() {
        return DEFAULT_WRITABLE_PASSWORD_PROPERTY;
    }

    /**
     * If you have a different property for you writable user instead of the default {@link #DEFAULT_WRITABLE_USERNAME_PROPERTY}, then override
     * this method
     * @return the property name of the username of a writable user
     */
    protected String getWritableUserName() {
        return DEFAULT_WRITABLE_USERNAME_PROPERTY;
    }

    /**
     * Override this method if you need a different location for storing form data
     * @return default repository location for stored nodes
     */
    protected String getFormDataNodeName(){
        return DEFAULT_STORED_FORMS_LOCATION;
    }
    
    /**
     * Override this method if you need a different node type
     * @return default node type to store form data
     */
    protected String getFormDataNodeType(){
        return DEFAULT_FORMDATA_TYPE;
    }
    
    
    private void addInitialStructure(Node formData) throws RepositoryException {
        char a = 'a';
        for(int i = 0; i < 26 ; i ++) {
          Node letter =  formData.addNode(Character.toString((char)(a+i)), "nt:unstructured");
          for(int j = 0; j < 26 ; j ++) {
              letter.addNode(Character.toString((char)(a+j)), "nt:unstructured");
            }
        }
    }
    
    private Node createRandomNode(Node formData)throws RepositoryException {
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
                result = result.addNode(Character.toString((char)(a+r)));
            }
        }
        return result;
    }
    
    
}
