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

        $scope.AddTenant = function() {
          CpeuiSvc.addTenant($scope.tenantId, function() {
              $scope.updateTenantView();
          });
        };

        $scope.tenantDialog = new CpeuiDialogs.Dialog('AddTenant', {},
            function(obj) {
              CpeuiSvc.addTenant(obj.id, function() {
                 $scope.updateTenantView();
              });
            });

        $scope.DeleteTenant = function(tenantID) {
          CpeuiDialogs.confirm(function() {
            CpeuiSvc.deleteTenant(tenantID, function() {
                $scope.updateTenantView();
            });
          });
        };

        // Profiles
        $scope.profiles = [];
        $scope.updateProfilesView = function() {
          CpeuiSvc.getProfiles(function(profiles) {
            $scope.profiles = profiles;
          });
        };

        var profileDialogController = function($scope, $mdDialog) {

            $scope.getDefualtCbs = function(cir) {
                return Math.round(cir * 0.0125);
            }

            $scope.done = function() {
                if ($scope.obj.default_cbs) {
                    $scope.obj.cbs = $scope.getDefualtCbs($scope.obj.cir);
                }
                if ($scope.projectForm.$valid) {
                  $scope.callback($scope.obj);
                  $mdDialog.hide();
                }
              };
          };

        $scope.addProfile = new CpeuiDialogs.Dialog('AddProfile', {}, function(obj) {
            CpeuiSvc.addProfile(obj['bw-profile'], obj.cir, obj.cbs, function() {
              $scope.updateProfilesView();
            });
        }, profileDialogController);

        $scope.editProfile = function(profileName, cbs, cir) {
            new CpeuiDialogs.Dialog('AddProfile', {}, function(obj) {
                CpeuiSvc.editProfile(obj['bw-profile'], obj.cir, obj.cbs, function() {
                  $scope.updateProfilesView();
                });
            }, profileDialogController).show(null,{edit:true, profileName:profileName, cbs:cbs, cir:cir});
        };

        $scope.deleteProfile = function(profileName) {
          CpeuiDialogs.confirm(function() {
            CpeuiSvc.deleteProfile(profileName, function() {
              $scope.updateProfilesView();
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

        $scope.assignNetworkToTenant = function(svc) {
          CpeuiDialogs.customConfirm("Are You Sure?",
              "Are you sure you want to assign service "+ svc['svc-id'] +" to tenant " + $scope.selectedTenant[svc['svc-id']] +"?",
              function() {
                CpeuiSvc.addTenantToService(svc['svc-id'], $scope.selectedTenant[svc['svc-id']], function(){
                  svc['tenant-id'] = $scope.selectedTenant[svc['svc-id']];
                },function(){
                  $scope.selectedTenant[svc['svc-id']] = undefined;
                });
              }, function() {
                $scope.selectedTenant[svc['svc-id']] = undefined;
              });
        };

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

        $scope.addCEName = function(ce){
          ce._naming = true;
          var input = $('#INPUT_' +ce['dev-id']);          
          // hack to focus input after show is complete
          setTimeout(function(){input.focus();},20);          
          input.parent().on('blur',function(){
            setTimeout(function(){
              ce._naming = false;
              delete ce._new_name;
            },20);
          });
          
          input.bind("keyup", function (eventSubmit) {
            if(eventSubmit.which === 13) {            
              $('#OK_' +ce['dev-id']).click();
            } else if(eventSubmit.which === 27) {              
              input.parent().blur();
            }
          });
        }
        
        $scope.renameCE = function(ce){
          CpeuiSvc.addCeName(ce, ce._new_name, function(){
            ce['device-name'] = ce._new_name;
            });
          ce._naming = false;
        }
        
        $scope.services = [];
        $scope.networkNames = {};
        
        $scope.updateNetworksView = function() {
          CpeuiSvc.getAllServices(function(services) {
            $scope.services = services;
          });
          CpeuiSvc.getNetworkNames(function(networks){        
            networks.forEach(function(net){
              $scope.networkNames[net.uuid] = net.name;
            });
          });
        };
        
        // General
        $scope.updateView = function() {
          if ($scope.isTabSet('admin',4)){
            $scope.updateNetworksView();
          }
          $scope.updateTenantView();
          $scope.updateCesView();
          $scope.updateUniView();
          $scope.updateProfilesView()
        };

        $scope.updateView();
      });
});
