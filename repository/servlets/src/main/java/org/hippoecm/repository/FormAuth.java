/*
 *  Copyright 2010-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.repository;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;

import javax.jcr.LoginException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang.StringUtils;
import org.hippoecm.repository.api.HippoSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FormAuth {

    private static final Logger log = LoggerFactory.getLogger(FormAuth.class);
    private static final String AUTHORIZED = "cms-user-authorized";
    private static final String AUTHORIZATION_CREDENTIALS = "cms-user-credentials";


    public static final Session authorize(HttpServletRequest request, HippoRepository repository) throws ServletException, IOException {

        HttpSession httpSession = request.getSession();
        if(httpSession.getAttribute(AUTHORIZED) != null){
            return login(request, (SimpleCredentials) httpSession.getAttribute(AUTHORIZATION_CREDENTIALS), repository);
        }

        SimpleCredentials credentials = parseAuthorizationForm(request);
        if(credentials != null){
            Session hippoSession = login(request, credentials, repository);
            if(hippoSession != null){
                httpSession.setAttribute(AUTHORIZED, true);
                httpSession.setAttribute(AUTHORIZATION_CREDENTIALS, credentials);
                return hippoSession;
            }
        }

        return null;
    }

    public static Session login(HttpServletRequest request, SimpleCredentials credentials, HippoRepository repository){
        Session hippoSession;
        try {
            if (credentials.getUserID() == null || credentials.getUserID().length() == 0) {
                hippoSession = repository.login();
            } else {
                hippoSession = repository.login(credentials);
            }
            if (((HippoSession) hippoSession).getUser().isSystemUser()) {
                final InetAddress address = InetAddress.getByName(request.getRemoteHost());
                if (!address.isAnyLocalAddress() && !address.isLoopbackAddress()) {
                    throw new LoginException();
                }
            }
            return hippoSession;
        } catch (Exception e){
            return null;
        }
    }

    public static void logout(HttpServletRequest request){
        request.getSession().removeAttribute(AUTHORIZED);
        request.getSession().removeAttribute(AUTHORIZATION_CREDENTIALS);
    }

    public static final void showLoginPage(HttpServletRequest request, HttpServletResponse response, String message) throws ServletException, IOException {
        PrintWriter writer = response.getWriter();
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("text/html");

        writer.println("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\"");
        writer.println("    \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">");
        writer.println("<html xmlns=\"http://www.w3.org/1999/xhtml\">");
        writer.println("<head>");
        writer.println("  <title>Hippo Repository Browser - Login</title>");
        writer.println("  <meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\" />");
        writer.println("  <style type=\"text/css\">");
        writer.println("    h3 {margin:2px}");
        writer.println("    table.params {font-size:small}");
        writer.println("    td.header {text-align: left; vertical-align: top; padding: 10px;}");
        writer.println("    td {text-align: left}");
        writer.println("    th {text-align: left}");
        writer.println("    * {font-family: tahoma, arial, sans-serif, helvetica;}");
        writer.println("    * {font-size: 14px;}");
        writer.println("    form {margin-top: 10px}");
        writer.println("    div.message {font-size: small; color: red}");
        writer.println("    .hippo-header {background: #32629b; height: 25px; background-image: url(data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAB0AAAAYCAYAAAAGXva8AAAAGXRFWHRTb2Z0d2FyZQBBZG9iZSBJbWFnZVJlYWR5ccllPAAAAWZJREFUeNrsVtFRwkAQ9WjA60A6MB2YEqACsQKxA6xArCBSQbQCsQPoIHQQKojv6QuzZC4hkwQ/HHbmzYW73L3s7ts9XFEUV39t7kJ6VjtFivUIGA/J9+Opc85Oegxz4BbYmvevAZJ/4P23Xk5aT+XVJ8eGTTMg1cf1I+UhIvQtNkZdiY/Ci5EhXeN503IzCRMgY8jNUsxzOI+zslOkKcZphy8fK9e0e+BV4x64Ab6sBg6kEk7eRyBl2DE8As/0Er9fzPKW5x9yCiTA5Azl6Ct6ick30voTcDd453Eu1yPJV8r3L6kWfdcyaEFeCmpz1JHk+mKgsDKUs1JoSl8a7EhK/Kpt2dQQJiohr5R5eTsNdiSzqakpLNQY4pBwKooNNodRYD4PNXiGTEJgWT2oFqsWqT4bLUTK0EwC0meRLykKCW9Xc/tkre5Te8uIZF7mwti7zbW5jaq2NKVS33svf1f+Fem3AAMA5G0u/dkz/GsAAAAASUVORK5CYII=); background-repeat: no-repeat; background-position-x: 5px;");
        writer.println("  </style>");
        writer.println("</head>");
        writer.println("<body>");
        writer.println("  <div class=\"hippo-header\"></div>");
        writer.println("  <table summary=\"infotable\">");
        writer.println("    <tr>");
        writer.println("      <td class=\"header\">");
        writer.println("        <h3>Log in</h3>");
        writer.println("        <div class=\"message\">" + message + "</div>");
        writer.println("          <form method=\"post\" action=\"\" accept-charset=\"UTF-8\">");
        writer.println("            <table style=\"params\" summary=\"searching\">");
        writer.println("              <tr>");
        writer.println("                <th>Username: </th>");
        writer.println("                <td>");
        writer.println("                    <input name=\"username\" type=\"text\" size=\"15\"/>");
        writer.println("                </td>");
        writer.println("              </tr>");
        writer.println("              <tr>");
        writer.println("                <th>Password: </th>");
        writer.println("                <td>");
        writer.println("                  <input name=\"password\" type=\"password\" size=\"15\"/>");
        writer.println("                </td>");
        writer.println("              </tr>");
        writer.println("              <tr>");
        writer.println("                <th>&nbsp;</th>");
        writer.println("                <td>");
        writer.println("                  <input type=\"submit\" value=\"Log in\"/>");
        writer.println("                </td>");
        writer.println("              </tr>");
        writer.println("            </table>");
        writer.println("          </form>");
        writer.println("        </td>");
        writer.println("      </tr>");
        writer.println("    </table>");
        writer.println("  </body>");
        writer.println("</html>");

        writer.close();
    }



    public static SimpleCredentials parseAuthorizationForm(HttpServletRequest request) throws ServletException, IOException {
        String username = request.getParameter("username");
        String password = request.getParameter("password");

        if(StringUtils.isEmpty(username) || StringUtils.isEmpty(password)){
            return null;
        }
        return new SimpleCredentials(username, password.toCharArray());
    }

}
