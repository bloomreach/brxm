//############################################
// PLUGINS CONTROLLER LOADER
//############################################
var _PROVIDER_QUEUE = 0;
app.controller('pluginLoaderCtrl', function ($scope, $sce, $log, $rootScope, $http, MyHttpInterceptor) {

    $scope.render = function ($scope, $route, $routeParams) {

        // Update the rendering of the page.
        $scope.render = function () {
            var renderAction = $route.current.action;
            $log.info(renderAction);
/*
            var renderPath = renderAction.split(".");
            var username = ($routeParams.username || "");

            // Reset the booleans used to set the class
            // for the navigation.
            var isHome = (renderPath[ 0 ] == "home");
            var isFriends = (renderPath[ 0 ] == "friends");
            var isContact = (renderPath[ 0 ] == "contact");

            // Store the values in the model.
            $scope.renderAction = renderAction;
            $scope.renderPath = renderPath;
            $scope.username = username;
            $scope.isHome = isHome;
            $scope.isFriends = isFriends;
            $scope.isContact = isContact;*/

        };

        $rootScope.$on("$routeChangeSuccess", function ($currentRoute, $previousRoute) {
                    $scope.render();
                }
        );
    };

   /* var queue = angular.module('Essentials')._invokeQueue;
    for (var i = _PROVIDER_QUEUE; i < queue.length; i++) {
        var call = queue[i];
        var provider = _PROVIDERS[call[0]];
        if (provider) {
            provider[call[1]].apply(provider, call[2]);
        }
    }*/
});


app.controller('toolCtrl', function ($scope, $sce, $log, $rootScope, $http, MyHttpInterceptor) {
    $scope.resultMessages = {"@page": "1", "@totalSize": "0", "message": [
        {"value": "Not implemented yet"},
        {"value": "Not implemented yet"}
    ]};
    $scope.runBeanWriter = function () {
        $http({
            method: 'GET',
            url: $rootScope.REST.beanwriter
        }).success(function (data) {
                    $scope.resultMessages = data;
                });

    };
});
app.controller('pluginCtrl', function ($scope, $location, $sce, $log, $rootScope, $http, MyHttpInterceptor) {
    $scope.tabs = [
        {name: "Installed Plugins", link: "/plugins"},
        {name: "Find additional", link: "/find-plugins"}
    ];
    $scope.isPageSelected = function (path) {
        return $location.path() == path;
    };
    //plugin list
    $scope.init = function () {
        $http({
            method: 'GET',
            url: $rootScope.REST.plugins
        }).success(function (data) {
                    $scope.plugins = data;
                });

    };
    $scope.init();

});

/*
 //############################################
 // MAIN CONTROLLER
 //############################################
 */
// TODO move wizard data to won file
app.controller('mainCtrl', function ($scope, $sce, $log, $rootScope, $http, MyHttpInterceptor) {

    $scope.installSampleData = false;
    $scope.stepVisible = [true, false];
    $scope.resultMessages = null;
    $scope.selectedDescription = "Please make a selection";
    $scope.packs = null;

    $scope.init = function () {
        $http({
            method: 'GET',
            url: $rootScope.REST.powerpacks
        }).success(function (data) {
                    $scope.packs = data;
                });

    };

    // TODO fix HTML rendering
    //$scope.trustedContent = $sce.trustAsHtml($scope.resultMessages.message.value);
    $scope.selectChange = function () {

        for (var i = 0; i < $scope.packs.powerpacks.length; i++) {
            var powerpack = $scope.packs.powerpacks[i];
            if (powerpack.value === $scope.selectedItem) {
                $scope.selectedDescription = powerpack.name;
            }
        }
    };
    $scope.onWizardButton = function (index, step) {
        if (index == 0) {
            $scope.stepVisible[0] = false;
            $scope.stepVisible[1] = true;
            $scope.packs.steps[0].buttonText = "Previous";
        }
        else {
            $scope.stepVisible[0] = true;
            $scope.stepVisible[1] = false;
            $scope.packs.steps[0].buttonText = "Next";


        }

        // execute installation:
        $http({
            method: 'GET',
            url: $rootScope.REST.powerpacks_install + $scope.selectedItem + "/" + $scope.installSampleData
        }).success(function (data) {
                    $scope.resultMessages = data;
                    $scope.stepVisible = [false, true];
                });
    };

    $scope.init();

});

/*
 //############################################
 // MENU CONTROLLER
 //############################################

 */
app.controller('mainMenuCtrl', function ($scope, $log, $location, $rootScope, $http, MyHttpInterceptor) {


    $scope.menu = [
        {name: "Plugins", link: "#/plugins"},
        {name: "Tools", link: "#/tools"}
    ];

    $scope.isPageSelected = function (path) {
        var myPath = $location.path();
        if (myPath == '/find-plugins') {
            myPath = '/plugins';
        }
        return  '#' + myPath == path;
    };
    $scope.onMenuClick = function (menuItem) {
        $log.info(menuItem);
    };


});