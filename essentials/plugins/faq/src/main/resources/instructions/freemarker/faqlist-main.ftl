<#include "../include/imports.ftl">

<#-- @ftlvariable name="document" type="{{beansPackage}}.FaqList" -->
<#if document??>
  <#if document.FAQ??>
    <div class="has-edit-button">
      <@hst.manageContent hippobean=document/> <#-- edit faq list document -->
      <h1>${document.title?html}</h1>
      <div>
        <@hst.html hippohtml=document.description/>
      </div>
      <#list document.faqItems as faq>
        <div class="has-edit-button">
          <h3><a href="<@hst.link hippobean=faq />">${faq.question?html}</a></h3>
          <@hst.html hippohtml=faq.answer/>
          <#-- edit or create faq item -->
          <@hst.manageContent hippobean=faq documentTemplateQuery="new-faq-item" rootPath="faq" defaultPath="${document.name}-items" folderTemplateQuery="new-faq-item-folder" />
        </div>
      </#list>
      <#-- create faq item -->
      <div class="has-new-content-button">
        <@hst.manageContent documentTemplateQuery="new-faq-item" rootPath="faq" defaultPath="${document.name}-items" folderTemplateQuery="new-faq-item-folder" />
      </div>
    </div>
  <#else>
  <div class="alert alert-danger">The selected document should be of type FAQ list.</div>
  </#if>
<#elseif editMode>
  <div class="has-edit-button">
    <img src="<@hst.link path="/images/essentials/catalog-component-icons/faq.png" />"> Click to edit FAQ, or select or create a document.
    <#-- add faq list document -->
    <@hst.manageContent documentTemplateQuery="new-faq-list" parameterName="document" rootPath="faq"/>
  </div>
</#if>