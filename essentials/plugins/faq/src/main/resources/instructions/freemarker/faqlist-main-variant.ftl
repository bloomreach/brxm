<#include "../../include/imports.ftl">

<#-- @ftlvariable name="document" type="{{beansPackage}}.FaqList" -->
<#if document??>
  <#if document.FAQ??>
  <div class="has-edit-button">
    <@hst.manageContent templateQuery="new-faq-item" componentParameter="document" document=document rootPath="faq"/>
    <h1>${document.title?html}</h1>
    <@hst.html hippohtml=document.description/>
    <div class="panel-group" id="faqitems" role="tablist" aria-multiselectable="true">
      <#list document.faqItems as faq>
        <div class="panel panel-default">
          <div class="panel-heading" role="tab" id="heading${faq_index}">
            <h4 class="panel-title">
              <a data-toggle="collapse" data-parent="#faqitems" href="#faq_${faq_index}" aria-expanded="false" aria-controls="collapse${faq_index}">
              ${faq.question?html}
              </a>
            </h4>
          </div>
          <div id="faq_${faq_index}" class="panel-collapse collapse" role="tabpanel" aria-labelledby="heading${faq_index}">
            <div class="panel-body">
              <@hst.html hippohtml=faq.answer/>
            </div>
          </div>
        </div>
      </#list>
    </div>
  </div>
    <@hst.headContribution category="htmlBodyEnd">
    <script type="text/javascript" src="<@hst.webfile path="/js/jquery-2.1.0.min.js"/>"></script>
    </@hst.headContribution>
    <@hst.headContribution category="htmlBodyEnd">
    <script type="text/javascript" src="<@hst.webfile path="/js/bootstrap.min.js"/>"></script>
    </@hst.headContribution>
  <#else>
  <div class="alert alert-danger">The selected document should be of type FAQ list.</div>
  </#if>
<#-- @ftlvariable name="editMode" type="java.lang.Boolean"-->
<#elseif editMode>
<div>
  <img src="<@hst.link path="/images/essentials/catalog-component-icons/faq.png" />"> Click to edit FAQ
  <@hst.manageContent templateQuery="new-faq-item" componentParameter="document" document=document rootPath="faq"/>
</div>
</#if>