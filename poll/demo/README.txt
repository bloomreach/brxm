
How to generate a demo project for the Polls feature
----------------------------------------------------

1) Generate a project from the archetype.

2) To demo a custom poll document with title and poll compound, add to essentials/pom.xml:

    <dependency>
      <groupId>org.onehippo.cms7</groupId>
      <artifactId>hippo-plugin-poll-essentials-demo-feature</artifactId>
      <version>${hippo.plugin.poll.version}</version>
    </dependency>
	
3) Build && run locally.

- With Essentials, set up the project with the default settings.
  Note that JSP for templating language is not supported for the Polls Demo feature.

- With Essentials, install Polls feature, then rebuild && run twice (boarding, installing).

- With Essentials, install Polls Demo feature, then rebuild && run.

4) Demo

Visit http://localhost:8080/site/custompoll to see the custom poll directly in the site, or show it in the Channel
Manager preview. The standard poll component is first to be dragged onto a page (e.g. home) to become visible.