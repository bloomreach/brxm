Hippo Site Toolkit Maven Archetype
------------------------------------

This archetype creates a Maven multi project containing three subprojects:

ecm     - custom Hippo CMS 7 / Hippo Repository 2 project
content - sample content 
site    - sample website


To use, first build the archetype by running the following command from the
archetype-start directory:

  mvn install
  
Then run the following command to create your project:

   mvn archetype:create 
     -DgroupId=org.example 
     -DartifactId=myproject 
     -Dversion=1.01.00
     -DarchetypeGroupId=org.onehippo.ecm.hst
     -DarchetypeArtifactId=site-toolkit-archetype 
     -DarchetypeVersion=2.01.00 

