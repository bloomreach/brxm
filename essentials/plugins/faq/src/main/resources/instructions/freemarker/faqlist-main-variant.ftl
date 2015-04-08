<#include "../../include/imports.ftl">

<#-- @ftlvariable name="document" type="{{beansPackage}}.FaqDocument" -->
<@hst.defineObjects/>
<#if document??>
    <div class="has-edit-button">
        <@hst.cmseditlink hippobean=document/>
        <h1>${document.title?html}</h1>
        <@hst.html hippohtml=document.description/>
        <div class="panel-group" id="faqitems" role="tablist" aria-multiselectable="true">
            <#list document.faqDocuments as faq>
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
<#-- @ftlvariable id="editMode" type="java.lang.Boolean"-->
<#elseif editMode>
<img src="<@hst.link path="/images/essentials/catalog-component-icons/faq.png" />"> Click to edit FAQ
</#if>