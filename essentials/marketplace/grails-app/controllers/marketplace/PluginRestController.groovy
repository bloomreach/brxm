package marketplace

import grails.rest.RestfulController

class PluginRestController extends RestfulController {
    static responseFormats = ['json', 'xml']

    PluginRestController() {
        super(Plugin)
    }
}
