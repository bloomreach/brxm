import grails.util.Environment
import grails.util.GrailsUtil
import marketplace.Artifact
import marketplace.ArtifactType
import marketplace.Plugin
import marketplace.Vendor

class BootStrap {

    def init = { servletContext ->
        //Create a sample database for demo
        def hippoVendor = new Vendor(name: "Hippo", website: "http://www.onehippo.com" , introduction: "Hippo sets the standard for how organizations can bring real-time relevance to their audience and is the foundation for personalized communication across all channels: mobile, social and web." )
        hippoVendor.save(flush: true, validate: true, failOnError: true)
        def taxonomyPlugin = new Plugin(name: "Taxonomy plugin", title: "Taxonomy plugin", pluginLink: "http://taxonomy.forge.onehippo.org/index.html", introduction: "Editors can enrich their metadata using the Taxonomy plugin to categorize the content. The CMS administrator can edit the taxonomy itself.")
        taxonomyPlugin.vendor = hippoVendor
        def siteArtifact1 = new Artifact(artifactId: "taxonomy-api", groupId: "org.onehippo", scope: "compile", aversion: null, artifactType: ArtifactType.SITE)
        siteArtifact1.save(flush: true, validate: true, failOnError: true)
        def siteArtifact2 = new Artifact(artifactId: "taxonomy-addon-frontend", groupId: "org.onehippo", scope: "compile", aversion: null, artifactType: ArtifactType.SITE)
        siteArtifact2.save(flush: true, validate: true, failOnError: true)
        def cmsArtifact = new Artifact(artifactId: "taxonomy-addon-repository", groupId: "org.onehippo", scope: "compile", aversion: null, artifactType: ArtifactType.CMS)
        cmsArtifact.save(flush: true, validate: true, failOnError: true)
        taxonomyPlugin.artifacts = [siteArtifact1, siteArtifact2, cmsArtifact]
        taxonomyPlugin.save(flush: true, validate: true, failOnError: true)
    }

    def destroy = {
    }
}
