package org.opendaylight.unimgr.mef.nrp.ovs.util;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author bartosz.michalik@amartus.com
 */
public class MdsalUtilsExt {
    private static final Logger LOG = LoggerFactory.getLogger(MdsalUtilsExt.class);
    /**
     * Read a specific Link from a specific datastore.
     * @param dataBroker The dataBroker instance to create transactions
     * @param store The datastore type.
     * @param topologyName The topology name.
     * @return An Optional Link instance
     */
    public static final Optional<Topology> readTopology(DataBroker dataBroker,
                                                        LogicalDatastoreType store,
                                                        String topologyName) {
        final ReadTransaction read = dataBroker.newReadOnlyTransaction();
        final TopologyId topologyId = new TopologyId(topologyName);
        InstanceIdentifier<Topology> topologyInstanceId
                = InstanceIdentifier.builder(NetworkTopology.class)
                .child(Topology.class, new TopologyKey(topologyId))
                .build();
        final CheckedFuture<Optional<Topology>, ReadFailedException> topologyFuture = read.read(store, topologyInstanceId);

        try {
            return topologyFuture.checkedGet();
        } catch (final ReadFailedException e) {
            LOG.info("Unable to read topology with Iid {}", topologyInstanceId, e);
        }
        return Optional.absent();
    }
}
