
How to create the demo project
==============================

1) Generate project by the Maven archetype with parameters:

   groupId: org.onehippo.plugins
   artifactId: polldemo
   version: reflect Hippo version, e.g. 11.1.0-SNAPSHOT for the archetype 4.1.0-SNAPSHOT
   package: org.onehippo.plugins.polldemo
   projectName: Hippo Plugin Poll Demo

   Move the generated project to the /demo directory.

2) Build and run the project and install the Poll feature with Essentials, in the default set up.
   Rebuild and run.


Creating a custom poll document the contains the poll compound.
---------------------------------------------------------------
For the next steps, we rely on the auto-export function to export changes into the project.

3) In the CMS document type editor, create a new document type 'customPollDocument' with
   - a String field with caption=Title and path=title
   - a Poll compound field with caption=Poll and path=poll

   Don't forget to commit changes!

4) From Essentials, use the Beanwriter tool to generate a CustomPollDocument Java class.
   Implement in the bean the getter for the poll compound:

     import org.onehippo.forge.poll.contentbean.compound.Poll;

     public Poll getPoll() {
         return getBean("polldemo:poll");
     }

5) Custom poll component.

   - Copy demo-creation/webfiles/custompoll.component.ftl into bootstrap/webfiles/src/main/resources/site/freemarker/polldemo

     (this file was created from contents of the standard template at
     /hst:hst/hst:configurations/hst:default/hst:templates/poll.component.ftl plus some extra code to show the custom title)

   - Copy demo-creation/components to site/src/main/java/org/onehippo/plugins/polldemo

6) From the console, import:

  _demo_creation/import/polldata-polldemo.xml              into /polldata
  _demo_creation/import/content-custom-example-poll.xml    into /content/documents/polldemo/polls

  _demo_creation/import/custompoll-catalogitem.xml         into /hst:hst/hst:configurations/polldemo/hst:catalog/polldemo-catalog
  _demo_creation/import/custompoll-page.xml                into /hst:hst/hst:configurations/polldemo/hst:pages
  _demo_creation/import/custompoll-sitemapitem.xml         into /hst:hst/hst:configurations/polldemo/hst:sitemap
  _demo_creation/import/custompoll-template.xml            into /hst:hst/hst:configurations/polldemo/hst:templates
  _demo_creation/import/custompoll-workspace-container.xml into /hst:hst/hst:configurations/polldemo/hst:workspace/hst:containers

7) Rebuild and run.
   In the CMS Channel Manager, edit the channel and:

   - drop the standard poll component on the home page and pick the example poll document in the component properties.
   - drop the custom poll component on the custompoll page and pick the example custom poll document.

   Publish the changes!

The auto export should have updated the bootstrap content & configuration.
