/*
 * Copyright 2007 Hippo
 *
 * Licensed under the Apache License, Version 2.0 (the  "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.frontend;

import org.apache.wicket.Component;
import org.apache.wicket.RestartResponseAtInterceptPageException;
import org.apache.wicket.Session;
import org.apache.wicket.authorization.Action;
import org.apache.wicket.authorization.IAuthorizationStrategy;
import org.apache.wicket.util.value.ValueMap;

/**
 * Base class for an authenticated application, i.e. only user authenticated
 * against the repository get to see the application. Unauthenticated users
 * are presented a sign in page.
 *
 */
public class AuthenticatedMain extends Main {

	@Override
	protected void init() {
		super.init();
		
        getSecuritySettings().setAuthorizationStrategy(new IAuthorizationStrategy()
        {
            public boolean isActionAuthorized(Component component, Action action)
            {
                return true;
            }

            public boolean isInstantiationAuthorized(Class componentClass)
            {
                if (IAuthenticatedWebPage.class.isAssignableFrom(componentClass))
                {
                    UserSession session = (UserSession)Session.get();
                    ValueMap credentials = session.getCredentials();
                    String username = credentials.getString("username");
                    
                    if (username == null || username.equals("") || username.equals("anonymous")) {
                        // Force sign in
                    	// TODO: make sign in page class configurable
                        throw new RestartResponseAtInterceptPageException(SignIn.class);
                    }

                }
                return true;
            }
        });
		
	}
	
	@Override
	public Class getHomePage() {
		return AuthenticatedHome.class;
	}

	

}
