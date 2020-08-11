<#--
  Copyright 2008-2014 Hippo B.V. (http://www.onehippo.com)

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

<div class="yui-u">

  <h1>${document.title}</h1>

  <p>${document.summary}</p>

  <@hst.html hippohtml=document.html/>

  <p>Example channel property: '${channelInfoExample}'</p>

  <#if "${document.resource!}" != "">
    <@hst.link var="resource" hippobean=document.resource />
    <#if "${resource!}" != "">
      <a href="${resource}">${document.resource.name}</a>
    </#if>
  </#if>

  <div>
    <@hst.include ref="todolist"/>
  </div>

  <div class="bannersHome" id="container-1">
    <@hst.include ref="container-1"/>
  </div>

  <div class="bannersHome" id="container-2">
    <@hst.include ref="container-2"/>
  </div>

  <hr/>

  <p>Dummy Example REST links </p>
  <@hst.link var="imageset" hippobean=image mount="restapi-gallery" />
  <@hst.link var="original" hippobean=image subPath="original" mount="restapi-gallery"/>
  <@hst.link var="thumbnail" hippobean=image subPath="thumbnail" mount="restapi-gallery"/>
  <p>ImageSet : <a target="_blank" href="${imageset}">${imageset}</a></p>
  <p>Thumbnail : <a target="_blank" href="${thumbnail}">${thumbnail}</a></p>
  <p> Original : <a target="_blank" href="${original}">${original}</a></p>

  <br/>
  <br/>

  <div style="color:red">Note: this page is loaded by a repository freemarker template, see [/hst:hst/hst:configurations/democommon/hst:scripts/home.ftl] </div>

  <p style="height: 30em;"> </p>

</div>
  