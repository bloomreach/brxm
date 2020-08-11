===================================================
  Hippo Plugin Content Tagging
===================================================

About
==========
The content tagging plugin contains multiple plug-ins to work with tags. 
It contains:
- A tagcloud for browsing your documents based on tags
- A plug-in for adding tags to documents
- A plug-in based system for suggesting tags (TagSuggestor)
- A plug-in for suggesting tags for the current document

You can use the plug-in for adding tags and the tagcloud seperate and without the others.
The plug-in for suggesting tags of the current document has a dependy on the TagSuggestor.


How to use
==========
- Add an dependency to your quickstart/war/pom.xml by adding the following 
  to your <dependecies> section:

<!-- tagging addon -->
<dependency>
   <groupId>org.hippo.cms7</groupId>
   <artifactId>hippo-plugin-content-tagging</artifactId>
   <version>6.0.0</version>
   <type>jar</type>
</dependency>

Next start your application (quickstart/war).

=== Activating the tagcloud ===
-  Use the console to navigate to "/hippo:configuration/hippo:frontend/cms/cms-browser/browserPlugin"
-  Now add "extension.tagcloud" to the wicket.extensions property
-  Also add a new property "extension.tagcloud" with value "service.browse.tagcloud" (String)
-  Be sure to save your changes and you are done.

=== Content ===
The content type you what to tag should have the mixin hippostd:taggable. You do not have to 
define a property hippostd:tags in the prototype as the plugin will try to create it by itself,
but it won't do harm either.

By default it will try to add the template plug-ins to the default content article. You can copy the
nodes to the same location of other content types to enable tagging there.

Example:
[defaultcontent:taggablenews] > hippostd:taggable, hippo:document, hippostd:publishable, hippostd:publishableSummary
- defaultcontent:title (string)
- defaultcontent:date (date)
- defaultcontent:introduction (string)
+ defaultcontent:body (hippostd:html)
+ defaultcontent:internallink (hippo:facetselect)

Background
==========
=== Tags (Input) plug-in ===
This is a simple Wicket plug-in that sets a value on a document.

=== Tags suggestion plug-in ===
This is a simple Wicket plug-in that draw's the results of the TagSuggestor in a sorted tagcloud.
Hence it shares some code with the TagCloud plug-in.

=== TagSuggestor ===
The main purpose of the TagSuggestor is to act as a central point of communication. All outside
resources interface with the TagSuggestor. The TagSuggestor than relay's the request to all the 
TagProviders. All the TagProviders return to the TagSuggestor who in turn returns to the calling
resource. The result is that off all TagProviders combined.  

TagProviders all extend from the AbstractTagsProvider and thus implement the ITagsProvider interface.
Every TagProvider receives the document from the TagSuggestor for inspection and can do it's own magic
to search for tags. All TagProviders must return their suggestion in a TagCollection.

=== Tagcloud ===
The tagcloud plug-in consists of:
- TagCloudPlugin class. This is the plug-in and the Wicket component that draws the tagcloud
- A DocumentListingPlugin and DocumentsProvider. These are necessary to lookup the the handle's
  from the documents returned by the Facetsearch.  


TODO
==========
- expand configuration options. At the moment you can configure the score given to the individual
  tags, but you could also add configuration options for the collection as a whole. The code for 
  that is already there. Just need to add the property in the console and read it in the constructor.
  Also the names under with the suggestor and the providers register could be made configurable in
  the console. This would make it easy to use multiple suggestors simultaneously with different 
  configurations.
- make the TagCloud pageable so it will be possible to reach less popular tags
- make adding tag (input & suggesting) plug-ins to content available in GUI (template editor)


Bugs
==========
Please report all bugs to JIRA https://issues.onehippo.com/browse/HIPPLUG

