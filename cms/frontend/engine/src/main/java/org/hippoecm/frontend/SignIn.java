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

import javax.jcr.LoginException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.wicket.Application;
import org.apache.wicket.PageParameters;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.PasswordTextField;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.util.value.ValueMap;
import org.hippoecm.repository.HippoRepository;


/**
 * Basic sign in page to let a user sign in to the repository.
 *
 */
public final class SignIn extends WebPage
{
    private static final long serialVersionUID = 1L;

    public SignIn()
    {
        this(null);
    }

    public SignIn(final PageParameters parameters)
    {
        final FeedbackPanel feedback = new FeedbackPanel("feedback");
        add(feedback);
        add(new SignInForm("signInForm"));
    }

    public final class SignInForm extends Form
    {
        private static final long serialVersionUID = 1L;

        private final ValueMap properties = new ValueMap();

        public SignInForm(final String id)
        {
            super(id);
            add(new TextField("username", new PropertyModel(properties, "username")));
            add(new PasswordTextField("password", new PropertyModel(properties, "password")));
        }

        public final void onSubmit()
        {
            Main main = (Main) Application.get();
            HippoRepository repository = main.getRepository();
            Session jcrSession = null;
            
            String username = properties.getString("username");
            String password = properties.getString("password");
            
            String message = "Unable to sign in";

            try {
                jcrSession = repository.login(username, password.toCharArray());
            } catch (LoginException e) {
                message += ": " + e;
                e.printStackTrace();
            } catch (RepositoryException e) {
                message += ": " + e;
                e.printStackTrace();
            }

            if (jcrSession != null) {
            
                UserSession userSession = (UserSession) getSession();
                ValueMap credentials = new ValueMap();
                credentials.add("username", username);
                credentials.add("password", password);
                
                userSession.setJcrSession(jcrSession, credentials);

                if (!continueToOriginalDestination())
                {
                    setResponsePage(getApplication().getHomePage());
                }
                
            }
            else
            {
                error(message);
            }
            
        }
    }
}
