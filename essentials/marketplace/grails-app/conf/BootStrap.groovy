import grails.util.Environment
import grails.util.GrailsUtil
import marketplace.Artifact
import marketplace.Plugin
import marketplace.Vendor

class BootStrap {

    def init = { servletContext ->
        //Create a sample database for demo
        def hippoVendor = new Vendor(name: "Hippo", website: "http://www.onehippo.com" , introduction: "Hippo sets the standard for how organizations can bring real-time relevance to their audience and is the foundation for personalized communication across all channels: mobile, social and web." )
        hippoVendor.save(flush: true, validate: true, failOnError: true)
        def taxonomyPlugin = new Plugin(name: "Taxonomy plugin", title: "Taxonomy plugin", introduction: "Editors can enrich their metadata using the Taxonomy plugin to categorize the content. The CMS administrator can edit the taxonomy itself.")
        taxonomyPlugin.vendor = hippoVendor
        def artifact = new Artifact(artifactId: "dashboard-taxonomy-plugin", groupId: "org.onehippo.cms7.essentials", scope: "compile", aversion: "1.01.00-SNAPSHOT")
        artifact.save(flush: true, validate: true, failOnError: true)
        taxonomyPlugin.artifact = artifact
        taxonomyPlugin.save(flush: true, validate: true, failOnError: true)
    }

    def destroy = {
    }
}
