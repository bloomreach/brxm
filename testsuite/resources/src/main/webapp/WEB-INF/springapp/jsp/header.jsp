<%--
  Copyright 2013-2013 Hippo B.V. (http://www.onehippo.com)

  Licensed under the Apache License, Version 2.0 (the  "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS"
  BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License. --%>

<div id="hd">
  <div id="topnav" class="yui-gc">
    <div class="yui-u first"></div>
    <div class="yui-u">
    </div>
  </div>
  <div id="nav">
    <ul class="menu">
      <hst:link var="flag" path="/images/icons/flag-16_${pageContext.request.locale.language}.png"/>
      <li>
          <img src="${flag}"/> ${pageContext.request.locale.displayCountry}
      </li>
      <hst:link var="home" siteMapItemRefId="homeId"/>
      <li>
          <a href="${home}">Home</a>
      </li>
      <hst:link var="about" siteMapItemRefId="aboutId"/>
      <li>
          <a href="${about}">About Us</a>
      </li>
    </ul>
  </div>
</div>
