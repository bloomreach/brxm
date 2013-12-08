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
    $scope.message = "Welcome";
    $scope.packs=null;

    $scope.init = function(){
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
app.controller('mainMenuCtrl', function ($scope, $log, $rootScope, $http, MyHttpInterceptor) {

    $scope.menu = [
        {item: "Plugins"},
        {item: "Tools"}
    ];

    $scope.onMenuClick = function (menuItem) {
        $log.info(menuItem);
    };


});