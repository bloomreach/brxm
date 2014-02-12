(function () {
    "use strict";
    angular.module('hippo.essentials')
            .controller('freemarkerSyncCtrl', function ($scope, $sce, $log, $rootScope, $http) {

                $scope.init = function () {
                    //var query = {"query":{"query": "//element(*, hst:template)[@hst:script]", "type": "xpath", "page": 0, "pageSize": 0}};
                    var query = {"query":{"query": "//element(*, hst:template)", "type": "xpath", "page": 0, "pageSize": 0}};
                    $http.post($rootScope.REST.jcrQuery, query).success(function (data) {
                        console.log("========================================");
                        console.log(data);
                        $scope.scriptNodes = data.nodes;
                    });
                };
                $scope.init();
            })
})();