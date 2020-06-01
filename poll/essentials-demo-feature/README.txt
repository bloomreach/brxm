Hippo Essentials Demo Feature
=============================
This module contains a Hippo Essentials Demo Feature. It adds a custom poll document: a document in the project
namespace with a title and the poll compound. It also adds a rendering template and some HST configuration for it.

To use this feature, add a dependency to this artifact to the Essentials module of a Hippo project. After a (re)build it
will show up in the Feature Library.

    <dependency>
      <groupId>org.onehippo.cms7</groupId>
      <artifactId>hippo-plugin-poll-essentials-demo-feature</artifactId>
      <version>${hippo.plugin.poll.version}</version>
    </dependency>
