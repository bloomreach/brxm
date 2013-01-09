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
  <body onload="return document.getElementById('loginForm').submit();">
    <form id="loginForm" method="POST" action="${j_security_check}">
      <input type="hidden" name="j_username" value="${j_username?html}" /> 
      <input type="hidden" name="j_password" value="${j_password?html}" />
      <noscript>
        <input type="submit" value="Login now" />
      </noscript>
    </form>
  </body>
</html>