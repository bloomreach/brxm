# Dashboard Document Wizard Plugin

This is the Dashboard Document Wizard plugin for Hippo CMS. Documentation on how to use this
plugin in your Hippo project can be found at:

http://www.onehippo.org/library/concepts/plugins/dashboard-document-wizard/about.html

## Build and Run the Demo Project

1. Create a demo project from the archetype as described at https://www.onehippo.org/trails/getting-started/hippo-essentials-getting-started.html
 
2. Add following dependency to *essentials/pom.xml*
```xml
<dependency>
  <groupId>org.onehippo.cms7</groupId>
  <artifactId>hippo-plugin-dashboard-document-wizard-essentials-demo-feature</artifactId>
  <version>${hippo.plugin.dashboard-document-wizard.version}</version>     
</dependency>
```

3. Build and run locally.
From Essentials Library, select Dashboard Document Wizard Demo. Rebuild and run.
 
The project contains example document types showing all selections variants, in both CMS and site.
