package marketplace

import grails.rest.RestfulController

class PluginRestController extends RestfulController {
    static responseFormats = ['json', 'xml']

    PluginRestController() {
        super(Plugin)
    }

    def index(Integer max) {
        params.max = Math.min(max ?: 10, 100)
        def wrapper = [:]
        wrapper['items'] = Plugin.list(params)
        respond wrapper
    }
}
