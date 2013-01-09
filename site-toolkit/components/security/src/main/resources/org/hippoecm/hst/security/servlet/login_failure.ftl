<#--
  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)

  Licensed under the Apache License, Version 2.0 (the  "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS"
  BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
-->
<html>
  <head>
    <title>Login failure</title>
    <link rel="shortcut icon" href="hst/security/skin/images/hippo-cms.ico" type="image/x-icon"/>
    <link rel="stylesheet" type="text/css" href="hst/security/skin/screen.css" />
  </head>
  <body class="hippo-root" style="text-align:center">
    <div>
      <div class="hippo-login-panel">
        <form class="hippo-login-panel-form" name="signInForm" method="post" action="..${request.servletPath}/proxy">
          <h2><div class="hippo-global-hideme"><span>Hippo CMS 7</span></div></h2>
          <div class="hippo-login-form-container">
            <table>
              <tr>
                <td>
                  <p>${messages.getString("message.auth.failed")} ${j_username?html}.</p>
                </td>
              </tr>
              <tr>
                <td>
                  <p><a href="..${request.servletPath}/form">${messages.getString("message.try.again")}</a></p>
                </td>
              </tr>
            </table>
          </div>
        </form>
        <div class="hippo-login-panel-copyright">
          &copy; 1999-2012 Hippo B.V.
        </div>
      </div>
    </div>
  </body>
</html>
