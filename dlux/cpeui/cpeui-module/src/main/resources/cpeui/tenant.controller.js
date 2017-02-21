define([ 'app/cpeui/cpeui.module' ], function(cpeui) {
  cpeui.register.controller('OpenTenantCtrl', function($scope, CpeuiSvc, CpeuiDialogs, CpeUiUtils, $stateParams) {

    $scope.curTenant = $stateParams.tenantid;
    $scope.unisTables = {};
    $scope.unis = [];
    $scope.ces = [];
    $scope.ipvcs = [];
    $scope.evcs = [];
    $scope.subnets = {};
    $scope.profiles =[];
    $scope.cesDisplayNames = {};
    $scope.unisMap = {};
    $scope.networkNames = {};
    $scope.expandFlags = {
        ipuni:{},
        tuni:{},
        L2:{},L3:{}
    };

    var tabIndexs = {
        "inventory" : 0,
        "L2" : 1,
        "L3" : 2
      }
    if ($stateParams.tenantTabName in tabIndexs) {
      $scope.tab.tenantData = tabIndexs[$stateParams.tenantTabName];
    } else {
        $scope.tab.tenantData = tabIndexs.inventory;
    }

    CpeuiSvc.getTenantList(function(tenant_list) {
        if (tenant_list.filter(t => t.name == $scope.curTenant).length == 0) {
            window.location = "#/cpeui/admin/tenants";
        }
    });

    function init(){
      $scope.updateUnis(function(unis){
        CpeuiSvc.getCes(function(ces) {
          $scope.ces = ces.filter(function(item) {

            var filteredUnis = unis.filterByField('device', item["dev-id"]);
            filteredUnis = filteredUnis.filterByField('prettyID', 'br-int', true);
            filteredUnis = filteredUnis.filter(function(i){return !(i.prettyID && i.prettyID.startsWith('tun'));});

            return (filteredUnis.length);
          });
          ces.forEach(function(ce){
            $scope.cesDisplayNames[ce['dev-id']] = ce['device-name'] ? ce['device-name'] : ce['dev-id'];
          });
          $scope.updateEvcView();
        });
      });

      CpeuiSvc.getNetworkNames(function(networks){
        networks.forEach(function(net){
          $scope.networkNames[net.uuid] = net.name;
        });
      });

      CpeuiSvc.getProfiles(function(profiles) {
        $scope.profiles = profiles;
      });
    }

    $scope.updateUnis = function(callback) {
      CpeuiSvc.getUnis(function(unis) {
        unis.forEach(function(u) {
          u.prettyID = u['uni-id'].split(":")[u['uni-id'].split(":").length - 1];
          $scope.unisMap[u['uni-id']] = u;
        });
        $scope.unis = unis.filter(function(u){return u["tenant-id"] == $scope.curTenant;});

        if (callback) {
          callback($scope.unis);
        }
      });
    };

    $scope.updateEvcView = function() {
      CpeuiSvc.getServices($scope.curTenant, function(services) {

        $scope.evcs = services.filter(function(svc){ return svc.evc != undefined;});
        $scope.ipvcs = services.filter(function(svc){ return svc.ipvc != undefined;});
        $scope.updateUnis();
        function mapUniToService(uni, service) {
            var uniObj = $scope.unis.filterByField('uni-id',uni['uni-id'])[0];
            if (uniObj === undefined) {
                return;
            }
            if (!uniObj.vlanToService) {
                uniObj.vlanToService = [];
            }
            uniObj.hasService = true;
            if (uni['evc-uni-ce-vlans'] && uni['evc-uni-ce-vlans']['evc-uni-ce-vlan']){
                uni['evc-uni-ce-vlans']['evc-uni-ce-vlan'].forEach(function(v){
                    uniObj.vlanToService.push({"vlan":v.vid, "svc":service});
                });
            } else {
                if (uni["ip-uni-id"]) {
                    var ipuni = $scope.unis.filterByField('uni-id',uni['uni-id'])[0];
                    if (ipuni && ipuni["ip-unis"] && ipuni["ip-unis"]["ip-uni"]) {
                        ipuni["ip-unis"]["ip-uni"].forEach(function(ipu){
                            if (ipu['ip-uni-id'] == uni["ip-uni-id"]){
                                var vlan = ipu.vlan ? ipu.vlan : 0;
                                uniObj.vlanToService.push({"vlan":vlan, "svc":service});
                            }
                        });
                    }
                } else {
                    uniObj.vlanToService.push({"vlan":0, "svc":service});
                }
            }
        }

        $scope.ipvcs.forEach(function(e){
          if (e.ipvc.unis != undefined && e.ipvc.unis.uni != undefined){
              e.ipvc.unis.uni.forEach(function(u){
                u.device = u['uni-id'].split(":")[u['uni-id'].split(":").length-2];
                u.prettyID = u['uni-id'].split(":")[u['uni-id'].split(":").length-1];
                mapUniToService(u,e);
            });
          }
        });
        $scope.evcs.forEach(function(e){
          e.isTree = (e.evc['evc-type'] == 'rooted-multipoint');
          e.device2unis = {};
          if (e.evc.unis != undefined && e.evc.unis.uni != undefined){
            e.evc.unis.uni.forEach(function(u){
              if (e.device2unis[$scope.unisMap[u['uni-id']].device] == undefined){
                e.device2unis[$scope.unisMap[u['uni-id']].device] = [];
              }
              u.prettyID = u['uni-id'].split(":")[u['uni-id'].split(":").length - 1];
              e.device2unis[$scope.unisMap[u['uni-id']].device].push(u);
              mapUniToService(u,e);
            });
          }
        });
      });
      CpeuiSvc.getAllIpUniSubnets(function(raw_subnets){
          var subnets ={}
          if (raw_subnets) {
              raw_subnets.forEach(function(sub) {
                if (subnets[sub["uni-id"]] == undefined) {
                  subnets[sub["uni-id"]] = {};
                }
                if (subnets[sub["uni-id"]][sub["ip-uni-id"]] == undefined) {
                  subnets[sub["uni-id"]][sub["ip-uni-id"]] = [];
                }
                subnets[sub["uni-id"]][sub["ip-uni-id"]].push(sub);
              });
          }
        $scope.subnets = subnets;
      });
    };

    $scope.doesAllUniHasService = function(ceUnis) {
        for (var i=0 ; i< ceUnis.length; ++i) {
            if (!ceUnis[i].hasService) {
                return false;
            }
        }
        return true;
    }

    $scope.title = function(str) {
      if (!str) {
        return str;
        }
      return str.split('-').map(function(s) {
        return s[0].toUpperCase() + s.slice(1);
      }).join(' ');
    }

    var evcTypes = {
      'epl' : 'point-to-point',
      'evpl' : 'point-to-point',
      'eplan' : 'multipoint-to-multipoint',
      'evplan' : 'multipoint-to-multipoint',
      'eptree' : 'rooted-multipoint',
      'evptree' : 'rooted-multipoint'
    }

    var addEvcController = function($scope, $mdDialog) {
      $scope.initObj = function(svc) {
          $scope.obj = angular.merge($scope.obj, svc);
          if (!$scope.obj.evc) {
              $scope.obj.evc = {};
          }
          if (!$scope.obj.evc['max-svc-frame-size']){
              $scope.obj.evc['max-svc-frame-size'] = 1522;
          }
          if (!$scope.obj.evc['mac-timeout']){
              $scope.obj.evc['mac-timeout'] = 300;
          }
      }
      $scope.validate = function(form) {
        form.svc_type.$setTouched(); // patch because angular bug http://stackoverflow.com/questions/36138442/error-not-showing-for-angular-material-md-select
        console.log($scope);
        return form.$valid;
      };
    };

    $scope.editEvc = function($event, svc) {
        new CpeuiDialogs.Dialog('AddEvc', {}, function(obj) {
            obj['svc-id'] = svc['svc-id'];
            CpeuiSvc.addEvc(obj, evcTypes[obj['svc-type']], $scope.curTenant, function() {
                  $scope.updateEvcView();
                });
          }, addEvcController).show($event, {svc:svc});
    }

    $scope.openMenu = function($mdOpenMenu, ev) {
        originatorEv = ev;
        $mdOpenMenu(ev);
      };

    $scope.evcDialog = new CpeuiDialogs.Dialog('AddEvc', {}, function(obj) {
      CpeuiSvc.addEvc(obj, evcTypes[obj.svc_type], $scope.curTenant, function() {
            $scope.updateEvcView();
          });
    }, addEvcController);

    $scope.changeProfile = function(svcId, svcType, uni, profile) {
        var uniKey = (svcType == 'evc') ? uni['uni-id'] : (uni['uni-id'] +'/' + uni['ip-uni-id']);
        CpeuiDialogs.customConfirm("Are you sure?",
                "This will change " + uniKey + " bandwidth profile.",
                function() {
                    CpeuiSvc.changeUniProfile(svcId, svcType, uniKey, profile);
                },function() {
                    uni.selectedProfile = uni['ingress-bw-profile']
                });
    }


    $scope.ipvcDialog = new CpeuiDialogs.Dialog('AddIpvc', {}, function(obj) {
      CpeuiSvc.addIpvc(obj, $scope.curTenant, function() {
            $scope.updateEvcView();
          });
    });

    $scope.linkIpvcUniDialog = new CpeuiDialogs.Dialog('LinkIpvcUni', {},
        function(obj) {
        CpeuiSvc.addIpUni(obj.uni['uni-id'], obj['ip-address'], obj.vlan, obj['segmentation-id'], function(ipUniId) {
          CpeuiSvc.addIpvcUni(obj.svc_id, obj.uni['uni-id'], ipUniId, obj.profile_name,
              function() {
                  $scope.updateUnis($scope.updateEvcView);
              });
            });
        });

    var ipUniDialogController = function($scope, $mdDialog) {
      $scope.hasVlans = false;
      if ($scope.params.uni['ip-unis'] && $scope.params.uni['ip-unis']['ip-uni']) {
        var ipunis = $scope.params.uni['ip-unis']['ip-uni'];
        for (i = 0; i < ipunis.length; i++) {
          if (ipunis[i].vlan){
            $scope.hasVlans = true;
          }
        }
      }
    };

    $scope.ipUniDialog = new CpeuiDialogs.Dialog('AddIpUni', {}, function(obj) {
      CpeuiSvc.addIpUni(obj['uni-id'], obj['ip-address'], obj.vlan, obj['segmentation-id'], function() {
          $scope.updateUnis();
        });
      }, ipUniDialogController);

    $scope.openIpUniDialog = function(event,uni){
      if (uni['ip-unis'] && (uni['ip-unis']['ip-uni'] != undefined)){
        var ipunis = uni['ip-unis']['ip-uni'];
        for (i = 0; i < ipunis.length; i++) {
          if (!ipunis[i].vlan){
            CpeuiDialogs.alert("Error","You Can't have more then one ip-uni with no vlan. please remove the non-vlan ip-uni before adding new.")
            return;
          }
        }
      }
      $scope.ipUniDialog.show(event,{'uniid':uni['uni-id'], uni:uni})
    }


    var staticRoutingController = function($scope, $mdDialog, params) {
        $scope.add = function(obj){
            if ($scope.projectForm.$valid) {
                if (!obj.selectedUni.subnets) {
                    obj.selectedUni.subnets = [];
                }
                obj.selectedUni.subnets.push({"subnet":obj.network,"gateway":obj.gateway});

                if (!$scope.toAdd) {
                    $scope.toAdd = [];
                }
                $scope.toAdd.push(obj);

                // reset form
                $scope.obj={};
                $scope.projectForm.$setPristine();
                $scope.projectForm.$setUntouched();

            }
        };

        $scope.removeSubnet = function(u,subnet) {
            if (!$scope.toRemove) {
                $scope.toRemove = [];
            }
            u.subnets = u.subnets.filter(s=>s!=subnet);
            $scope.toRemove.push({"uni":u, "subnet":subnet});
        }

        $scope.done = function() {
            if (!$scope.toRemove && !$scope.toAdd) {
                $mdDialog.hide();
                return;
            }
            CpeuiSvc.getAllIpUniSubnets(function(subnets){
                if ($scope.toRemove) {
                    $scope.toRemove.forEach(function(u){
                        subnets = subnets.filter(function(s) {
                            if (s['uni-id'] != u.uni['uni-id']) {
                                return true;
                            } else if (s['ip-uni-id'] != u.uni['ip-uni-id']) {
                                return true;
                            } else if (s.subnet != u.subnet.subnet) {
                                return true;
                            }
                            return false;
                        });
                    });
                }
                if ($scope.toAdd) {
                    $scope.toAdd.forEach(function(added){
                        var u = added.selectedUni;
                        subnets.push({
                            "ip-uni-id":u['ip-uni-id'],
                            "subnet":added.network,
                            "gateway":added.gateway,
                            "uni-id":u['uni-id'],
                        });
                    });
                }
                CpeuiSvc.setAllSubnets(subnets, $scope.callback);
            });
          $mdDialog.hide();
        };

      };

    $scope.openRoutingDialog = function(ipvc) {
        if (ipvc.ipvc.unis && ipvc.ipvc.unis.uni) {
            ipvc.ipvc.unis.uni.forEach(function(u){
                var mefUni = $scope.getMefInterfaceIpvc(u['uni-id'],u['ip-uni-id']);
                if (mefUni === undefined) {
                    return;
                }
                u.ipAddress = mefUni['ip-address'];
                u.deviceName = $scope.cesDisplayNames[u.device];
                if ($scope.subnets[u['uni-id']]) {
                    u.subnets = $scope.subnets[u['uni-id']][u['ip-uni-id']];
                }
                if (mefUni.vlan) { u.vlan = mefUni.vlan };
            });
        }
        new CpeuiDialogs.Dialog('StaticRouting', {"ipvc":ipvc, "subnets":angular.copy($scope.subnets)}, function() {
            $scope.updateEvcView();
        }, staticRoutingController).show();
    }
    
    var dhcpDialogController = function($scope, $mdDialog, params) {
        
        $scope.selectedNetworks = [];
        $scope.vrfid = params.vrfId;
        $scope._isAddingRow = false;
        
        $scope.selectAll = function(){
            if (!$scope.isAllSelected()){
                $scope.selectedNetworks = $scope.getAvailableNetwork();                
            } else {
                $scope.selectedNetworks = [];                
            }
        };
        $scope.isPartialSelected = function(){
            return ($scope.selectedNetworks.length != 0) && ($scope.selectedNetworks.length != $scope.getAvailableNetwork().length);
        };
        $scope.isAllSelected = function(){
            return ($scope.selectedNetworks.length ==  $scope.getAvailableNetwork().length);
        };
        
        $scope.updateEnabled = function() {
            $scope.selectedNetworks.forEach(function(subnet){
                $scope.addDhcp(subnet);
            });
            $scope.selectedNetworks = [];
            $('md-backdrop').click();// close md-select dropdown
        };

        $scope.openStaticAssigment = function(subnet) {
            $scope.focusedSubnet = subnet;
            CpeuiSvc.getDhcpStaticAllocation($scope.vrfid, subnet.subnet, function(allocations) {
                $scope.subnetAllocations = allocations;
                $scope.dialogState = 'STATIC';
            });
        };
        $scope.openMain = function() {
            $scope.dialogState = 'MAIN';
        };
        //confirmation
        $scope.confirmationMsg = null;
        $scope.okCallback = null;
        $scope.previousState = null;

        $scope.confirm = function(msg, okCallback) {
            $scope.confirmationMsg = msg;
            $scope.okCallback = okCallback;
            $scope.previousState = $scope.dialogState;
            $scope.dialogState = 'CONFIRM';
        }
        $scope.back = function() {
            $scope.dialogState = $scope.previousState;
        }
        $scope.confirmed = function() {
            $scope.okCallback()
            $scope.dialogState = $scope.previousState;
        }

        $scope.updateDhcpData = function() {
            CpeuiSvc.getDhcp($scope.vrfid, function(dhcps){
                $scope.dhcps = dhcps;
                //$scope.params.ipvcUnis = $scope.params.ipvcUnis.filter(x => $scope.dhcps[x.mefUni['ip-address']] == undefined);
            });
        }

        $scope.getAvailableNetwork = function(){
            if ($scope.dhcps !== undefined) {
                return $scope.params.ipvcUnis.filter(x => ($scope.dhcps[x.cidr] == undefined));
            } else {
                return $scope.params.ipvcUnis;
            }
        }

        $scope.addDhcp = function(subnet) {
            CpeuiSvc.addDhcp($scope.vrfid, subnet.cidr, subnet.min ,subnet.max,function(){
                // TODO find a way to getDhcp only once, after the last add
                $scope.updateDhcpData();
            });
        }

        $scope.removeDhcp = function(subnet) {
            $scope.confirm("This will delete this dhcp configuration",function(){
                CpeuiSvc.removeDhcp($scope.vrfid, subnet, function(){
                    $scope.updateDhcpData();
                });
            });
        };
        $scope.isAddingRow = function() {
            return $scope._isAddingRow;
        }
        $scope.setAddingRow = function(val) {
            $scope._isAddingRow = val;
        }
        $scope.addAllocation = function(form, mac, ip) {
            if (form.$valid) {
                CpeuiSvc.addDhcpStaticAllocation($scope.vrfid, $scope.focusedSubnet.subnet, [[mac,ip]], function(){
                    CpeuiSvc.getDhcpStaticAllocation($scope.vrfid, $scope.focusedSubnet.subnet, function(allocations) {
                        $scope.subnetAllocations = allocations;
                    });
                });

                // reset form
                delete $scope.obj.mac;
                delete $scope.obj.ip;
                form.$setPristine();
                form.$setUntouched();
                $scope.setAddingRow(false);
            }
        }

        $scope.removeAllocation = function(subnet, mac) {
            $scope.confirm("Are you sure you want to remove this allocation?",function(){
                CpeuiSvc.removeDhcpStaticAllocation($scope.vrfid, subnet, mac, function(){
                    CpeuiSvc.getDhcpStaticAllocation($scope.vrfid, subnet, function(allocations) {
                        $scope.subnetAllocations = allocations;
                    });
                });
            });
        };

        $scope.setDNS = function(primaryDns, secondaryDns) {
            for (var i in $scope.dhcps) {
                var allocPool = $scope.dhcps[i];
                CpeuiSvc.setDHCPDnsServers($scope.vrfid, allocPool.subnet, primaryDns, secondaryDns, $scope.updateDhcpData);
            };
        }

        $scope.updateDhcpData();
    };

    $scope.openDhcpDialog = function(ipvc) {
        CpeuiSvc.getServicesVrfId(ipvc['svc-id'],function(vrfId){
            params = {ipvc:ipvc,ipvcUnis:[],vrfId:vrfId};
            if (ipvc.ipvc.unis && ipvc.ipvc.unis.uni) {
                params.ipvcUnis = ipvc.ipvc.unis.uni.map(u => CpeUiUtils.getSubnetEdges($scope.getMefInterfaceIpvc(u['uni-id'],u['ip-uni-id'])['ip-address']));
            }
            new CpeuiDialogs.Dialog('DHCP', params, function() {
                $scope.updateEvcView();
            }, dhcpDialogController).show();
        });
    }

    $scope.ipUniSubnetDialog = new CpeuiDialogs.Dialog('AddIpUniSubnet', {}, function(obj) {
      CpeuiSvc.addIpUniSubnet(obj.uniid, obj.ipuniid, obj.subnet, obj.gateway, function() {
        if ($scope.subnets[obj.uniid] == undefined){
          $scope.subnets[obj.uniid] = {};
        }
        if ($scope.subnets[obj.uniid][obj.ipuniid] == undefined) {
          $scope.subnets[obj.uniid][obj.ipuniid] = [];
        }
        $scope.subnets[obj.uniid][obj.ipuniid].push({
          "uni-id": obj.uniid,
          "ip-uni-id": obj.ipuniid,
          "subnet": obj.subnet,
          "gateway": obj.gateway
        });
      });
    });

    $scope.deleteIpUni = function(uniid, ipuni_id) {
        CpeuiDialogs.confirm(function() {
          CpeuiSvc.deleteIpUni(uniid, ipuni_id, function() {
            $scope.updateEvcView(); // TODO update unis only
          });
        });
      };
      $scope.deleteIpvcUni = function(svc_id, uni_id, ipuni_id) {
        CpeuiDialogs.confirm(function() {
          CpeuiSvc.deleteIpvcUni(svc_id, uni_id, ipuni_id, function() {
            $scope.updateEvcView();
          });
        });
      };
      $scope.getMefInterfaceIpvc = function(uni_id,ipuni_id){
        var uni = $scope.unis.filterByField('uni-id',uni_id)[0];
        if ((uni == undefined) || (uni['ip-unis'] == undefined) || (uni['ip-unis']['ip-uni'] == undefined)) {
          return undefined;
        }
        return uni['ip-unis']['ip-uni'].filterByField('ip-uni-id',ipuni_id)[0];
      }

      $scope.deleteIpUniSubnet = function(uniid, ipuni_id, subnet) {
          CpeuiDialogs.confirm(function() {
            CpeuiSvc.deleteIpUniSubnet(uniid, ipuni_id, subnet, function() {
              $scope.updateEvcView(); // TODO update unis only
            });
          });
    };

    $scope.deleteEvc = function(svcid) {
      CpeuiDialogs.confirm(function() {
        CpeuiSvc.removeEvc(svcid, function() {
          $scope.updateEvcView();
        });
      });
    };

    $scope.deleteEvcUni = function(svc_id, uni_id) {
      CpeuiDialogs.confirm(function() {
        CpeuiSvc.deleteEvcUni(svc_id, uni_id, function() {
          $scope.updateEvcView();
        });
      });
    };

    var linkEvcUniController = function($scope, $mdDialog, params) {
      $scope.params = params;
      $scope.obj = {
        vlans : []
      };
      $scope.deleteVlan = function(vlan) {
        $scope.obj.vlans.splice($scope.obj.vlans.indexOf(vlan), 1);
      };
      $scope.addVlan = function(vlan) {
        if ($scope.obj.vlans.indexOf(vlan) == -1) {
          $scope.obj.vlans.push(vlan);
          console.log(vlan);
        }
        $('#vlan_input').val(undefined);
      };

      $scope.filterUsedUnis = function(evc){
        return function(u) {
          if (u.prettyID == 'br-int') {
            return false;
          }
          if (u.prettyID && u.prettyID.startsWith('tun')) {
            return false;
          }
          if (evc.evc == undefined || evc.evc.unis.uni == undefined){
            return true;
          }
          return evc.evc.unis.uni.filterByField('uni-id',u['uni-id']).length == 0;
        };
      };
    };

    $scope.linkEvcUniDialog = new CpeuiDialogs.Dialog('LinkEvcUni', {},
        function(obj) {
          if (!obj.role) {
            obj.role = "root";
          }
          CpeuiSvc.addEvcUni(obj.svc_id, obj.uni_id, obj.role, obj.vlans, obj.profile_name,
              function() {
                $scope.updateEvcView();
              });
        }, linkEvcUniController);

    var editVlanController = function($scope, $mdDialog, params) {
      $scope.params = params;

      $scope.deleteVlan = function(svc_id, uni_id, vlan, allvlans) {
        CpeuiSvc.deleteVlan(svc_id, uni_id, vlan, function() {
          allvlans.splice(allvlans.indexOf(vlan), 1)
        });
      };
      $scope.addVlan = function(svc_id, uni_id, vlan, allvlans) {
        if (allvlans == undefined) {
          allvlans = [];
        }
        if (allvlans.indexOf(vlan) == -1) {
          CpeuiSvc.addVlan(svc_id, uni_id, vlan, function() {
            allvlans.push(vlan);
          });
        }
        $('#vlan_input').val(undefined);
      };
    };

    $scope.editVlanDialog = new CpeuiDialogs.Dialog('EditVlans', {}, undefined, editVlanController);

    $scope.sortUni = function(uni) {
      return uni['uni-id'];
    };

    $scope.getKeys = function(obj){
        var keys = [];
        for(var keyName in obj){
            keys.push(keyName)
        }
        return keys;
    }
    $scope.sortCeFromId = function(ce) {
        return $scope.cesDisplayNames[ce];
    };

    $scope.isEmpty = function(obj){
        return angular.equals({}, obj);
    }

    init();
  });
});
