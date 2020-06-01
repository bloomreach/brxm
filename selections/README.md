# Selections Plugin

This is the Selections plugin for BloomReach Experience Manager. 
Documentation on how to use this plugin in your project can be found at:

  http://www.onehippo.org/library/concepts/plugins/selections/about.html

## Build and Run the Demo Project

1. Create a demo project from the archetype as described at https://www.onehippo.org/trails/getting-started/hippo-essentials-getting-started.html
 
2. Add following dependency to *essentials/pom.xml*
```xml
<dependency>
  <groupId>org.onehippo.cms7</groupId>
  <artifactId>hippo-plugin-selections-essentials-demo-feature</artifactId>
  <version>${hippo.plugin.selections.version}</version>     
</dependency>
```

3. Build and run locally.
From Essentials Library, select Selections Demo. Rebuild and run.
 
The project contains example document types showing all selections variants, in both CMS and site.