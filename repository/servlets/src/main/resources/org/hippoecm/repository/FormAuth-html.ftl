<!DOCTYPE html>
<#--
  Copyright 2014 Hippo B.V. (http://www.onehippo.com)

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

${response.setContentType("text/html;charset=UTF-8")}


<html>
<head>
    <title>Hippo Repository Browser - Login</title>
    <meta http-equiv="Content-Type" content="text/html; charset=utf-8" />
    <style type="text/css">
      h3 {margin:2px}
      table.params {font-size:small}
      td.header {text-align: left; vertical-align: top; padding: 10px;}
      td {text-align: left}
      th {text-align: left}
      * {font-family: tahoma, arial, sans-serif, helvetica;}
      * {font-size: 14px;}
      form {margin-top: 10px}
      div.message {font-size: small; color: red}
      .hippo-header {background: #32629b url(${request.contextPath}/hippo-icon.png) no-repeat 5px 0; height: 25px;}
    </style>
  </head>
<body>
  <div class="hippo-header"></div>
  <table summary="infotable">
      <tr>
          <td class="header">
              <h3>Log in</h3>
              <div class="message">${message}</div>
                <form method="post" action="" accept-charset="UTF-8">
                    <table style="params" summary="searching">
                        <tr>
                            <th>Username: </th>
                            <td>
                                  <input name="username" type="text" size="15" autofocus="autofocus"/>
                              </td>
                          </tr>
                        <tr>
                            <th>Password: </th>
                            <td>
                                <input name="password" type="password" size="15"/>
                              </td>
                          </tr>
                        <tr>
                            <th>&nbsp;</th>
                            <td>
                                <input type="submit" value="Log in"/>
                              </td>
                          </tr>
                      </table>
                  </form>
              </td>
          </tr>
      </table>
  </body>
</html>
