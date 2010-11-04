<#--
  Copyright 2008-2009 Hippo

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
  <@hst.html hippohtmlByBeanPath="document.html"/>
  
  <p> </p>
  
  <@hst.link var="resource" hippobeanByBeanPath="document.resource" />
  <#if "${resource!}" != "">
    <a href="${resource}">${document.resource.name}</a>
  </#if>
  
  <div>
    <@hst.include ref="todolist"/>
  </div>

  <div class="bannersHome">
    <@hst.include ref="banners"/>
  </div>
  <div class="bannersHome">
    <@hst.include ref="morebanners"/>
  </div>

  <hr/>
  <p>Dummy Example REST links </p>
  <@hst.link var="imageset" hippobeanByBeanPath="image" mount="restapi-gallery" />
  <@hst.link var="picture" hippobeanByBeanPath="image" subPath="picture" mount="restapi-gallery"/>
  <@hst.link var="thumbnail" hippobeanByBeanPath="image" subPath="thumbnail" mount="restapi-gallery"/>
  <p> ImageSet : <a target="_blank" href="${imageset}">${imageset}</a></p>
  <p> Thumbnail : <a target="_blank" href="${thumbnail}">${thumbnail}</a></p>
  <p> Picture : <a target="_blank" href="${picture}">${picture}</a></p>
  <p style="height: 30em;">  </p>
  
  
  
</div>
