unimgr
------

The unimgr project is an OpenDaylight component that implements the MEF Legato and Presto API
reference points to provide management of connectivity services across multi-vendor
devices. Currently supported devices are:

* Cisco IOS-XR devices managed via Netconf
* Openflow switches managed via OVSDB

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
340 | Active   |  80 | 0.4.0.SNAPSHOT                      | ODL :: unimgr :: unimgr-api
341 | Active   |  80 | 0.4.0.SNAPSHOT                      | ODL :: unimgr :: unimgr-cli
342 | Active   |  80 | 0.4.0.SNAPSHOT                      | ODL :: unimgr :: unimgr-impl
343 | Active   |  80 | 0.4.0.SNAPSHOT                      | unimgr-nrp-tapi-api
344 | Active   |  80 | 0.4.0.SNAPSHOT                      | ODL :: unimgr :: unimgr-presto-api
```

You can use the karaf console to monitor the OpenDaylight log messages:

```
opendaylight-user@root>log:tail
2017-11-22 17:03:09,366 | INFO  | on-dispatcher-91 | OvsdbConnectionManager           | 337 - org.opendaylight.ovsdb.southbound-impl - 1.6.0.SNAPSHOT | Connecting to 127.0.0.1:6640
2017-11-22 17:03:09,426 | INFO  | entLoopGroup-8-1 | LoggingHandler                   | 16 - io.netty.common - 4.1.8.Final | [id: 0xa15f3fde, L:/0:0:0:0:0:0:0:0:6640] RECEIVED: [id: 0x101a849a, L:/127.0.0.1:6640 - R:/127.0.0.1:51291]
2017-11-22 17:03:09,433 | INFO  | on-dispatcher-91 | OvsdbConnectionManager           | 337 - org.opendaylight.ovsdb.southbound-impl - 1.6.0.SNAPSHOT | OVSDB Connection from /127.0.0.1:6640
```
