define(['app/cpeui/cpeui.module'],function(cpeui) {

    cpeui.factory('CpeuiSvc', function($http) {
        var baseUrl = "/restconf/config/mef-global:mef-global/";
        var svc = {};

        svc.getTenantList = function(callback) {
            var tenantList = [];
            $http({
                method:'GET',
                url:baseUrl + "tenants-instances"
            }).then(function successCallback(response) {
                tenantList = response.data["tenants-instances"]["tenant-list"];
                if (callback != undefined) {
                    callback(tenantList);
                }
            }, function errorCallback(response) {
                if (response.status == 404) {
                  callback([]);
                } else {
                  console.log(response);
                }
            });
        };

        svc.addTenant = function(name, callback){
            $http({
                method:'POST',
                url:baseUrl + "tenants-instances/",
                data: {"tenant-list":[{
                      "name": name
                    }]}
            }).then(function successCallback(response) {
                if (callback != undefined) {
                    callback();
                }
            });
        };
        svc.deleteTenant = function(id, callback) {
            $http({
                method:'DELETE',
                url:baseUrl + "tenants-instances/tenant-list/" + id + "/",
            }).then(function successCallback(response) {
                if (callback != undefined) {
                    callback();
                }
            });
        };

        // Profiles
        svc.getProfiles = function(callback) {
          $http({
              method:'GET',
              url:"/restconf/config/mef-global:mef-global/profiles/ingress-bwp-flows/"
          }).then(function successCallback(response) {
              if (callback != undefined) {
                  callback(response.data["ingress-bwp-flows"]["bwp-flow"]);
              }
          }, function errorCallback(response) {
              if (response.status == 404) {
                  callback([]);
              }
              console.log(response);
          });
      };

      svc.addProfile = function(name, cir, cbs, callback){
          $http({
              method:'POST',
              url:"/restconf/config/mef-global:mef-global/profiles/ingress-bwp-flows/",
              data: {"bwp-flow":{
                        "bw-profile" : name,
                         "cir" : cir,
                         "cbs" : cbs
                    }}
          }).then(function successCallback(response) {
              if (callback != undefined) {
                  callback();
              }
          });
      };

      svc.deleteProfile = function(name, callback) {
          $http({
              method:'DELETE',
              url:"/restconf/config/mef-global:mef-global/profiles/ingress-bwp-flows/bwp-flow/"+name,
          }).then(function successCallback(response) {
              if (callback != undefined) {
                  callback();
              }
          });
      };

        // CEs
        svc.addCe = function(id, name, callback) {
            $http({
                method:'POST',
                url:"/restconf/config/mef-topology:mef-topology/devices/",
                data:{
                      "device": [
                                 {
                                   "dev-id": id,
                                   "device-name":name,
                                   "role": "ce",
                                   "interfaces": {"interface": []}
                                 }
                               ]
                             }

            }).then(function successCallback(response) {
                if (callback != undefined) {
                    callback();
                }
            });
        };

        svc.addCeName = function(ce, new_name, callback) {
          $http({
            method:'POST',
            url:"/restconf/config/mef-topology:mef-topology/devices/device/" + ce['dev-id'],
            data: {"device-name": new_name}
        }).then(function successCallback(response) {
            if (callback != undefined) {
                callback();
            }
          }, function errorCallback(response) {
            console.log(response);
            $http({
              method:'GET',
              url:"/restconf/config/mef-topology:mef-topology/devices/device/" + ce['dev-id']
            }).then(function successCallback(response) {
              ce["device-name"] = response.data["device"][0]["device-name"];
            });
          });
        };

        svc.getCes = function(callback) {
            var ces;
            var operMap = {};

            $http({
                method:'GET',
                url:"/restconf/operational/mef-topology:mef-topology/devices/"
            }).then(function successCallback(response) {
                ces = response.data["devices"]["device"];
                ces.forEach(function(c) {
                  c.displayName = c['dev-id'];
                  operMap[c['dev-id']] = c;
                });
            }).finally(function() {
                $http({
                  method:'GET',
                  url:"/restconf/config/mef-topology:mef-topology/devices/"
                }).then(function(response){
                  var confCes = response.data["devices"]["device"];
                  confCes.forEach(function(c) {
                    c.displayName = c['device-name'] ? c['device-name'] : c['dev-id'];
                    if (operMap[c['dev-id']]) {
                      for (var attrname in c) {
                        operMap[c['dev-id']][attrname] = c[attrname];
                      }
                    } else {
                      operMap[c['dev-id']] = c;
                    }
                  });
                }).finally(function() {
                  if (callback != undefined) {
                    callback(Object.values(operMap));
                  }
                });
            });
        };

        svc.removeCe = function(ceid, callback) {
             $http({
                method:'DELETE',
                url:"/restconf/config/mef-topology:mef-topology/devices/device/" + ceid + "/"
            }).then(function successCallback(response) {
                if (callback != undefined) {
                    callback();
                }
            });
        };

        // UNIs

        svc.addUni = function(id, tenantid, callback) {
            $http({
                method:'POST',
                url:"/restconf/config/mef-interfaces:mef-interfaces/unis/",
                data:{"uni": [{
                    "uni-id": id,
                    "tenant-id":tenantid,
                    "admin-state-enabled":true
                    }]}
            }).then(function successCallback(response) {
                if (callback != undefined) {
                    callback();
                }
            });
        };


        svc.updateUni = function(id, tenantid, callback) {
            // TODO didn't find a better way to keep other uni fields, PATCH method is not supported :(
            $http({
                method:'GET',
                url:"/restconf/operational/mef-interfaces:mef-interfaces/unis/uni/" + id + "/"
            }).then(function successCallback(response) {
                uni = response.data;
                uni["uni"][0]["tenant-id"] = tenantid;
                if (!tenantid) {
                  delete uni["uni"][0]["tenant-id"];
                }
                uni["uni"][0]["admin-state-enabled"] = true;
                $http({
                  method:'PUT',
                  url:"/restconf/config/mef-interfaces:mef-interfaces/unis/uni/" + id + "/",
                    data:uni
                }).then(function successCallback(response) {
                    if (callback != undefined) {
                        callback();
                    }
                });
            });
        };

        svc.getUnis = function(callback) {
            var unis;
            $http({
                method:'GET',
                url:"/restconf/operational/mef-interfaces:mef-interfaces/unis/"
            }).then(function successCallback(response) {
                unis = response.data["unis"]["uni"];
                if (unis != undefined){
                    for (i=0; i < unis.length; i++) {
                        if ((unis[i]["physical-layers"] != undefined) && (unis[i]["physical-layers"].links != undefined)){
                            unis[i].device = unis[i]["physical-layers"].links.link[0].device;
                        }
                    }
                }
                var confMap = {}
                $http({
                  method:'GET',
                  url:"/restconf/config/mef-interfaces:mef-interfaces/unis/"
                }).then(function(response){
                  var confUnis = response.data["unis"]["uni"];
                  confUnis.forEach(function(u) {
                    confMap[u['uni-id']] = u;
                  });
                }).finally(function(){
                  unis.forEach(function(u) {
                    u.prettyID = u['uni-id'].split(":")[u['uni-id'].split(":").length - 1];
                    // copy config fields like tenant-id
                    if (confMap[u['uni-id']]){
                      for (var attrname in confMap[u['uni-id']]) {
                        u[attrname] = confMap[u['uni-id']][attrname]; 
                      }
                    }
                  });
                  if (callback != undefined) {
                    callback(unis);
                  }
                });
            }, function errorCallback(response) {
                if (response.status == 404) {
                  callback([]);
                }
                console.log(response);
            });
        };

        svc.getTenantUnis = function(tenantid, callback) {
            var unis;
            svc.getUnis(function(unis){
                if (unis != undefined){
                    unis = unis.filter(function(u){return u["tenant-id"] == tenantid;});
                }
                if (callback != undefined) {
                    callback(unis);
                }
            });
        };

        svc.removeUni = function(uniid, callback) {
             $http({
                method:'DELETE',
                url:"/restconf/config/mef-interfaces:mef-interfaces/unis/uni/" + uniid + "/"
            }).then(function successCallback(response) {
                if (callback != undefined) {
                    callback();
                }
            });
        };

        // IPVCs
        svc.addIpvc = function(ipvc, tenant, callback) {
          var data = {
            "mef-service" :  {
              "svc-id" : ipvc.id,
              "svc-type" : 'eplan',
              "tenant-id" : tenant,
              "ipvc" : {
                "ipvc-id" : ipvc.id,
                "ipvc-type" : 'multipoint',
              }
            }
          };
          $http({
              method:'POST',
              url:"/restconf/config/mef-services:mef-services/",
              data:data
          }).then(function successCallback(response) {
              if (callback != undefined) {
                  callback();
              }
          });
      };

      svc.addIpUni = function(uniid, ipuni_id, ip_address, vlan, callback) {
        var data = {"ip-uni":{
          "ip-uni-id": ipuni_id,
          "ip-address": ip_address
        }};
        if (vlan){
          data["ip-uni"].vlan = vlan;
        }
        $http({
            method:'POST',
            url:"/restconf/config/mef-interfaces:mef-interfaces/unis/uni/"+uniid+"/ip-unis/",
            data:data
        }).then(function successCallback(response) {
            if (callback != undefined) {
                callback();
            }
        });
    };
    
    svc.getAllIpUniSubnets = function(callback) {
      $http({
          method:'GET',
          url : "/restconf/config/mef-interfaces:mef-interfaces/subnets/"
      }).then(function successCallback(response) {
          var raw_subnets = response.data["subnets"]["subnet"];
          var subnets ={}
          raw_subnets.forEach(function(sub){
            if (subnets[sub["uni-id"]] == undefined) {
              subnets[sub["uni-id"]] = {};
            }
            if (subnets[sub["uni-id"]][sub["ip-uni-id"]] == undefined) {
              subnets[sub["uni-id"]][sub["ip-uni-id"]] = [];
            }
            subnets[sub["uni-id"]][sub["ip-uni-id"]].push(sub);
          });
          if (callback != undefined) {
              callback(subnets);
          }
      }, function errorCallback(response) {
          console.log(response);
      });
  };

  svc.addIpUniSubnet = function(uniid, ipuniid, subnet, gateway, callback) {
        var data = {
            "subnet": 
            {
              "subnet": subnet,
              "uni-id":uniid,
              "ip-uni-id":ipuniid,
              "gateway": gateway
            }
        };
        $http(
            {
              method : 'POST',
              url : "/restconf/config/mef-interfaces:mef-interfaces/subnets/",                                
              data : data
            }).then(function successCallback(response) {
          if (callback != undefined) {
            callback();
          }
        });
      };
    
    svc.deleteIpUniSubnet = function(uniid, ipuni_id, subnet, callback) {
        
        $http({
            method:'DELETE',
            url:"/restconf/config/mef-interfaces:mef-interfaces/subnets/subnet/"+uniid+"/"+ipuni_id+"/"+subnet.replace("/","%2F")+"/"
        }).then(function successCallback(response) {
            if (callback != undefined) {
                callback();
            }
        });
    };
    svc.deleteIpUni = function(uniid, ipuni_id, callback) {
        
        $http({
            method:'DELETE',
            url:"/restconf/config/mef-interfaces:mef-interfaces/unis/uni/"+uniid+"/ip-unis/ip-uni/"+ipuni_id+"/"
        }).then(function successCallback(response) {
            if (callback != undefined) {
                callback();
            }
        });
    };
    
    svc.getIpUniSubnets = function(uniid, ipuni_id, callback) {
      $http({
          method:'GET',
          url:"/restconf/config/mef-interfaces:mef-interfaces/subnets/"
            //subnet/"+uniid+"/ip-unis/ip-uni/"+ipuni_id+"/subnets"
      }).then(function successCallback(response) {
          subnets = response.data["subnets"]["subnet"];
          subnets = subnets.filterByField('uni-id',uniid).filterByField('ip-uni-id',ipuni_id);
          if (callback != undefined) {
              callback(subnets);
          }
      });
    };

    
    // EVCs
    function getJsonUnis(unis) {
            var uni_json = [];
            if (unis == undefined) {
                unis = [];
            }
            unis.forEach(function(i){uni_json.push({"uni-id":i});});
            return uni_json;
        }
        svc.addEvc = function(evc, evc_type, tenant, callback) {
            var uni_json = getJsonUnis(evc.unis);
//            preserved-vlan
            var data = {
              "mef-service" :  {
                "svc-id" : evc.id,
                "svc-type" : evc.svc_type,
                "tenant-id" : tenant,
                "evc" : {
                  "evc-id" : evc.id,
                  "evc-type" : evc_type,
                  "preserve-ce-vlan-id" : evc.is_preserve_vlan,
                  "max-svc-frame-size" : evc.mtu_size,
                  "unicast-svc-frm-delivery" : evc.unicast,
                  "multicast-svc-frm-delivery" : evc.multicast,
                  "unis" : {
                    "uni" : uni_json
                  },
                  "admin-state-enabled" : true
                }
              }
            };
            if (evc.is_preserve_vlan) {
              data["mef-service"]["evc"]["preserved-vlan"] = evc.preserved_vlan;
            }
            $http({
                method:'POST',
                url:"/restconf/config/mef-services:mef-services/",
                data:data
            }).then(function successCallback(response) {
                if (callback != undefined) {
                    callback();
                }
            });
        };
        svc.getServices = function(tenantid, callback) {
            var evcs;
            $http({
                method:'GET',
                url:"/restconf/config/mef-services:mef-services/"
            }).then(function successCallback(response) {
                evcs = response.data["mef-services"]["mef-service"]; // TODO try to filter on server side
                if (evcs != undefined) {
                    evcs = evcs.filter(function(evc){return evc["tenant-id"] == tenantid;});
                    for (i=0; i < evcs.length; i++) {
                        if ((evcs[i].evc != undefined) && (evcs[i].evc.unis.uni != undefined)) {
                            var unis = evcs[i].evc.unis.uni;
                            for (j=0; j < unis.length; j++) {
                                if ((unis[j]['evc-uni-ce-vlans'] != undefined) && (unis[j]['evc-uni-ce-vlans']['evc-uni-ce-vlan'] != undefined)){
                                    unis[j].vlans = unis[j]['evc-uni-ce-vlans']['evc-uni-ce-vlan'].map(function(u){return u.vid;}).sort();
                                } else {
                                    unis[j].vlans = [];
                                }
                            }
                        }
                    }
                }
                if (callback != undefined) {
                    callback(evcs);
                }
            }, function errorCallback(response) {
                console.log(response);
            });
            return evcs;
        };

        svc.getAllServices = function(callback) {
          $http({
              method:'GET',
              url:"/restconf/config/mef-services:mef-services/"
          }).then(function successCallback(response) {
              if (callback != undefined) {
                  callback(response.data["mef-services"]["mef-service"]);
              }
          }, function errorCallback(response) {
              if (response.status == 404) {
                callback([]);
              }
              console.log(response);
          });
        };

        svc.addTenantToService = function(svcId, tenantName, callbackSuccess, callbackFailure){
          $http({
            method:'POST',
            url:"/restconf/config/mef-services:mef-services/mef-service/" + svcId,
            data:{"tenant-id":tenantName}
          }).then(function() {
              if (callbackSuccess != undefined) {
                callbackSuccess();
              }
          }, function() {
            if (callbackFailure != undefined) {
              callbackFailure();
            } else {
              console.log(response);
            }
          });
        };

        svc.removeEvc = function(svcid, callback) {
             $http({
                method:'DELETE',
                url:"/restconf/config/mef-services:mef-services/mef-service/" + svcid + "/"
            }).then(function successCallback(response) {
                if (callback != undefined) {
                    callback();
                }
            });
        };

        svc.addIpvcUni = function(svcid, uni_id, ipuni_id, profile_name, callback) {
          var data = {"uni":{
                          "uni-id":uni_id,
                          "ip-uni-id":ipuni_id
                          }
                      };
          if (profile_name) {
            data.uni["ingress-bw-profile"] = profile_name;
          }
           $http({
              method:'PUT',
              url:"/restconf/config/mef-services:mef-services/mef-service/" + svcid + "/ipvc/unis/uni/"+uni_id+"/"+ipuni_id,
              data: data
          }).then(function successCallback(response) {
              if (callback != undefined) {
                  callback();
              }
          }, function failureCallback(response) {
              if (callback != undefined) {
                  callback();
              }
          });
      };
      
      svc.deleteIpvcUni = function(svcid, uni_id, ipuni_id, callback) {
        $http({
           method:'DELETE',
           url:"/restconf/config/mef-services:mef-services/mef-service/" + svcid + "/ipvc/unis/uni/" + uni_id +"/"+ipuni_id + "/"
       }).then(function successCallback(response) {
           if (callback != undefined) {
               callback();
           }
       });
   };
      
      
      
        svc.addEvcUni = function(svcid, uni_id, role, vlans, profile_name, callback) {
            var data = {"uni":{
                            "uni-id":uni_id,
                            "role":role,
                            "admin-state-enabled":true
                            }
                        };
            if (profile_name) {
              data.uni["ingress-bw-profile"] = profile_name;
            }
            if (vlans != undefined) {
                data.uni['evc-uni-ce-vlans'] = {"evc-uni-ce-vlan":[]}
                for (var i=0; i< vlans.length; ++i) {
                    data.uni['evc-uni-ce-vlans']["evc-uni-ce-vlan"].push({"vid":vlans[i]});
                }
            }
             $http({
                method:'POST',
                url:"/restconf/config/mef-services:mef-services/mef-service/" + svcid + "/evc/unis",
                data: data
            }).then(function successCallback(response) {
                if (callback != undefined) {
                    callback();
                }
            }, function failureCallback(response) {
                if (callback != undefined) {
                    callback();
                }
            });
        };

        svc.addEvcUniVlan = function(svcid, uni_id, vlan, callback) {
             $http({
                method:'POST',
                url:"/restconf/config/mef-services:mef-services/mef-service/" + svcid + "/evc/unis/uni/" + uni_id + "/evc-uni-ce-vlans/",
                data:{"evc-uni-ce-vlan":{"vid":vlan}}
            }).then(function successCallback(response) {
                if (callback != undefined) {
                    callback();
                }
            });
        };

        svc.deleteEvcUni = function(svcid, uni_id, callback) {
             $http({
                method:'DELETE',
                url:"/restconf/config/mef-services:mef-services/mef-service/" + svcid + "/evc/unis/uni/" + uni_id + "/"
            }).then(function successCallback(response) {
                if (callback != undefined) {
                    callback();
                }
            });
        };



        svc.deleteVlan = function(svc_id, uni_id, vlan,callback) {
            $http({
                method:'DELETE',
                url:"/restconf/config/mef-services:mef-services/mef-service/" + svc_id + "/evc/unis/uni/" + uni_id + "/evc-uni-ce-vlans/evc-uni-ce-vlan/"+ vlan+"/"
            }).then(function successCallback(response) {
                if (callback != undefined) {
                    callback();
                }
            });
        };

        svc.addVlan = function(svc_id, uni_id, vlan, callback) {
            $http({
                method:'PUT',
                url:"/restconf/config/mef-services:mef-services/mef-service/" + svc_id + "/evc/unis/uni/"+uni_id+"/evc-uni-ce-vlans/evc-uni-ce-vlan/"+vlan+"/",
                data:{"evc-uni-ce-vlan":{"vid":vlan}}
            }).then(function successCallback(response) {
                if (callback != undefined) {
                    callback();
                }
            });
        };

        svc.getNetworkNames = function(callback){
          $http({
            method:'GET',
            url:"/restconf/config/neutron:neutron/networks/"
        }).then(function successCallback(response) {
            if (callback != undefined) {
                callback(response.data.networks.network);
            }
        });
        };

        return svc;

    });
});
