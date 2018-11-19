Build and install the project:
```
mvn clean install
```

Use the archetype with this command:

```
mvn org.apache.maven.plugins:maven-archetype-plugin:2.4:generate \
-DarchetypeRepository=https://maven.onehippo.com/maven2 \
-DarchetypeGroupId=org.onehippo.cms7 \
-DarchetypeArtifactId=hippo-essentials-plugin-archetype \
-DarchetypeVersion=[target.essentials.version]
```

Build and install the result of the archetype (the myessentialsplugin):
```
mvn clean install
```

Add the Essentials plugin as dependency to a CMS project. Add it to the essentials/pom.xml:
```
<dependency>
  <groupId>org.example</groupId>
  <artifactId>myessentialsplugin</artifactId>
  <version>0.1.0-SNAPSHOT</version>
</dependency>
```

After rebuilding and restarting the CMS project the new Essentials plugin appears in the Library list. 
