<#assign hst=JspTaglibs["http://www.hippoecm.org/jsp/hst/core"]>

<#if title??>
  <@hst.headContribution keyHint="hst.seo.document.title">
    <title>${title?xml}</title>
  </@hst.headContribution>
</#if>

<#if metaKeywords??>
  <@hst.headContribution keyHint="hst.seo.document.keywords">
    <meta name="keywords" content="${metaKeywords?xml}"/>
  </@hst.headContribution>
</#if>

<#if metaDescription??>
  <@hst.headContribution keyHint="hst.seo.document.description">
    <meta name="description" content="${metaDescription?xml}"/>
  </@hst.headContribution>
</#if>

<#if dublinCoreSchemaLink??>
  <@hst.headContribution keyHint="hst.seo.schema.dc">
    <link rel="schema.DC" href="http://purl.org/dc/elements/1.1/"/>
  </@hst.headContribution>
</#if>

<#if dublinCoreTermsLink??>
  <@hst.headContribution keyHint="hst.seo.dc.terms">
    <link rel="schema.DCTERMS" href="http://purl.org/dc/terms/"/>
  </@hst.headContribution>
</#if>

<#if dublinCoreCopyrightLink??>
  <@hst.headContribution keyHint="hst.seo.dc.copyright">
    <link rel="DC.rights copyright" href="${dublinCoreCopyrightLink?xml}"/>
  </@hst.headContribution>
</#if>

<#if dublinCoreLanguage??>
  <@hst.headContribution keyHint="hst.seo.dc.language">
    <meta scheme="DCTERMS.RFC3066" name="DC.language" content="${dublinCoreLanguage?xml}"/>
  </@hst.headContribution>
</#if>

<#if dublinCoreTermsCreated??>
  <@hst.headContribution keyHint="hst.seo.dc.terms.created">
    <meta name="DCTERMS.created" content="${dublinCoreTermsCreated?xml}"/>
  </@hst.headContribution>
</#if>

<#if dublinCoreTermsModified??>
  <@hst.headContribution keyHint="hst.seo.dc.terms.modified">
    <meta name="DCTERMS.modified" content="${dublinCoreTermsModified?xml}"/>
  </@hst.headContribution>
</#if>
