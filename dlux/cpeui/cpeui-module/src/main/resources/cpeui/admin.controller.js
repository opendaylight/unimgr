define([ 'app/cpeui/cpeui.module' ], function(cpeui) {

  cpeui.register.controller('AdminPageCtrl',
      function($scope, CpeuiSvc, CpeuiDialogs, $mdDialog, $mdMedia) {

        // Tenants

        $scope.selectedTenant = {};

        $scope.updateTenantView = function() {
          CpeuiSvc.getTenantList(function(tenant_list) {
            $scope.tenantArray = tenant_list;
          });
        }

        $scope.AddTenant = function(serviceType) {
          CpeuiSvc.addTenant($scope.tenantId, serviceType, function() {
            $scope.updateView();
          });
        };

        $scope.tenantDialog = new CpeuiDialogs.Dialog('AddTenant', {},
            function(obj) {
              CpeuiSvc.addTenant(obj.id, obj.service_type, function() {
                $scope.updateView();
              });
            });

        $scope.OpenTenantPortal = function(tenant) {
          $scope.currentTenent = tenant;
          window.location = "#cpeui/tenant/" + tenant.name;
        };

        $scope.DeleteTenant = function(tenantID) {
          CpeuiDialogs.confirm(function() {
            CpeuiSvc.deleteTenant(tenantID, function() {
              $scope.updateView();
            });
          });
        };

        // CEs
        $scope.updateCesView = function() {
          CpeuiSvc.getCes(function(ces) {
            $scope.ces = ces;
          });
        };

        $scope.cesDialog = new CpeuiDialogs.Dialog('AddCE', {}, function(obj) {
          CpeuiSvc.addCe(obj.id, obj.name, function() {
            $scope.updateCesView();
          });
        });

        $scope.deleteCe = function(tenantid, ceid) {
          CpeuiDialogs.confirm(function() {
            CpeuiSvc.removeCe(ceid, function() {
              $scope.updateCesView();
            });
          });
        };

        $scope.assignCpeToTenant = function(cpeId) {
          CpeuiDialogs.customConfirm("Are You Sure?",
              "Are you sure you want to override all this CPE's unis tenants?",
              function() {
                for (var i = 0; i < $scope.unis.length; ++i) {
                  if ($scope.unis[i].device == cpeId) {
                    CpeuiSvc.updateUni($scope.unis[i]['uni-id'],
                        $scope.selectedTenant[cpeId]);
                  }
                }
              }, function() {
                $scope.selectedTenant[cpeId] = undefined;
              });
        }

        function updateCpeTenants(unis) {
          // update tenant cpe tenant column
          var hasMultipleTenants = [];
          var device2tenant = {};
          for (var i = 0; i < $scope.unis.length; ++i) {
            var tenant = $scope.unis[i]["tenant-id"];

            if (hasMultipleTenants.indexOf($scope.unis[i].device) != -1) {
              continue;
            }
            if (device2tenant[$scope.unis[i].device] == undefined) {
              if (tenant) {
                device2tenant[$scope.unis[i].device] = tenant;
              } else {
                device2tenant[$scope.unis[i].device] = ""; // none
              }
            } else if (device2tenant[$scope.unis[i].device] != tenant) {
              if ((device2tenant[$scope.unis[i].device] != "") || (tenant)) {
                device2tenant[$scope.unis[i].device] = true; // multiple
                hasMultipleTenants.push($scope.unis[i].device);
              }
            }
          }
          var devices = Object.keys(device2tenant);
          for (var i = 0; i < devices.length; ++i) {
            if (device2tenant[devices[i]] == true) {
              $scope.selectedTenant[devices[i]] = undefined;
            } else {
              $scope.selectedTenant[devices[i]] = device2tenant[devices[i]];
            }
          }
        }

        // UNIs
        $scope.updateUniView = function() {
          CpeuiSvc.getUnis(function(unis) {
                $scope.unis = unis;
                $scope.unis.forEach(function(u) {
                      u.prettyID = u['uni-id'].split(":")[u['uni-id']
                          .split(":").length - 1];
                    });
                updateCpeTenants(unis);
              });
        };

        $scope.linkUniDialog = new CpeuiDialogs.Dialog('LinkUni', {}, function(
            obj) {
          CpeuiSvc.updateUni(obj.id, obj.tenant, function() {
            $scope.updateUniView();
          });
        });

        $scope.deleteUni = function(id) {
          CpeuiDialogs.confirm(function() {
            CpeuiSvc.removeUni(id, function() {
              $scope.updateUniView();
            });
          });
        };

        // General
        $scope.updateView = function() {
          $scope.updateTenantView();
          $scope.updateCesView();
          $scope.updateUniView();
        };

        $scope.updateView();
      });
});
