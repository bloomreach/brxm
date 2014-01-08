/*app.directive("modalShow", function () {
  return {
    restrict: "A",
    scope: {
      modalVisible: "="
    },
    link: function (scope, element, attrs) {

      //Hide or show the modal
      scope.showModal = function (visible) {
        if (visible)
        {
          element.modal("show");
        }
        else
        {
          element.modal("hide");
        }
      }

      //Check to see if the modal-visible attribute exists
      if (!attrs.modalVisible)
      {

        //The attribute isn't defined, show the modal by default
        scope.showModal(true);

      }
      else
      {

        //Watch for changes to the modal-visible attribute
        scope.$watch("modalVisible", function (newValue, oldValue) {
          scope.showModal(newValue);
        });

        //Update the visible value when the dialog is closed through UI actions (Ok, cancel, etc.)
        element.bind("hide.bs.modal", function () {
          scope.modalVisible = false;
          if (!scope.$$phase && !scope.$root.$$phase)
            scope.$apply();
        });

      }

    }
  };

})*/;


app.controller('galleryPluginCtrl', function ($scope, $sce, $log, $rootScope, $http, MyHttpInterceptor, $modal) {

  $scope.message = "Gallery plugin";
  $scope.selectedTab = 1;

  $scope.invalidated = false;



  $scope.updateSelectedImageSetsAndVariants = function() {

    $scope.imageSetVariants =
      [$scope.imageProcessor.variants[2]];


    $scope.variantImageSets =
      [$scope.imageSets[2]];



  };




/*
  var fruitsApp = angular.module('fruitsApp', [])

  fruitsApp.factory('fruitsFactory', function($http) {
    return {
      getFruitsAsync: function(callback) {
        $http.get('fruits.json').success(callback);
      }
    };
  });
*/




  // TODO populate image processors from rest service
  $http.get('plugins/galleryPlugin/testimageprocessor.json').success(function(data) {
    $scope.imageProcessor = data;


  });

  // TODO populate image sets from rest service
    $http.get('plugins/galleryPlugin/testimagesets.json').success(function(data) {
        $scope.imageSets = data;

      //$scope.updateSelectedImageSetsAndVariants();
    });






  // TODO populate image sets from rest service
  $http.get('plugins/galleryPlugin/cmslanguages.json').success(function(data) {
    $scope.cmsLanguages = data;
  });


  $scope.variantTranslationsModels = {};


  $scope.init = function () {
        $log.info(" **** gallery plugin called ***");
    };

    $scope.displayImageSet = function(imageSet) {
      $scope.currentImageSet = imageSet;
      $log.info("selected" + $scope.currentImageSet.name);
    }

    $scope.addImageSet = function() {
      $log.info("Add image set");
    }

    $scope.deleteCurrentImageSet = function() {
      $log.info("Add image set");
    }

  // TODO change this
  $scope.projectGalleryNamespace = "projectgallery";


  $scope.newVariantTemplate =
  {
    "id": "",
    "namespace": "",
    "name": "",
    "translations": [
      {
        "locale": "",
        "message": ""
      }
    ],
    "width": 0,
    "height": 0,
    "properties": [],
    "imageSets": []
  };

  $scope.newImageSetTemplate =
  {
    "id": "",
    "namespace": "",
    "name": "",
    "translations": [
      {
        "locale": "",
        "message": ""
      }
    ],
    "variants": [
    {
      "id": "82759c6b-54a1-4842-8e75-772dfa4d72ec",
      "name" : "thumbnail",
      "translations": [
        {
          "locale": "",
          "message": "Thumbnail"
        },
        {
          "locale": "en",
          "message": "Thumbnail"
        },
        {
          "locale": "nl",
          "message": "Thumbnail"
        }
      ],
      "width": 60,
      "height": 60,
      "upscaling": false
    },
    {
      "id": "aeed4080-c60f-4a3c-ab0a-ad871004cbc3",
      "name" : "original",
      "translations": [
        {
          "locale": "",
          "message": "Original"
        },
        {
          "locale": "en",
          "message": "Original"
        },
        {
          "locale": "nl",
          "message": "Origineel"
        }
      ],
      "width": 0,
      "height": 0,
      "upscaling": false
    }
  ]
  };

  /**
   * A
   * @param variant
   * @param language
   * @param message
   */
  $scope.addVariant = function() {
    var newVariant = angular.copy($scope.newVariantTemplate);
    newVariant.namespace = $scope.projectGalleryNamespace;
    newVariant.id = $scope.generateUUID();
    $scope.imageProcessor.variants.push(newVariant);
  }

  $scope.removeVariant = function(variant) {
    var index = $scope.imageProcessor.variants.indexOf(translation)
    $scope.imageProcessor.variant.splice(index,1);
  }



  $scope.addVariantTranslation = function(variant, language, message) {
    variant.translations.push({"locale": language.locale,"message": message});
  }

  $scope.removeVariantTranslation = function(variant, translation) {
    var index = variant.translations.indexOf(translation)
    variant.translations.splice(index,1);
  }



  $scope.addImageSet = function() {
    var newImageSet = angular.copy($scope.newImageSetTemplate);
    newImageSet.namespace = $scope.projectGalleryNamespace;
    newImageSet.id = $scope.generateUUID();
    $scope.imageSets.push(newImageSet);
  }

  $scope.removeImageSet = function(imageSet) {
    var index = $scope.imageSets.indexOf(imageSet)
    $scope.imageSets.splice(index,1);
  }



  $scope.addImageSetTranslation = function(imageSet, language, message) {
    imageSet.translations.push({"locale": language.locale,"message": message});
  }

  $scope.removeImageSetTranslation = function(imageSet, translation) {
    var index = imageSet.translations.indexOf(translation)
    imageSet.translations.splice(index,1);
  }



  $scope.calculateIdForVariant = function(variant) {
    return variant.id;
  }

  $scope.calculateIdForImageSet = function(imageSet) {
    return imageSet.id;
  }

  $scope.generateUUID = function() {
    var d = new Date().getTime();
    var uuid = 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function(c) {
      var r = (d + Math.random()*16)%16 | 0;
      d = Math.floor(d/16);
      return (c=='x' ? r : (r&0x7|0x8)).toString(16);
    });
    return uuid;
  };


  $scope.updateImageSetsForVariant = function (variant) {
    for (var i = 0, length = $scope.imageSets.length; i < length; ++i) {
      var imageSet = $scope.imageSets[i];
      $scope.updateImageSetForVariant(imageSet, variant);
    }
  };

  $scope.updateImageSetForVariant = function (imageSet, variant) {
    $log.info('Update image set ' + imageSet.name + ' for: ' + variant.name);
  };

  $scope.updateVariantsForImageSet = function (imageSet) {
    for (var i = 0, length = $scope.imageProcessor.variants.length; i < length; ++i) {
      var variant = $scope.imageProcessor.variants[i];
      $scope.updateVariantForImageSet(variant, imageSet);
    }
  };

  $scope.updateVariantForImageSet = function (variant, imageSet) {
    $log.info('Update variant ' + variant.name + ' for: ' + imageSet.name);
  };


  $scope.saveVariantsAndImageSets = function() {
    if(!$scope.validateVariants() || !$scope.validateImageSets()) {
      $scope.invalidated = true;
      $log.info('Unable to save image sets');
      return
    }
    $scope.invalidated = false;
    $log.info('Save image sets');
  }

  $scope.validateVariants = function() {
    for (var i = 0, length = $scope.imageProcessor.variants.length; i < length; ++i) {
      var variant = $scope.imageProcessor.variants[i];
      if(variant.name === "") {
        return false;
      }
    }
    return true;
  }

  $scope.validateImageSets = function() {
    for (var i = 0, length = $scope.imageSets.length; i < length; ++i) {
      var imageSet = $scope.imageSets[i];
      if(imageSet.name === "") {
        return false;
      }
    }
    return true;
  }

  $scope.openDeleteImageSetConfirmation = function () {

    var modalInstance = $modal.open({
      templateUrl: 'deleteImageSetConfirmation.html',
      controller: ModalConfirmationCtrl,
      resolve: {
        currentImageSet: function () {
          return $scope.currentImageSet;
        }
      }
    });

    modalInstance.result.then(function (currentImageSet) {
      $log.info('Delete: ' + currentImageSet.name);

    }, function () {
      $log.info('Modal dismissed at: ' + new Date());
    });
  };















  $scope.imageSetVariants2 = [
    {
      "name": "projectgallery:large",
      "translations": [
        {
          "locale": "",
          "message": "Large"
        },
        {
          "locale": "en",
          "message": "Large"
        },
        {
          "locale": "nl",
          "message": "Groot"
        }
      ],
      "width": 400,
      "height": 400,
      "properties": [
        {
          "name": "upscaling",
          "value": "false"
        }
      ]
    }
  ];
















  $scope.upscalingValues = [
    {
      value: false,
      description: "default (off)",
      default: true
    },
    {
      value: true,
      description: "on"
    }
  ];
  $scope.defaultUpscalingValue = $scope.upscalingValues[0];
  $scope.upscalingValue = $scope.defaultUpscalingValue;




  $scope.optimizeValues = [
    {
      value: "quality",
      description: "default (quality)",
      default: true
    },
    {
      value: "speed",
      description: "speed"
    },
    {
      value: "speed.and.quality",
      description: "speed and quality"
    },
    {
      value: "best.quality",
      description: "best quality"
    },
    {
      value: "auto",
      description: "auto"
    }
  ];
  $scope.defaultOptimizeValue = $scope.optimizeValues[0];
  $scope.optimizeValue = $scope.defaultOptimizeValue;

  $scope.compressionValues = [
    {
      value: 1,
      description: "default (uncompressed)",
      default: true
    },
    {
      value: 0.95,
      description: "best"
    },
    {
      value: 0.9,
      description: "very good"
    },
    {
      value: 0.8,
      description: "good"
    },
    {
      value: 0.7,
      description: "medium"
    },
    {
      value: 0.5,
      description: "low"
    }
  ];
  $scope.defaultCompressionValue = $scope.compressionValues[0];
  $scope.compressionValue = $scope.defaultCompressionValue;

    $scope.init();

});

var ModalConfirmationCtrl = function ($scope, $modalInstance, currentImageSet) {

  $scope.currentImageSet = currentImageSet;

  $scope.ok = function () {
    $modalInstance.close($scope.currentImageSet);
  };

  $scope.cancel = function () {
    $modalInstance.dismiss('cancel');
  };
};


app.filter('hideHippoGalleryVariantsFilter', function () {
  return function (variants) {
    var shownVariants = [];
    angular.forEach(variants, function (variant) {
      console.log('filter vairant: ' + variant.name);

      if (variant.namespace !== 'hippogallery') shownVariants.push(variant);
    });
    return shownVariants;
  };
});