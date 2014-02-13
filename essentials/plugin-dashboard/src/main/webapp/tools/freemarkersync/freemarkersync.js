(function () {
    "use strict";
    angular.module('hippo.essentials')
            .controller('freemarkerSyncCtrl', function ($scope, $sce, $log, $rootScope, $http) {
                $scope.endpoint = $rootScope.REST.dynamic + 'freemarkersync/';
                $scope.saveToRepository = function () {
                    angular.forEach($scope.scriptNodes, function (value) {
                        console.log(value);
                    });
                };
                $scope.saveToFile = function () {
                    var map = Essentials.mapBuilder();
                    var one = false;
                    angular.forEach($scope.scriptNodes, function (value) {
                        map.put(value.displayValue, value.value);
                    });

                    $http.post($scope.endpoint + "file", map).success(function (data) {
                        console.log(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
                        console.log(map);
                        console.log(data);
                        console.log("<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<");
                    });
                };
                $scope.init = function () {
                    var query = Essentials.queryBuilder("//hst:hst/hst:configurations//element(*, hst:template)");
                    $http.post($rootScope.REST.jcrQuery, query).success(function (data) {
                        $scope.scriptNodes = [];
                        angular.forEach(data.nodes, function (value) {
                            var myValue = value.path;
                            var displayValue = myValue.replace("/hst:hst/hst:configurations/", "");
                            $scope.scriptNodes.push({"value": myValue, "displayValue": displayValue, "selected": false});
                        });
                    });

                    // get files and match with above paths:
                    $http.get($scope.endpoint).success(function (data) {
                        console.log("-----------------------");
                        console.log(data);
                    });

                };
                $scope.init();
            })
})();