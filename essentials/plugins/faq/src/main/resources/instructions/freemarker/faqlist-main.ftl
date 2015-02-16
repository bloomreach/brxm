<#include "../include/imports.ftl">

<#-- @ftlvariable name="document" type="{{beansPackage}}.FaqListDocument" -->
<@hst.defineObjects/>
<#if document??>
<div class="has-edit-button">
<@hst.cmseditlink hippobean=document/>
<h1>${document.title}</h1>
<div>
    <@hst.html hippohtml=document.description/>
</div>
    <#list document.faqDocuments as faq>
    <div>
        <h3><a href="<@hst.link hippobean=faq />">${faq.question}</a></h3>
        <@hst.html hippohtml=faq.answer/>
    </div>
    </#list>

</div>
<#elseif editMode>
  <img src="<@hst.link path="/images/essentials/catalog-component-icons/faq.png" />"> Click to edit FAQ
</#if>