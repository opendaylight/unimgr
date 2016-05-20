unimgr
------


#### Create FcRoute
```
POST :host/restconf/config/CoreModel-CoreNetworkModule-ObjectClasses:FcRouteList
Authorization: :basic-auth
Content-Type: application/json
{
   "FcRoute": {
       "id" : "new-route",
       "ForwardingConstruct": [
       {
          "uuid": "an-original-name",
          "FcPort": [ {
                     "id": "a-end",
                     "_ltpRefList": [ "host-a:a-end-ltp" ]
                     }, {
                     "id": "z-end",
                     "_ltpRefList": [ "host-z:z-end-ltp" ]
                     } ],
          "forwardingDirection": "BIDIRECTIONAL",
          "layerProtocolName": "funky-layer-protocol",
          "_fcSpecRef": "nonexistent-fcspec"
       }
   ] }
}
```

#### Delete FcRoute
```
DELETE :host/restconf/config/CoreModel-CoreNetworkModule-ObjectClasses:FcRouteList/FcRoute/new-route
Authentication: :basic-auth
```

#### List FcRoute
```
GET :host/restconf/config/CoreModel-CoreNetworkModule-ObjectClasses:FcRouteList
Authorization: :basic-auth
```
