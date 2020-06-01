<#--
  Copyright 2010-2014 Hippo B.V. (http://www.onehippo.com)

  Licensed under the Apache License, Version 2.0 (the  "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS"
  BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License. -->

<#assign hst=JspTaglibs["http://www.hippoecm.org/jsp/hst/core"]>

<@hst.defineObjects/>
<#if hstRequest.requestContext.cmsRequest>
    <ul class="hst-container">
        <#list hstResponseChildContentNames as childContentName>
            <li class="hst-container-item">
                <@hst.include ref="${childContentName}"/>
            </li>
        </#list>
    </ul>
<#else>
    <ul>
        <#list hstResponseChildContentNames as childContentName>
            <@hst.include ref="${childContentName}" var="output"/>
            <#if output?has_content >
                <li>
                ${output}
                </li>
            </#if>
        </#list>
    </ul>
</#if>