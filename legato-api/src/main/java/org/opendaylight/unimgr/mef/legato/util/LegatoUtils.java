/*
 * Copyright (c) 2018 Xoriant Corporation and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.unimgr.mef.legato.util;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.unimgr.mef.legato.dao.EVCDao;
import org.opendaylight.yang.gen.v1.urn.mef.yang.mef.common.types.rev180321.PositiveInteger;
import org.opendaylight.yang.gen.v1.urn.mef.yang.mef.legato.services.rev171215.mef.services.carrier.ethernet.SubscriberServices;
import org.opendaylight.yang.gen.v1.urn.mef.yang.mef.legato.services.rev171215.mef.services.carrier.ethernet.SubscriberServicesBuilder;
import org.opendaylight.yang.gen.v1.urn.mef.yang.mef.legato.services.rev171215.mef.services.carrier.ethernet.subscriber.services.Evc;
import org.opendaylight.yang.gen.v1.urn.mef.yang.mef.legato.services.rev171215.mef.services.carrier.ethernet.subscriber.services.evc.end.points.EndPoint;
import org.opendaylight.yang.gen.v1.urn.mef.yang.mef.types.rev171215.VlanIdType;
import org.opendaylight.yang.gen.v1.urn.mef.yang.nrm.connectivity.rev180321.carrier.eth.connectivity.end.point.resource.CeVlanIdListAndUntagBuilder;
import org.opendaylight.yang.gen.v1.urn.mef.yang.nrm.connectivity.rev180321.vlan.id.list.and.untag.VlanId;
import org.opendaylight.yang.gen.v1.urn.mef.yang.nrm.connectivity.rev180321.vlan.id.list.and.untag.VlanIdBuilder;
import org.opendaylight.yang.gen.v1.urn.mef.yang.nrp._interface.rev180321.CreateConnectivityServiceInput1;
import org.opendaylight.yang.gen.v1.urn.mef.yang.nrp._interface.rev180321.CreateConnectivityServiceInput1Builder;
import org.opendaylight.yang.gen.v1.urn.mef.yang.nrp._interface.rev180321.EndPoint2;
import org.opendaylight.yang.gen.v1.urn.mef.yang.nrp._interface.rev180321.EndPoint2Builder;
import org.opendaylight.yang.gen.v1.urn.mef.yang.nrp._interface.rev180321.EndPoint7;
import org.opendaylight.yang.gen.v1.urn.mef.yang.nrp._interface.rev180321.EndPoint7Builder;
import org.opendaylight.yang.gen.v1.urn.mef.yang.nrp._interface.rev180321.UpdateConnectivityServiceInput1;
import org.opendaylight.yang.gen.v1.urn.mef.yang.nrp._interface.rev180321.UpdateConnectivityServiceInput1Builder;
import org.opendaylight.yang.gen.v1.urn.mef.yang.nrp._interface.rev180321.nrp.connectivity.service.attrs.NrpCarrierEthConnectivityResource;
import org.opendaylight.yang.gen.v1.urn.mef.yang.nrp._interface.rev180321.nrp.connectivity.service.attrs.NrpCarrierEthConnectivityResourceBuilder;
import org.opendaylight.yang.gen.v1.urn.mef.yang.nrp._interface.rev180321.nrp.connectivity.service.end.point.attrs.NrpCarrierEthConnectivityEndPointResource;
import org.opendaylight.yang.gen.v1.urn.mef.yang.nrp._interface.rev180321.nrp.connectivity.service.end.point.attrs.NrpCarrierEthConnectivityEndPointResourceBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev180307.LayerProtocolName;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev180307.PortDirection;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev180307.PortRole;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev180307.Uuid;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307.CreateConnectivityServiceInput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307.CreateConnectivityServiceInputBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307.ServiceType;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307.UpdateConnectivityServiceInput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307.UpdateConnectivityServiceInputBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307.connectivity.service.end.point.ServiceInterfacePoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307.connectivity.service.end.point.ServiceInterfacePointBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307.create.connectivity.service.input.ConnConstraintBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307.create.connectivity.service.input.EndPointBuilder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author santanu.de@xoriant.com
 */

public class LegatoUtils {

    private static final Logger LOG = LoggerFactory.getLogger(LegatoUtils.class);

    public static EVCDao parseNodes(Evc evc) {
        List<String> uniList = new ArrayList<String>();
        String vlanId;
        EVCDao evcDao = new EVCDao();

        assert evc != null;
        uniList = new ArrayList<String>();
        assert evc.getEndPoints().getEndPoint() != null && evc.getEndPoints().getEndPoint().size() > 0;
        for (EndPoint endPoint : evc.getEndPoints().getEndPoint()) {
            vlanId = "0";
            assert endPoint.getCeVlans().getCeVlan() != null;
            for (VlanIdType vlanIdType : endPoint.getCeVlans().getCeVlan()) {
                vlanId = vlanIdType.getValue().toString();
            }

            uniList.add(endPoint.getUniId().getValue().toString() + "#" + vlanId);
        }

        evcDao.setEvcId(evc.getEvcId().getValue());
        evcDao.setMaxFrameSize((evc.getMaxFrameSize().getValue() != null) ? evc.getMaxFrameSize().getValue() : 0);
        evcDao.setConnectionType((evc.getConnectionType().getName() != null) ? evc.getConnectionType().getName() : "");
        evcDao.setSvcType(evc.getSvcType().getName());
        evcDao.setUniList(uniList);
        return evcDao;
    }

    public static EndPoint2 buildCreateEthConnectivityEndPointAugmentation(String vlanId) {
        return new EndPoint2Builder()
                .setNrpCarrierEthConnectivityEndPointResource(buildNrpCarrierEthConnectivityEndPointResource(vlanId))
                .build();
    }

    public static EndPoint7 buildUpdateEthConnectivityEndPointAugmentation(String vlanId) {
        return new EndPoint7Builder()
                .setNrpCarrierEthConnectivityEndPointResource(buildNrpCarrierEthConnectivityEndPointResource(vlanId))
                .build();
    }

    public static CreateConnectivityServiceInput1 buildCreateConServiceAugmentation(String maxFrameSize) {
        return new CreateConnectivityServiceInput1Builder()
                .setNrpCarrierEthConnectivityResource(buildNrpCarrierEthConnectivityResource(maxFrameSize)).build();
    }

    public static UpdateConnectivityServiceInput1 buildUpdateConServiceAugmentation(String maxFrameSize) {
        return new UpdateConnectivityServiceInput1Builder()
                .setNrpCarrierEthConnectivityResource(buildNrpCarrierEthConnectivityResource(maxFrameSize)).build();
    }

    public static NrpCarrierEthConnectivityEndPointResource buildNrpCarrierEthConnectivityEndPointResource(
            String vlanId) {

        NrpCarrierEthConnectivityEndPointResourceBuilder nrpCarrierEthConnectivityEndPointResourceBuilder =
                new NrpCarrierEthConnectivityEndPointResourceBuilder();

        CeVlanIdListAndUntagBuilder ceVlanIdListAndUntagBuilder = new CeVlanIdListAndUntagBuilder();
        List<VlanId> vlanList = new ArrayList<>();
        if (Integer.parseInt(vlanId) > 0) {
            VlanIdBuilder vlanIdBuilder = new VlanIdBuilder().setVlanId(new PositiveInteger(Long.parseLong(vlanId)));
            vlanList.add(vlanIdBuilder.build());
        }
        ceVlanIdListAndUntagBuilder.setVlanId(vlanList);
        nrpCarrierEthConnectivityEndPointResourceBuilder.setCeVlanIdListAndUntag(ceVlanIdListAndUntagBuilder.build());

        return nrpCarrierEthConnectivityEndPointResourceBuilder.build();
    }

    public static NrpCarrierEthConnectivityResource buildNrpCarrierEthConnectivityResource(String maxFrameSize) {
        NrpCarrierEthConnectivityResourceBuilder nrpCarrierEthConnectivityResourceBuilder =
                new NrpCarrierEthConnectivityResourceBuilder();
        return nrpCarrierEthConnectivityResourceBuilder
                .setMaxFrameSize(new PositiveInteger(Long.parseLong(maxFrameSize))).build();
    }

    public static CreateConnectivityServiceInput buildCreateConnectivityServiceInput(EVCDao evcDao) {

        CreateConnectivityServiceInputBuilder createConnServiceInputBuilder =
                new CreateConnectivityServiceInputBuilder();
        List<org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307.create.connectivity.service.input.EndPoint> endpointList;
        List<String> uniList = evcDao.getUniList();
        boolean isExclusive = false;

        // if svc-type = epl/eplan then set is_exclusive flag as true
        if (evcDao.getSvcType().equalsIgnoreCase(LegatoConstants.EPL)
                || evcDao.getSvcType().equalsIgnoreCase(LegatoConstants.EPLAN)) {
            isExclusive = true;
        }

        switch (evcDao.getConnectionType().replace("-", "").toUpperCase()) {
            case LegatoConstants.POINTTOPOINT:
                createConnServiceInputBuilder.setConnConstraint(new ConnConstraintBuilder()
                        .setServiceLevel(LegatoConstants.BEST_EFFORT).setIsExclusive(isExclusive)
                        .setServiceType(ServiceType.POINTTOPOINTCONNECTIVITY).build());
                break;
            case LegatoConstants.MULTIPOINTTOMULTIPOINT:
                createConnServiceInputBuilder.setConnConstraint(new ConnConstraintBuilder()
                        .setServiceLevel(LegatoConstants.BEST_EFFORT).setIsExclusive(isExclusive)
                        .setServiceType(ServiceType.MULTIPOINTCONNECTIVITY).build());
                break;
            default:
                break;
        }


        // build end points
        assert uniList != null && uniList.size() > 0;
        endpointList = buildCreateEndpoints(uniList, LayerProtocolName.ETH);

        createConnServiceInputBuilder.setEndPoint(endpointList);

        createConnServiceInputBuilder.addAugmentation(CreateConnectivityServiceInput1.class,
                LegatoUtils.buildCreateConServiceAugmentation(evcDao.getMaxFrameSize().toString()));

        return createConnServiceInputBuilder.build();
    }

    public static UpdateConnectivityServiceInput buildUpdateConnectivityServiceInput(EVCDao evcDao,
            String uniStr, String uuid) {
        boolean isExclusive = false;

        UpdateConnectivityServiceInputBuilder updateConnServiceInputBuilder =
                new UpdateConnectivityServiceInputBuilder();

        // if svc-type = epl/eplan then set is_exclusive flag as true
        if (evcDao.getSvcType().equalsIgnoreCase(LegatoConstants.EPL)
                || evcDao.getSvcType().equalsIgnoreCase(LegatoConstants.EPLAN)) {
            isExclusive = true;
        }

        switch (evcDao.getConnectionType().replace("-", "").toUpperCase()) {
            case LegatoConstants.POINTTOPOINT:
                updateConnServiceInputBuilder
                        .setConnConstraint(new org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307.update.connectivity.service.input.ConnConstraintBuilder()
                                .setServiceLevel(LegatoConstants.BEST_EFFORT).setIsExclusive(isExclusive)
                                .setServiceType(ServiceType.POINTTOPOINTCONNECTIVITY).build());
                break;
            case LegatoConstants.MULTIPOINTTOMULTIPOINT:
                updateConnServiceInputBuilder
                        .setConnConstraint(new org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307.update.connectivity.service.input.ConnConstraintBuilder()
                                .setServiceLevel(LegatoConstants.BEST_EFFORT).setIsExclusive(isExclusive)
                                .setServiceType(ServiceType.MULTIPOINTCONNECTIVITY).build());
                break;
            default:
                break;
        }

        updateConnServiceInputBuilder
                .setEndPoint(buildUpdateEndpoints(uniStr, LayerProtocolName.ETH));
        updateConnServiceInputBuilder.addAugmentation(UpdateConnectivityServiceInput1.class,
                LegatoUtils.buildUpdateConServiceAugmentation(evcDao.getMaxFrameSize().toString()));
        updateConnServiceInputBuilder.setServiceIdOrName(uuid);

        return updateConnServiceInputBuilder.build();
    }

    private static List<org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307.create.connectivity.service.input.EndPoint> buildCreateEndpoints(
            List<String> uniList, LayerProtocolName layerProtocolName) {
        List<org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307.create.connectivity.service.input.EndPoint> endpointList =
                new ArrayList<org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307.create.connectivity.service.input.EndPoint>();

        EndPointBuilder endPointBuilder;
        String[] uniArr;

        for (String uniStr : uniList) {
            uniArr = uniStr.split("#");

            ServiceInterfacePoint sipRef = new ServiceInterfacePointBuilder()
                    .setServiceInterfacePointId(new Uuid(uniArr[0])).build();

            endPointBuilder = new EndPointBuilder().setRole(PortRole.SYMMETRIC)
                    .setLocalId("e:" + uniArr[0]).setServiceInterfacePoint(sipRef)
                    .setDirection(PortDirection.BIDIRECTIONAL)
                    .setLayerProtocolName(layerProtocolName).addAugmentation(EndPoint2.class,
                            LegatoUtils.buildCreateEthConnectivityEndPointAugmentation(uniArr[1]));

            endpointList.add(endPointBuilder.build());
        }

        endPointBuilder = null;
        uniArr = null;

        return endpointList;
    }

    private static org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307.update.connectivity.service.input.EndPoint buildUpdateEndpoints(
            String uniStr, LayerProtocolName layerProtocolName) {
        org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307.update.connectivity.service.input.EndPointBuilder endPointBuilder = new org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307.update.connectivity.service.input.EndPointBuilder();
        String[] uniArr;

        if (StringUtils.isNotBlank(uniStr)) {
            uniArr = uniStr.split("#");
            ServiceInterfacePoint sipRef = new ServiceInterfacePointBuilder()
                    .setServiceInterfacePointId(new Uuid(uniArr[0])).build();
            endPointBuilder.setRole(PortRole.SYMMETRIC).setLocalId("e:" + uniArr[0])
                    .setServiceInterfacePoint(sipRef).setDirection(PortDirection.BIDIRECTIONAL)
                    .setLayerProtocolName(layerProtocolName)
                    .addAugmentation(EndPoint7.class,
                            LegatoUtils.buildUpdateEthConnectivityEndPointAugmentation(uniArr[1]));
        }

        uniArr = null;

        return endPointBuilder.build();
    }

    public static Optional<Evc> readEvc(DataBroker dataBroker, LogicalDatastoreType store,
            InstanceIdentifier<?> evcNode) {
        final ReadTransaction read = dataBroker.newReadOnlyTransaction();
        final InstanceIdentifier<Evc> evcId = evcNode.firstIdentifierOf(Evc.class);
        final CheckedFuture<Optional<Evc>, ReadFailedException> linkFuture = read.read(store, evcId);
        try {
            return linkFuture.checkedGet();
        } catch (final ReadFailedException e) {
            LOG.error("Unable to read node with EVC Id {}, err: {} ", evcId, e);
        }
        return Optional.absent();
    }

    @SuppressWarnings("deprecation")
    public static Optional<?> readProfile(String string, DataBroker dataBroker, LogicalDatastoreType store,
            InstanceIdentifier<?> child) {
        final ReadTransaction read = dataBroker.newReadOnlyTransaction();
        try {

            switch (string) {
                case LegatoConstants.SLS_PROFILES:
                    final InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.mef.yang.mef.global.rev171215.mef.global.sls.profiles.Profile> profileId =
                            child.firstIdentifierOf(org.opendaylight.yang.gen.v1.urn.mef.yang.mef.global.rev171215.mef.global.sls.profiles.Profile.class);
                    final CheckedFuture<Optional<org.opendaylight.yang.gen.v1.urn.mef.yang.mef.global.rev171215.mef.global.sls.profiles.Profile>, ReadFailedException> profileFuture =
                            read.read(store, profileId);
                    return profileFuture.checkedGet();

                case LegatoConstants.COS_PROFILES:
                    final InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.mef.yang.mef.global.rev171215.mef.global.cos.profiles.Profile> cosProfileId =
                            child.firstIdentifierOf(org.opendaylight.yang.gen.v1.urn.mef.yang.mef.global.rev171215.mef.global.cos.profiles.Profile.class);
                    final CheckedFuture<Optional<org.opendaylight.yang.gen.v1.urn.mef.yang.mef.global.rev171215.mef.global.cos.profiles.Profile>, ReadFailedException> cosProfileFuture =
                            read.read(store, cosProfileId);
                    return cosProfileFuture.checkedGet();

                case LegatoConstants.BWP_PROFILES:
                    final InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.mef.yang.mef.global.rev171215.mef.global.bwp.flow.parameter.profiles.Profile> bwpProfileId =
                            child.firstIdentifierOf(org.opendaylight.yang.gen.v1.urn.mef.yang.mef.global.rev171215.mef.global.bwp.flow.parameter.profiles.Profile.class);
                    final CheckedFuture<Optional<org.opendaylight.yang.gen.v1.urn.mef.yang.mef.global.rev171215.mef.global.bwp.flow.parameter.profiles.Profile>, ReadFailedException> bwpProfileFuture =
                            read.read(store, bwpProfileId);
                    return bwpProfileFuture.checkedGet();

                case LegatoConstants.L2CP_EEC_PROFILES:
                    final InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.mef.yang.mef.global.rev171215.mef.global.l2cp.eec.profiles.Profile> l2cpEec_ProfileId =
                            child.firstIdentifierOf(org.opendaylight.yang.gen.v1.urn.mef.yang.mef.global.rev171215.mef.global.l2cp.eec.profiles.Profile.class);
                    final CheckedFuture<Optional<org.opendaylight.yang.gen.v1.urn.mef.yang.mef.global.rev171215.mef.global.l2cp.eec.profiles.Profile>, ReadFailedException> l2cpEecProfileFuture =
                            read.read(store, l2cpEec_ProfileId);
                    return l2cpEecProfileFuture.checkedGet();

                case LegatoConstants.L2CP_PEERING_PROFILES:
                    final InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.mef.yang.mef.global.rev171215.mef.global.l2cp.peering.profiles.Profile> l2cpPeering_ProfileId =
                            child.firstIdentifierOf(org.opendaylight.yang.gen.v1.urn.mef.yang.mef.global.rev171215.mef.global.l2cp.peering.profiles.Profile.class);
                    final CheckedFuture<Optional<org.opendaylight.yang.gen.v1.urn.mef.yang.mef.global.rev171215.mef.global.l2cp.peering.profiles.Profile>, ReadFailedException> l2cpPeeringProfileFuture =
                            read.read(store, l2cpPeering_ProfileId);
                    return l2cpPeeringProfileFuture.checkedGet();

                case LegatoConstants.EEC_PROFILES:
                    final InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.mef.yang.mef.global.rev171215.mef.global.eec.profiles.Profile> eecProfileId =
                            child.firstIdentifierOf(org.opendaylight.yang.gen.v1.urn.mef.yang.mef.global.rev171215.mef.global.eec.profiles.Profile.class);
                    final CheckedFuture<Optional<org.opendaylight.yang.gen.v1.urn.mef.yang.mef.global.rev171215.mef.global.eec.profiles.Profile>, ReadFailedException> eecProfileFuture =
                            read.read(store, eecProfileId);
                    return eecProfileFuture.checkedGet();

                case LegatoConstants.CMP_PROFILES:
                    final InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.mef.yang.mef.global.rev171215.mef.global.color.mapping.profiles.Profile> cmpProfileId =
                            child.firstIdentifierOf(org.opendaylight.yang.gen.v1.urn.mef.yang.mef.global.rev171215.mef.global.color.mapping.profiles.Profile.class);
                    final CheckedFuture<Optional<org.opendaylight.yang.gen.v1.urn.mef.yang.mef.global.rev171215.mef.global.color.mapping.profiles.Profile>, ReadFailedException> cmpProfileFuture =
                            read.read(store, cmpProfileId);
                    return cmpProfileFuture.checkedGet();

                default:
                    LOG.info("IN DEFAULT CASE :  NO MATCH");
            }
        } catch (final ReadFailedException e) {
            LOG.error("Unable to read node ", e);
        }
        return Optional.absent();
    }

    @SuppressWarnings("deprecation")
    public static boolean deleteFromOperationalDB(InstanceIdentifier<?> nodeIdentifier,
            DataBroker dataBroker) {

        LOG.info("Received a request to delete node {}", nodeIdentifier);
        boolean result = false;

        final WriteTransaction transaction = dataBroker.newWriteOnlyTransaction();
        transaction.delete(LogicalDatastoreType.OPERATIONAL, nodeIdentifier);

        try {
            transaction.submit().checkedGet();
            result = true;
        } catch (org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException e) {
            LOG.error("Unable to remove node ", nodeIdentifier, e);
        }
        return result;
    }

    @SuppressWarnings("deprecation")
    public static <T extends DataObject> void addToOperationalDB(T typeOfProfile,
            InstanceIdentifier<T> profilesTx, DataBroker dataBroker) {
        LOG.info("Received a request to add node {}", profilesTx);

        WriteTransaction transaction = dataBroker.newWriteOnlyTransaction();
        transaction.merge(LogicalDatastoreType.OPERATIONAL, profilesTx, typeOfProfile);

        try {
            transaction.submit().checkedGet();
        } catch (org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException e) {
            LOG.error("Unable to add node in OperationalDB(). Err: ", e);
        }

    }

    @SuppressWarnings("deprecation")
    public static boolean updateEvcInOperationalDB(Evc evc,
            InstanceIdentifier<SubscriberServices> nodeIdentifier, DataBroker dataBroker) {
        LOG.info("Received a request to add node {}", nodeIdentifier);

        boolean result = false;

        List<Evc> evcList = new ArrayList<Evc>();
        evcList.add(evc);

        final WriteTransaction transaction = dataBroker.newWriteOnlyTransaction();
        transaction.put(LogicalDatastoreType.OPERATIONAL, nodeIdentifier,
                new SubscriberServicesBuilder().setEvc(evcList).build());

        try {
            transaction.submit().checkedGet();
            result = true;
        } catch (org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException e) {
            LOG.error("Unable to add node in OperationalDB() ", nodeIdentifier, e);
        }
        return result;

    }

}
