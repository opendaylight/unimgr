define([ 'angularAMD', 'app/routingConfig', 'app/core/core.services',
    'Restangular', 'common/config/env.module',
    'app/cpeui/assets/angular-material.min',
    'app/cpeui/assets/angular-animate.min',
    'app/cpeui/assets/angular-aria.min',
    'app/cpeui/assets/angular-messages.min'], function(ng) {

  var cpeui = angular.module('app.cpeui', [ 'app.core', 'ui.router.state','restangular', 'config', 'ngMaterial', 'ngMessages', 'ngAnimate' ]);

  cpeui.config(function($stateProvider, $compileProvider, $controllerProvider, $provide, NavHelperProvider, $translateProvider, $urlRouterProvider) {

    cpeui.register = cpeui; // for adding services, controllers, directives etc.
                            // to angular module before bootstrap

    cpeui.register = {
      controller : $controllerProvider.register,
      directive : $compileProvider.directive,
      factory : $provide.factory,
      service : $provide.service

    };
    
    $urlRouterProvider.otherwise("/cpeui/admin/");

    NavHelperProvider.addControllerUrl('app/cpeui/cpeui.controller');
    NavHelperProvider.addToMenu('cpe', {
      "link" : "#/cpeui/admin/",
      "active" : "main.cpeui",
      "title" : "CPE Portal",
      "icon" : "icon-bullseye", // Add navigation icon css class here
      "page" : {
        "title" : "Cpe",
        "description" : "Cpe"
      }
    });

    var access = routingConfig.accessLevels;

    $stateProvider.state('main.cpeui', {
      url : 'cpeui',
      access : access.public,
      abstract : true,
      views : {
        'content' : {
          templateUrl : 'src/app/cpeui/cpeui.tpl.html',
          controller : 'CpeuiCtrl'
        },
      }
    });

    $stateProvider.state('main.cpeui.tenant', {
      url : '/tenant/:tenantid/:tenantTabName',
      access : access.public,
      views : {
        'cpeui' : {
          templateUrl : 'src/app/cpeui/tenant.tpl.html',
          controller : 'OpenTenantCtrl'
        }
      }
    });

    $stateProvider.state('main.cpeui.admin', {
      abstract : true,
      views : {
        'cpeui' : {
          templateUrl : 'src/app/cpeui/admin.tpl.html',
          controller : 'AdminPageCtrl'
        }
      }
    });

    $stateProvider.state('main.cpeui.admin.tenants', {
      url : '/admin/{tabName}',
      access : access.public,
      params : {
        tabName : {
          value : "tenants"
        }
      },
      views : {
        'tenants' : {
          templateUrl : 'src/app/cpeui/tenantsTable.tpl.html',
          controller : 'TenantTableCtrl'
        }
      }
    });

  });

  return cpeui;
});
