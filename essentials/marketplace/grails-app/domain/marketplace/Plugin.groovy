package marketplace
/**
 * @see  org.onehippo.cms7.essentials.dashboard.Plugin
 */
class Plugin {
    static hasMany = [screenshots: Screenshot, assets: Asset]
    String name
    String type
//    String icon
    String pluginClass
    String description
    Vendor vendor
    Artifact artifact
//    String vendorLink
//    String documentationLink
//    String pluginLink
//    String issuesLink

    static constraints = {
        name nullable: false
        type nullable: true
//        icon nullable: true
        pluginClass nullable: true
        description nullable: true
        vendor nullable: false
//        vendorLink url: true, nullable: true
//        documentationLink nullable: true
//        pluginLink nullable: true
//        issuesLink nullable: true
        name()
        type()
        vendor()
        description()
        pluginClass()
        vendor()
        artifact()
    }
}
