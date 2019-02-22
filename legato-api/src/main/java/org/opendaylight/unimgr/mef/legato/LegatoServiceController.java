/*
 * Copyright (c) 2018 Xoriant Corporation and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.unimgr.mef.legato;

import com.google.common.base.Optional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.unimgr.api.UnimgrDataTreeChangeListener;
import org.opendaylight.unimgr.mef.legato.dao.EVCDao;
import org.opendaylight.unimgr.mef.legato.util.LegatoConstants;
import org.opendaylight.unimgr.mef.legato.util.LegatoUtils;
import org.opendaylight.yang.gen.v1.urn.mef.yang.mef.legato.services.rev171215.MefServices;
import org.opendaylight.yang.gen.v1.urn.mef.yang.mef.legato.services.rev171215.mef.services.CarrierEthernet;
import org.opendaylight.yang.gen.v1.urn.mef.yang.mef.legato.services.rev171215.mef.services.carrier.ethernet.SubscriberServices;
import org.opendaylight.yang.gen.v1.urn.mef.yang.mef.legato.services.rev171215.mef.services.carrier.ethernet.subscriber.services.Evc;
import org.opendaylight.yang.gen.v1.urn.mef.yang.mef.legato.services.rev171215.mef.services.carrier.ethernet.subscriber.services.EvcKey;
import org.opendaylight.yang.gen.v1.urn.mef.yang.mef.types.rev171215.EvcIdType;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307.CreateConnectivityServiceInput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307.CreateConnectivityServiceOutput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307.DeleteConnectivityServiceInput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307.DeleteConnectivityServiceInputBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307.TapiConnectivityService;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307.UpdateConnectivityServiceInput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307.UpdateConnectivityServiceOutput;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author santanu.de@xoriant.com
 */

public class LegatoServiceController extends UnimgrDataTreeChangeListener<Evc> {

    public LegatoServiceController(DataBroker dataBroker) {
        super(dataBroker);
        registerListener();
    }

    private static final Logger LOG = LoggerFactory
            .getLogger(LegatoServiceController.class);

    private static final InstanceIdentifier<Evc> EVC_IID = InstanceIdentifier
            .builder(MefServices.class).child(CarrierEthernet.class)
            .child(SubscriberServices.class).child(Evc.class).build();

    private static final InstanceIdentifier<SubscriberServices> EVC_IID_OPERATIONAL = InstanceIdentifier
            .builder(MefServices.class).child(CarrierEthernet.class)
            .child(SubscriberServices.class).build();

    private ListenerRegistration<LegatoServiceController> dataTreeChangeListenerRegistration;
    private static final Map<String, List<String>> EVC_UUID_MAP_LIST = new HashMap<String, List<String>>();

    private TapiConnectivityService prestoConnectivityService;

    public void setPrestoConnectivityService(
            TapiConnectivityService prestoConnectivityService) {
        this.prestoConnectivityService = prestoConnectivityService;
    }

    public void registerListener() {
        LOG.info("Initializing LegatoServiceController:int() ");

        assert prestoConnectivityService != null;

        dataTreeChangeListenerRegistration = dataBroker
                .registerDataTreeChangeListener(new DataTreeIdentifier<Evc>(
                        LogicalDatastoreType.CONFIGURATION, EVC_IID), this);
    }

    public void close() throws Exception {
        if (dataTreeChangeListenerRegistration != null) {
            dataTreeChangeListenerRegistration.close();
        }
    }

    @Override
    public void add(DataTreeModification<Evc> newDataObject) {
        LOG.info("  Node Added  " + newDataObject.getRootNode().getIdentifier());

        Optional<Evc> optionalEvc =
                LegatoUtils.readEvc(dataBroker, LogicalDatastoreType.CONFIGURATION, newDataObject
                        .getRootPath().getRootIdentifier());

        if (optionalEvc.isPresent()) {
            addNode(optionalEvc.get());
        }
    }

    @Override
    public void remove(DataTreeModification<Evc> removedDataObject) {
        LOG.info("  Node removed  "
                + removedDataObject.getRootNode().getIdentifier());

        deleteNode(removedDataObject.getRootNode().getDataBefore());
    }

    @Override
    public void update(DataTreeModification<Evc> modifiedDataObject) {
        LOG.info("  Node modified  "
                + modifiedDataObject.getRootNode().getIdentifier());
        Optional<Evc> optionalEvc = LegatoUtils.readEvc(dataBroker,
                LogicalDatastoreType.CONFIGURATION, modifiedDataObject
                        .getRootPath().getRootIdentifier());

        if (optionalEvc.isPresent()) {
            updateNode(optionalEvc.get());
        }

    }

    private void addNode(Evc evc) {
        LOG.info(" inside addNode()");

        try {
            assert evc != null;
            createConnection(evc);

        } catch (Exception ex) {
            LOG.error(LegatoConstants.ERROR, ex);
        }

        LOG.info(" ********** END addNode() ****************** ");

    }

    private void updateNode(Evc evc) {
        LOG.info(" inside updateNode()");


        try {
            assert evc != null;
            updateConnection(evc);

        } catch (Exception ex) {
            LOG.error(LegatoConstants.ERROR, ex);
        }

        LOG.info(" ********** END updateNode() ****************** ");

    }

    private void deleteNode(Evc evc) {
        LOG.info(" inside deleteNode()");

        try {
            assert evc != null;
            deleteConnection(evc.getEvcId().getValue());
        } catch (Exception ex) {
            LOG.error(LegatoConstants.ERROR, ex);
        }

        LOG.info(" ********** END deleteNode() ****************** ");
    }

    private void createConnection(Evc evc) {
        LOG.info("inside createConnection()");

        try {
            EVCDao evcDao =  LegatoUtils.parseNodes(evc);
            LOG.info("========" + evcDao.getUniVlanIdList().toString());
            assert evcDao != null
                    && evcDao.getUniIdList() != null && evcDao.getConnectionType() != null;
            LOG.info(" connection-type :{}, svc-type :{}", evcDao.getConnectionType(), evcDao.getSvcType());

            if (!evcDao.getSvcType().equalsIgnoreCase("other")) {
                if ((evcDao.getSvcType().equalsIgnoreCase(LegatoConstants.EPL)
                        || evcDao.getSvcType().equalsIgnoreCase(LegatoConstants.EVPL))
                        && (!evcDao.getConnectionType().replace("-", "")
                                .equalsIgnoreCase(LegatoConstants.POINTTOPOINT))) {
                    LOG.info(
                            "connection-type in payload should be point-to-point when svc-type is epl/evpl");
                } else if ((evcDao.getSvcType().equalsIgnoreCase(LegatoConstants.EPLAN)
                        || evcDao.getSvcType().equalsIgnoreCase(LegatoConstants.EVPLAN))
                        && (!evcDao.getConnectionType().replace("-", "")
                                .equalsIgnoreCase(LegatoConstants.MULTIPOINTTOMULTIPOINT))) {
                    LOG.info(
                            "connection-type in payload should be multipoint-to-multipoint when svc-type is eplan/evplan");
                } else if ((evcDao.getSvcType().equalsIgnoreCase(LegatoConstants.EPTREE)
                        || evcDao.getSvcType().equalsIgnoreCase(LegatoConstants.EVPTREE))
                        && (!evcDao.getConnectionType().replace("-", "")
                                .equalsIgnoreCase(LegatoConstants.ROOTEDMULTIPOINT))) {
                    LOG.info(
                            "connection-type in payload should be rooted-multipoint when svc-type is eptree/evptree");
                } else {
                    assert evcDao.getUniVlanIdList() != null;
                    List<String> vlanIdList = LegatoUtils.validateVlanTag(evcDao);

                    if (vlanIdList.size() > 0) {
                        List<String> uuidList = new ArrayList<String>();
                        for (int i = 0; i < vlanIdList.size(); i++) {
                            if (callCreateConnectionService(
                                    LegatoUtils.buildCreateConnectivityServiceInput(evcDao, vlanIdList.get(i), evc.getEndPoints().getEndPoint()),
                                    evcDao.getEvcId(), uuidList)) {
                            } else {
                                // Safe option is to remove created connectivity services if one of them fails. 
                                deleteConnection(evcDao.getEvcId());
                                uuidList = null;
                                break;
                            }
                        }

                        if (uuidList != null && uuidList.size() > 0) {
                            EVC_UUID_MAP_LIST.put(evcDao.getEvcId(), uuidList);
                            
                            Optional<Evc> optionalEvc = LegatoUtils.readEvc(
                                    dataBroker,
                                    LogicalDatastoreType.CONFIGURATION,
                                    InstanceIdentifier
                                            .create(MefServices.class)
                                            .child(CarrierEthernet.class)
                                            .child(SubscriberServices.class)
                                            .child(Evc.class,
                                                    new EvcKey(new EvcIdType(evcDao.getEvcId()))));

                            // Add Node in OPERATIONAL DB
                            if (optionalEvc.isPresent()) {
                                LegatoUtils.updateEvcInOperationalDB(optionalEvc.get(), EVC_IID_OPERATIONAL,
                                        dataBroker);
                            }
                        }
                        LOG.info("EVC_UUID_MAP_LIST  " + EVC_UUID_MAP_LIST.toString());
                    } else {
                        if(evcDao.getSvcType().equalsIgnoreCase(LegatoConstants.EVPL) || evcDao.getSvcType().equalsIgnoreCase(LegatoConstants.EVPLAN) || 
                                evcDao.getSvcType().equalsIgnoreCase(LegatoConstants.EVPTREE)) {

                            LegatoUtils.removeFlowFromConfigDatastore(
                                        InstanceIdentifier.create(MefServices.class)
                                        .child(CarrierEthernet.class)
                                        .child(SubscriberServices.class)
                                        .child(Evc.class,
                                         new EvcKey(new EvcIdType(evcDao.getEvcId()))),
                                         dataBroker);

                            LOG.error("Service Type : " + evcDao.getSvcType() + ", EVC ID : "
                                    + evcDao.getEvcId()
                                    + " Removed successfully from configuration datastore. ");
                        }
                    }
                }
            } else {
                LOG.info("svc-type in payload should be epl, evpl, eplan, evplan, evptree");
            }

        } catch (Exception ex) {
            LOG.error("Error in createConnection(). Err: ", ex);
        }

    }

    private void updateConnection(Evc evc) {
        LOG.info("inside updateConnection()");

        try {
            EVCDao evcDao = LegatoUtils.parseNodes(evc);
            assert evcDao != null && evcDao.getUniIdList() != null
                    && evcDao.getConnectionType() != null;
            LOG.info(" connection-type :{}, svc-type :{} ", evcDao.getConnectionType(), evcDao.getSvcType());

            if (!evcDao.getSvcType().equalsIgnoreCase("other")) {
                if ((evcDao.getSvcType().equalsIgnoreCase(LegatoConstants.EPL)
                        || evcDao.getSvcType().equalsIgnoreCase(LegatoConstants.EVPL))
                        && (!evcDao.getConnectionType().replace("-", "")
                                .equalsIgnoreCase(LegatoConstants.POINTTOPOINT))) {
                    LOG.info(
                            "connection-type in payload should be point-to-point when svc-type is epl/evpl");
                } else if ((evcDao.getSvcType().equalsIgnoreCase(LegatoConstants.EPLAN)
                        || evcDao.getSvcType().equalsIgnoreCase(LegatoConstants.EVPLAN))
                        && (!evcDao.getConnectionType().replace("-", "")
                                .equalsIgnoreCase(LegatoConstants.MULTIPOINTTOMULTIPOINT))) {
                    LOG.info(
                            "connection-type in payload should be multipoint-to-multipoint when svc-type is eplan/evplan");
                } else if ((evcDao.getSvcType().equalsIgnoreCase(LegatoConstants.EPTREE)
                        || evcDao.getSvcType().equalsIgnoreCase(LegatoConstants.EVPTREE))
                        && (!evcDao.getConnectionType().replace("-", "")
                                .equalsIgnoreCase(LegatoConstants.ROOTEDMULTIPOINT))) {
                    LOG.info(
                            "connection-type in payload should be rooted-multipoint when svc-type is eptree/evptree");
                } else {
                    if (EVC_UUID_MAP_LIST.containsKey(evcDao.getEvcId())) {
                        LOG.info("Update UUID: {} of EVC Id: {} ",
                                EVC_UUID_MAP_LIST.get(evcDao.getEvcId()), evcDao.getEvcId());
                        assert evcDao.getUniVlanIdList() != null;
                        List<String> vlanIdList = LegatoUtils.validateVlanTag(evcDao);

                        LOG.info(" number of noOfVlan = " + vlanIdList.toString());
                        if (vlanIdList.size() > 0) {
                            // delete existing EVC and create service
                            deleteConnection(evcDao.getEvcId());
                            createConnection(evc);
                        }
                    } else {
                        LOG.info("UUID does not exists for EVC Id : {}", evcDao.getEvcId());
                    }
                }
            } else {
                LOG.info("svc-type in payload should be epl, evpl, eplan, evplan, eptree, evptree");
            }
        } catch (Exception ex) {

            LOG.error("Error in updateConnection(). Err: ", ex);
        }

    }

    private void deleteConnection(String evcId) {
        LOG.info(" inside deleteConnection()");
        try {

            assert EVC_UUID_MAP_LIST != null;

            if (EVC_UUID_MAP_LIST.containsKey(evcId)) {
                LOG.info("Deleting UUID: {} of EVC Id: {} ",
                        EVC_UUID_MAP_LIST.get(evcId), evcId);

                for (String uuid : EVC_UUID_MAP_LIST.get(evcId)) {
                    // on successful deletion of service, remove respective element from EVC_UUID_MAP_LIST
                    if (callDeleteConnectionService(new DeleteConnectivityServiceInputBuilder()
                            .setServiceIdOrName(uuid).build())) {
                        LOG.info("UUID {} is deleted successfully ", uuid);
                    }
                }
                EVC_UUID_MAP_LIST.remove(evcId);

                // delete EVC node from OPERATIONAL DB
                LegatoUtils.deleteFromOperationalDB(InstanceIdentifier
                        .create(MefServices.class).child(CarrierEthernet.class)
                        .child(SubscriberServices.class)
                        .child(Evc.class, new EvcKey(new EvcIdType(evcId))), dataBroker);

            } else {
                LOG.info("UUID does not exists for EVC Id : {}", evcId);
            }

        } catch (Exception ex) {
            LOG.error(LegatoConstants.ERROR, ex);
        }

        LOG.info(" ********** END deleteConnection() ****************** ");
    }


    private boolean callCreateConnectionService(
            CreateConnectivityServiceInput createConnServiceInput, String evcId, List<String> uuidList) {
        try {
            Future<RpcResult<CreateConnectivityServiceOutput>> response = this.prestoConnectivityService
                    .createConnectivityService(createConnServiceInput);

            if (response.get().isSuccessful()) {
                LOG.info("call Success = {}, response = {} ", response.get()
                        .isSuccessful(), response.get().getResult());
                LOG.info("evcId = {}, UUID = {} ", evcId, response.get()
                        .getResult().getService().getUuid().getValue());
                uuidList.add(response.get().getResult().getService()
                        .getUuid().getValue());

                return true;

            } else {
                LOG.info("call Failure = {} >> {} ", response.get()
                        .isSuccessful(), response.get().getErrors());
                return false;
            }
        } catch (Exception ex) {
            LOG.error("Error in callCreateConnectionService(). Err: ", ex);
            return false;
        }
    }

    private void callUpdateConnectionService(
            UpdateConnectivityServiceInput updateConnectivityServiceInput,
            String evcId) {
        try {
            Future<RpcResult<UpdateConnectivityServiceOutput>> response = this.prestoConnectivityService
                    .updateConnectivityService(updateConnectivityServiceInput);

            if (response.get().isSuccessful()) {
                LOG.info("call Success = {}, response = {} ", response.get()
                        .isSuccessful(), response.get().getResult());

                Optional<Evc> optionalEvc = LegatoUtils.readEvc(
                        dataBroker,
                        LogicalDatastoreType.CONFIGURATION,
                        InstanceIdentifier
                                .create(MefServices.class)
                                .child(CarrierEthernet.class)
                                .child(SubscriberServices.class)
                                .child(Evc.class,
                                        new EvcKey(new EvcIdType(evcId))));

                // update EVC Node in OPERATIONAL DB
                if (optionalEvc.isPresent()) {
                    LegatoUtils.deleteFromOperationalDB(InstanceIdentifier
                            .create(MefServices.class)
                            .child(CarrierEthernet.class)
                            .child(SubscriberServices.class)
                            .child(Evc.class, new EvcKey(new EvcIdType(evcId))), dataBroker);

                    LegatoUtils.updateEvcInOperationalDB(optionalEvc.get(), EVC_IID_OPERATIONAL,  dataBroker);
                }

            } else {
                LOG.info("call Failure = {} >> {} ", response.get()
                        .isSuccessful(), response.get().getErrors());
            }
        } catch (Exception ex) {

            LOG.error("Error in UpdateConnectivityServiceInput(). Err: ", ex);
        }
    }

    private boolean callDeleteConnectionService(
            DeleteConnectivityServiceInput deleteConnectivityServiceInput) {
        try {
            this.prestoConnectivityService
                    .deleteConnectivityService(deleteConnectivityServiceInput);
            return true;

        } catch (Exception ex) {
            LOG.error("Fail to call callDeleteConnectionService(). Err: ", ex);
            return false;
        }
    }
}
