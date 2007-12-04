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

    /**
	 * Constructor
	 */
	public SignIn()
	{
		this(null);
	}

	/**
	 * Constructor
	 * 
	 * @param parameters
	 *            The page parameters
	 */
	public SignIn(final PageParameters parameters)
	{
		// Create feedback panel and add to page
		final FeedbackPanel feedback = new FeedbackPanel("feedback");

		add(feedback);

		// Add sign-in form to page, passing feedback panel as validation error
		// handler
		add(new SignInForm("signInForm"));
	}

	/**
	 * Sign in form
	 * 
	 * @author Jonathan Locke
	 */
	public final class SignInForm extends Form
	{
        private static final long serialVersionUID = 1L;

        // El-cheapo model for form
		private final ValueMap properties = new ValueMap();

		/**
		 * Constructor
		 * 
		 * @param id
		 *            id of the form component
		 */
		public SignInForm(final String id)
		{
			super(id);

			// Attach textfield components that edit properties map model
			add(new TextField("username", new PropertyModel(properties, "username")));
			add(new PasswordTextField("password", new PropertyModel(properties, "password")));
		}

		/**
		 * @see org.apache.wicket.markup.html.form.Form#onSubmit()
		 */
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
                // Form method that will notify feedback panel
                // Try the component based localizer first. If not found try the
                // application localizer. Else use the default
                final String errmsg = getLocalizer().getString("loginError", this,
                        message);
                error(errmsg);
            }
		    
		}
	}
}
