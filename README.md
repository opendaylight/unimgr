unimgr
------

The unimgr project is an OpenDaylight component that implements the MEF Legato and Presto API
reference points to provide management of connectivity services across multi-vendor
devices. Currently supported devices are:

* Cisco IOS-XR devices managed via NETCONF

Building unimgr
----

To build and test the unimgr project:

```
% mvn clean install
```

If you want to skip the tests then:

```
% mvn clean install -DskipTests
```

Running unimgr
----

The unimgr project builds a karaf distribution that has the umimgr component as a deployed
feature. It is necessary to set a higher than default maximum heap size for the JVM when running
karaf:

```
% export JAVA_MAX_MEM=2g
% ./karaf/target/assembly/bin/karaf

Apache Karaf starting up. Press Enter to open the shell now...
100% [========================================================================]

Karaf started in 0s. Bundle stats: 10 active, 10 total

    ________                       ________                .__  .__       .__     __
    \_____  \ ______   ____   ____ \______ \ _____  ___.__.|  | |__| ____ |  |___/  |_
     /   |   \\____ \_/ __ \ /    \ |    |  \\__  \<   |  ||  | |  |/ ___\|  |  \   __\
    /    |    \  |_> >  ___/|   |  \|    `   \/ __ \\___  ||  |_|  / /_/  >   Y  \  |
    \_______  /   __/ \___  >___|  /_______  (____  / ____||____/__\___  /|___|  /__|
            \/|__|        \/     \/        \/     \/\/            /_____/      \/


Hit '<tab>' for a list of available commands
and '[cmd] --help' for help on a specific command.
Hit '<ctrl-d>' or type 'system:shutdown' or 'logout' to shutdown OpenDaylight.

opendaylight-user@root>
```

If you want to run karaf in debug mode so that you can attach a debugger:

```
% ./karaf/target/assembly/bin/karaf debug
Listening for transport dt_socket at address: 5005
Apache Karaf starting up. Press Enter to open the shell now...
```

You can then attach a debugger to localhost:5005

OpenDaylight Console
----

You can use the karaf console to verify that the unimgr is running:

```
opendaylight-user@root>bundle:list | grep unimgr | grep -v wrap
289 (0x Active   │  80 │ 0.6.0.SNAPSHOT  │ ODL :: unimgr :: unimgr-impl
290 (0x Active   │  80 │ 0.6.0.SNAPSHOT  │ ODL :: unimgr :: unimgr-nrp-tapi-api
```

To enable support for Ethernet Virtual Connection (EVC) services, install the following features via the OpenDaylight console:

```
opendaylight-user@root>feature:install odl-unimgr-legato-api
opendaylight-user@root>feature:install odl-unimgr-cisco-xr-driver 
opendaylight-user@root>bundle:list | grep unimgr | grep -v wrap
289 (0x Active   │  80 │ 0.6.0.SNAPSHOT  │ ODL :: unimgr :: unimgr-impl
290 (0x Active   │  80 │ 0.6.0.SNAPSHOT  │ ODL :: unimgr :: unimgr-nrp-tapi-api
364 (0x Active   │  80 │ 0.6.0.SNAPSHOT  │ ODL :: unimgr :: unimgr-legato-api
378 (0x Active   │  80 │ 0.6.0.SNAPSHOT  │ ODL :: unimgr :: unimgr-cisco-xr-driver
379 (0x Active   │  80 │ 0.6.0.SNAPSHOT  │ ODL :: unimgr :: cisco-xrmodels
```

You can use the karaf console to monitor the OpenDaylight log messages:

```
09:42:11.565 INFO [CommitFutures-1] Node xr-node created
09:42:11.567 INFO [features-3-thread-1] Blueprint bundle org.opendaylight.unimgr.cisco-xr-driver/0.6.0.SNAPSHOT has been started
09:42:11.567 INFO [Blueprint Event Dispatcher: 1] Blueprint container for bundle org.opendaylight.unimgr.cisco-xr-driver_0.6.0.SNAPSHOT [378] was successfully created
09:42:11.568 INFO [features-3-thread-1]   netconf-topology-config/1.8.1
09:42:11.571 INFO [opendaylight-cluster-data-notification-dispatcher-64] Abstract TAPI node updated successful
09:42:11.589 INFO [CommitFutures-1] netconf tree listener registered
09:42:11.597 INFO [features-3-thread-1] Starting NETCONF keystore service.
09:42:11.679 INFO [features-3-thread-1] Blueprint bundle netconf-topology-config/1.8.1 has been started
09:42:11.680 INFO [Blueprint Event Dispatcher: 1] Blueprint container for bundle netconf-topology-config_1.8.1 [366] was successfully created
09:42:11.681 INFO [features-3-thread-1]   org.opendaylight.netconf.aaa-authn-odl-plugin/1.8.1
09:42:11.689 INFO [features-3-thread-1] Blueprint bundle org.opendaylight.netconf.aaa-authn-odl-plugin/1.8.1 has been started
09:42:11.689 INFO [Blueprint Event Dispatcher: 1] Blueprint container for bundle org.opendaylight.netconf.aaa-authn-odl-plugin_1.8.1 [369] was successfully created
```
