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
  limitations under the License. -->

<#assign hst=JspTaglibs["http://www.hippoecm.org/jsp/hst/core"]>

<@hst.link var="yuiLoaderSrc" path="/javascript/yui/yuiloader/yuiloader-min.js"/>
<@hst.link var="inlineEditingSrc" path="/javascript/inline-editing.js"/>


<@hst.element var="yuiLoader" name="script">
  <@hst.attribute name="id" value="yuiloader" />
  <@hst.attribute name="type" value="text/javascript" />
  <@hst.attribute name="src" value="${yuiLoaderSrc}" />
</@hst.element>
<@hst.headContribution keyHint="yuiLoader" element=yuiLoader category="jsExternal"/>

<@hst.element var="inlineEditing" name="script">
  <@hst.attribute name="id" value="inlineEditing" />
  <@hst.attribute name="type" value="text/javascript" />
  <@hst.attribute name="src" value="${inlineEditingSrc}" />
</@hst.element>
<@hst.headContribution keyHint="inlineEditing" element=inlineEditing  category="jsExternal"/>

<@hst.element var="documentTitle" name="title">
${document.title}
</@hst.element>
<@hst.headContribution keyHint="documentTitle" element=documentTitle />

<#if "${goBackLink!}" != "">
<@hst.link var="goBackLink2" path="${goBackLink}"/>
<@hst.link var="goBackImgLink" path="/images/goback.jpg"/>
<div class="right">
  <a href="${goBackLink2}">
    <img src="${goBackImgLink}" class="noborder" alt="Go back"/>
  </a>
</div>
</#if>

<div class="yui-u">

  <@hst.componentRenderingURL var="componentRenderingURL"/>
  <a href="${componentRenderingURL}" target="_blank">Render only this component</a>
  </p>
  <div id="editable_cont" class="inline-editor-editable-container">
    <h2>${document.title}</h2>
    <@hst.cmseditlink hippobean=document/>
    <p>
        <#if isPreview>
            <span class="editable" id="demosite:summary">${document.summary}</span>
        <#else>
            <@hst.messagesReplace>${document.summary}</@hst.messagesReplace>
        </#if>
    </p>
    <div>
        <#if isPreview>
            <span class="editable inline" id="demosite:body"><@hst.html hippohtml=document.html/></span>
        <#else>
            <@hst.html hippohtml=document.html />
        </#if>
    </div>
  </div>

  <#if document.resource??>
    <@hst.link var="resource" hippobean=document.resource />
    <h2>resource link:</h2>
    <a href="${resource}">${document.resource.name}</a>
    <br/><br/>
  </#if>
  
  <#if document.image??>
    <@hst.link var="documentImageLink" hippobean=document.image.original/>
    <#if "${documentImageLink!}" != "">
      <img src="${documentImageLink}"/>
    </#if>
  </#if>

  <@hst.actionURL var="addURL">
    <@hst.param name="type" value="add"/>
  </@hst.actionURL>
  
  <div>
      <#list comments as comment>
         <div style="border:1px solid black; padding:15px;">
             <b>${comment.title}</b>
             <br/>
             <@hst.html hippohtml=comment.html/>
         </div>
      </#list>
  </div>
  
  <div>
    <form method="post" action="${addURL}">
      <h4>Enter your comment here:</h4>
      <table>
        <tr>
          <th><label for="title">Title:</label></th>
          <td><input type="text" id="title" name="title" value="" /></td>
        </tr>
        <tr>
          <th valign="top"><label for="comment">Comment:</label></th>
          <td><textarea name="comment" id="comment" rows="4" cols="40"></textarea></td>
        </tr>
        <tr>
          <td colspan="2">
            <input type="submit" value="Submit"/>
            <input type="reset" value="Reset"/>
          </td>
         </tr>
      </table>
    </form>
  </div>

</div>

<@hst.resourceURL var="inlineEditingResourceURL" resourceId='/WEB-INF/jsp/components/main/detailpage-ajaxresult.jsp' />
<form id="editorForm" method="post" action="${inlineEditingResourceURL}">
  <div class="yui-skin-sam">
    <input type="hidden" name="nodepath" value="${document.path}"/>
    <input type="hidden" name="customnodepath" value=""/>
    <input type="hidden" name="field" value=""/>
    <input type="hidden" name="workflowAction" value=""/>
    <textarea id="editor" name="editor" class="inline-editor-editor" cols="50" rows="5"></textarea>
    
    <@hst.link var="icons" path="/images/icons"/>
    <span id="editorToolbar" class="inline-editor-toolbar">
      <img src="${icons}/document-save-16.png" id="editorToolbar_save" alt="Save" title="Save"/>
      <img src="${icons}/document-revert-16.png" id="editorToolbar_close" alt="Close without saving" title="Close without saving"/>
      <img src="${icons}/workflow-requestpublish-16.png" id="editorToolbar_requestPublication" alt="Request publication" title="Request publication"/>
      <img src="${icons}/edit-16.png" id="editorToolbar_editInCMS" alt="Edit in CMS" title="Edit in CMS"/>
    </span>
  </div>
</form>

<@hst.headContribution category="jsInline">
<script type="text/javascript" language="javascript">
<![CDATA[<!--]]>
//Instantiate and configure Loader:
var loader = new YAHOO.util.YUILoader({

    // Identify the components you want to load.  Loader will automatically identify
    // any additional dependencies required for the specified components.
    require: ["container", "menu", "button", "editor", "json", "resize"],

    // Configure loader to retrieve the libraries locally
    base: '/site/javascript/yui/',

    // Configure loader to pull in optional dependencies.  For example, animation
    // is an optional dependency for slider.
    loadOptional: true,

    // The function to call when all script/css resources have been loaded
    onSuccess: function() {
        //this is your callback function; you can use
        //this space to call all of your instantiation
        //logic for the components you just loaded.
        
        // Load the edited css for the inline editor.
        // This must be done after the loader has been fully instantiated, so the css loads
        // after the default css (skin.css). That way, the element classes can be overwritten
        // by our own css file.
        var addcss=document.createElement("link")
        addcss.setAttribute("rel", "stylesheet")
        addcss.setAttribute("type", "text/css")
        addcss.setAttribute("href", "/site/css/inline-editing.css")
        document.getElementsByTagName("head")[0].appendChild(addcss);

        // Initialize editor
        initEditor("editable_cont", "editorForm", "editor", "editor2", "editorToolbar", "${cmsApplicationUrl}");
    },

    // Configure the Get utility to timeout after 10 seconds for any given node insert
    timeout: 10000
});

// Load the files using the insert() method. The insert method takes an optional
// configuration object, and in this case we have configured everything in
// the constructor, so we don't need to pass anything to insert().
loader.insert();
<![CDATA[//-->]]>
</script>
</@hst.headContribution>