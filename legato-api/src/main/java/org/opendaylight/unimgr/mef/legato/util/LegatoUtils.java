/*
 * Copyright (c) 2018 Xoriant Corporation and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.unimgr.mef.legato.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.ReadTransaction;
import org.opendaylight.mdsal.binding.api.WriteTransaction;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.unimgr.mef.legato.dao.EVCDao;
import org.opendaylight.yang.gen.v1.urn.mef.yang.mef.common.types.rev180321.PositiveInteger;
import org.opendaylight.yang.gen.v1.urn.mef.yang.mef.legato.services.rev171215.MefServices;
import org.opendaylight.yang.gen.v1.urn.mef.yang.mef.legato.services.rev171215.mef.services.CarrierEthernet;
import org.opendaylight.yang.gen.v1.urn.mef.yang.mef.legato.services.rev171215.mef.services.carrier.ethernet.SubscriberServices;
import org.opendaylight.yang.gen.v1.urn.mef.yang.mef.legato.services.rev171215.mef.services.carrier.ethernet.SubscriberServicesBuilder;
import org.opendaylight.yang.gen.v1.urn.mef.yang.mef.legato.services.rev171215.mef.services.carrier.ethernet.subscriber.services.Evc;
import org.opendaylight.yang.gen.v1.urn.mef.yang.mef.legato.services.rev171215.mef.services.carrier.ethernet.subscriber.services.EvcKey;
import org.opendaylight.yang.gen.v1.urn.mef.yang.mef.legato.services.rev171215.mef.services.carrier.ethernet.subscriber.services.evc.end.points.EndPoint;
import org.opendaylight.yang.gen.v1.urn.mef.yang.mef.types.rev171215.EvcIdType;
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

import com.google.common.util.concurrent.FluentFuture;

/*
 * @author santanu.de@xoriant.com
 */

public final class LegatoUtils {

    private LegatoUtils() {
        throw new IllegalStateException("Legato utils class");
    }

    private static final Logger LOG = LoggerFactory.getLogger(LegatoUtils.class);

    public static EVCDao parseNodes(Evc evc) {
        assert evc != null;
        assert evc.getEndPoints().getEndPoint() != null
                && evc.getEndPoints().getEndPoint().size() > 0;

        List<String> uniIdList = new ArrayList<String>();
        List<String> vlanIdList;
        Map<String, List<String>> uniVlanList = new HashMap<String, List<String>>();
        String vlanId;

        for (EndPoint endPoint : evc.getEndPoints().getEndPoint()) {
            vlanId = "0";
            vlanIdList = new ArrayList<String>();
            assert endPoint.getCeVlans().getCeVlan() != null;
            if (endPoint.getCeVlans().getCeVlan().size() > 0) {
                for (VlanIdType vlanIdType : endPoint.getCeVlans().getCeVlan()) {
                    vlanId = vlanIdType.getValue().toString();
                    vlanIdList.add(vlanId);
                }
            } else {
                vlanIdList.add(vlanId);
            }
            uniVlanList.put(endPoint.getUniId().getValue().toString(), vlanIdList);
            uniIdList.add(endPoint.getUniId().getValue().toString());
        }

        final EVCDao evcDao = new EVCDao();
        evcDao.setEvcId(evc.getEvcId().getValue());
        evcDao.setMaxFrameSize(
                (evc.getMaxFrameSize().getValue() != null) ? evc.getMaxFrameSize().getValue() : 0);
        evcDao.setConnectionType(
                (evc.getConnectionType().getName() != null) ? evc.getConnectionType().getName()
                        : "");
        evcDao.setSvcType(evc.getSvcType().getName());
        evcDao.setUniIdList(uniIdList);
        evcDao.setUniVlanList(uniVlanList);

        return evcDao;
    }

    public static EndPoint2 buildCreateEthConnectivityEndPointAugmentation(String vlanId) {
        return new EndPoint2Builder().setNrpCarrierEthConnectivityEndPointResource(
                buildNrpCarrierEthConnectivityEndPointResource(vlanId)).build();
    }

    public static EndPoint7 buildUpdateEthConnectivityEndPointAugmentation(String vlanId) {
        return new EndPoint7Builder().setNrpCarrierEthConnectivityEndPointResource(
                buildNrpCarrierEthConnectivityEndPointResource(vlanId)).build();
    }

    public static CreateConnectivityServiceInput1 buildCreateConServiceAugmentation(
            String maxFrameSize) {
        return new CreateConnectivityServiceInput1Builder().setNrpCarrierEthConnectivityResource(
                buildNrpCarrierEthConnectivityResource(maxFrameSize)).build();
    }

    public static UpdateConnectivityServiceInput1 buildUpdateConServiceAugmentation(
            String maxFrameSize) {
        return new UpdateConnectivityServiceInput1Builder().setNrpCarrierEthConnectivityResource(
                buildNrpCarrierEthConnectivityResource(maxFrameSize)).build();
    }

    public static NrpCarrierEthConnectivityEndPointResource buildNrpCarrierEthConnectivityEndPointResource(
            String vlanId) {

        NrpCarrierEthConnectivityEndPointResourceBuilder nrpCarrierEthConnectivityEndPointResourceBuilder =
                new NrpCarrierEthConnectivityEndPointResourceBuilder();

        CeVlanIdListAndUntagBuilder ceVlanIdListAndUntagBuilder = new CeVlanIdListAndUntagBuilder();
        List<VlanId> vlanList = new ArrayList<VlanId>();
        if (Integer.parseInt(vlanId) > 0) {
            VlanIdBuilder vlanIdBuilder =
                    new VlanIdBuilder().setVlanId(new PositiveInteger(Long.parseLong(vlanId)));
            vlanList.add(vlanIdBuilder.build());
        }
        ceVlanIdListAndUntagBuilder.setVlanId(vlanList);
        nrpCarrierEthConnectivityEndPointResourceBuilder
                .setCeVlanIdListAndUntag(ceVlanIdListAndUntagBuilder.build());

        return nrpCarrierEthConnectivityEndPointResourceBuilder.build();
    }

    public static NrpCarrierEthConnectivityResource buildNrpCarrierEthConnectivityResource(
            String maxFrameSize) {
        NrpCarrierEthConnectivityResourceBuilder nrpCarrierEthConnectivityResourceBuilder =
                new NrpCarrierEthConnectivityResourceBuilder();
        return nrpCarrierEthConnectivityResourceBuilder
                .setMaxFrameSize(new PositiveInteger(Long.parseLong(maxFrameSize))).build();
    }

    public static CreateConnectivityServiceInput buildCreateConnectivityServiceInput(EVCDao evcDao,
            String vlanId, List<EndPoint> endpoints) {

        boolean isExclusive = false;

        // if svc-type = epl, eplan or eptree then set is_exclusive flag as true
        if (evcDao.getSvcType().equalsIgnoreCase(LegatoConstants.EPL)
                || evcDao.getSvcType().equalsIgnoreCase(LegatoConstants.EPLAN)
                || evcDao.getSvcType().equalsIgnoreCase(LegatoConstants.EPTREE)) {
            isExclusive = true;
        }

        CreateConnectivityServiceInputBuilder createConnServiceInputBuilder =
                new CreateConnectivityServiceInputBuilder();

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
            case LegatoConstants.ROOTEDMULTIPOINT:
                createConnServiceInputBuilder.setConnConstraint(new ConnConstraintBuilder()
                        .setServiceLevel(LegatoConstants.BEST_EFFORT).setIsExclusive(isExclusive)
                        .setServiceType(ServiceType.ROOTEDMULTIPOINTCONNECTIVITY).build());
                break;
            default:
                break;
        }

        // if isExclusive == true i.e port bases service then set vlanId =0 to bypass given vlan tag
        if (isExclusive) {
            vlanId = "0";
        }

        // build end points
        assert endpoints != null && endpoints.size() > 0;

        createConnServiceInputBuilder.setEndPoint(buildCreateEndpoints(endpoints, LayerProtocolName.ETH, vlanId));

        createConnServiceInputBuilder.addAugmentation(CreateConnectivityServiceInput1.class,
                LegatoUtils.buildCreateConServiceAugmentation(evcDao.getMaxFrameSize().toString()));
        return createConnServiceInputBuilder.build();
    }

    public static UpdateConnectivityServiceInput buildUpdateConnectivityServiceInput(EVCDao evcDao,
            String uniStr, String uuid) {
        boolean isExclusive = false;

        // if svc-type = epl, eplan or eptree then set is_exclusive flag as true
        if (evcDao.getSvcType().equalsIgnoreCase(LegatoConstants.EPL)
                || evcDao.getSvcType().equalsIgnoreCase(LegatoConstants.EPLAN)
                || evcDao.getSvcType().equalsIgnoreCase(LegatoConstants.EPTREE)) {
            isExclusive = true;
        }

        UpdateConnectivityServiceInputBuilder updateConnServiceInputBuilder =
                new UpdateConnectivityServiceInputBuilder();

        switch (evcDao.getConnectionType().replace("-", "").toUpperCase()) {
            case LegatoConstants.POINTTOPOINT:
                updateConnServiceInputBuilder.setConnConstraint(
                        new org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307
                            .update.connectivity.service.input.ConnConstraintBuilder()
                                .setServiceLevel(LegatoConstants.BEST_EFFORT)
                                .setIsExclusive(isExclusive)
                                .setServiceType(ServiceType.POINTTOPOINTCONNECTIVITY).build());
                break;
            case LegatoConstants.MULTIPOINTTOMULTIPOINT:
                updateConnServiceInputBuilder.setConnConstraint(
                        new org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307
                            .update.connectivity.service.input.ConnConstraintBuilder()
                                .setServiceLevel(LegatoConstants.BEST_EFFORT)
                                .setIsExclusive(isExclusive)
                                .setServiceType(ServiceType.MULTIPOINTCONNECTIVITY).build());
                break;
            case LegatoConstants.ROOTEDMULTIPOINT:
                updateConnServiceInputBuilder.setConnConstraint(
                        new org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307
                            .update.connectivity.service.input.ConnConstraintBuilder()
                                .setServiceLevel(LegatoConstants.BEST_EFFORT)
                                .setIsExclusive(isExclusive)
                                .setServiceType(ServiceType.ROOTEDMULTIPOINTCONNECTIVITY).build());
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

    private static List<org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307
        .create.connectivity.service.input.EndPoint> buildCreateEndpoints(
            List<EndPoint> endpoints, LayerProtocolName layerProtocolName, String vlanId) {
        List<org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307
            .create.connectivity.service.input.EndPoint> endpointList =
                new ArrayList<org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307
                    .create.connectivity.service.input.EndPoint>();

        EndPointBuilder endPointBuilder;
        for (org.opendaylight.yang.gen.v1.urn.mef.yang.mef.legato.services.rev171215
            .mef.services.carrier.ethernet.subscriber.services.evc.end.points.EndPoint ep : endpoints) {
            ServiceInterfacePoint sipRef = new ServiceInterfacePointBuilder()
                    .setServiceInterfacePointId(new Uuid(ep.getUniId().getValue())).build();

            endPointBuilder = new EndPointBuilder()
                    .setRole(ep.getRole().getName().equalsIgnoreCase(PortRole.ROOT.getName())
                            ? PortRole.ROOT
                            : PortRole.LEAF)
                    .setLocalId("e:" + ep.getUniId().getValue()).setServiceInterfacePoint(sipRef)
                    .setDirection(PortDirection.BIDIRECTIONAL)
                    .setLayerProtocolName(layerProtocolName).addAugmentation(EndPoint2.class,
                            LegatoUtils.buildCreateEthConnectivityEndPointAugmentation(vlanId));

            endpointList.add(endPointBuilder.build());
        }
        endPointBuilder = null;

        return endpointList;
    }

    private static org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307
        .update.connectivity.service.input.EndPoint buildUpdateEndpoints(
            String uniStr, LayerProtocolName layerProtocolName) {
        org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307
            .update.connectivity.service.input.EndPointBuilder endPointBuilder =
                new org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307
                    .update.connectivity.service.input.EndPointBuilder();
        String[] uniArr;

        if (!uniStr.isEmpty()) {
            uniArr = uniStr.split("#");
            ServiceInterfacePoint sipRef = new ServiceInterfacePointBuilder()
                    .setServiceInterfacePointId(new Uuid(uniArr[0])).build();
            endPointBuilder.setRole(PortRole.SYMMETRIC).setLocalId("e:" + uniArr[0])
                    .setServiceInterfacePoint(sipRef).setDirection(PortDirection.BIDIRECTIONAL)
                    .setLayerProtocolName(layerProtocolName).addAugmentation(EndPoint7.class,
                            LegatoUtils.buildUpdateEthConnectivityEndPointAugmentation(uniArr[1]));
        }

        uniArr = null;

        return endPointBuilder.build();
    }

    public static Optional<Evc> readEvc(DataBroker dataBroker, LogicalDatastoreType store,
            InstanceIdentifier<?> evcNode) {
        final ReadTransaction read = dataBroker.newReadOnlyTransaction();
        final InstanceIdentifier<Evc> evcId = evcNode.firstIdentifierOf(Evc.class);
        try {
            final FluentFuture<Optional<Evc>> linkFuture = read.read(store, evcId);
            return linkFuture.get();
        } catch (final InterruptedException | ExecutionException e) {
            LOG.error("Unable to read node with EVC Id {}, err: {} ", evcId, e);
        }
        return Optional.empty();
    }

    public static <T extends DataObject> Optional<T> readProfile(
            DataBroker dataBroker, LogicalDatastoreType store, InstanceIdentifier<T> child, Class<T> c) {
        final ReadTransaction read = dataBroker.newReadOnlyTransaction();

        final InstanceIdentifier<T> profileId = child.firstIdentifierOf(c);
        final FluentFuture<Optional<T>> profileFuture = read.read(store, profileId);
        try {
            return profileFuture.get();
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Unable to read node ", e);
            return Optional.empty();
        }
    }

    public static Optional<?> readProfile(String string, DataBroker dataBroker, LogicalDatastoreType store,
            InstanceIdentifier<?> child) {
        final ReadTransaction read = dataBroker.newReadOnlyTransaction();
        try {

            switch (string) {
                case LegatoConstants.SLS_PROFILES:
                    final InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.mef.yang.mef.global.rev171215.mef.global.sls.profiles.Profile> profileId =
                            child.firstIdentifierOf(org.opendaylight.yang.gen.v1.urn.mef.yang.mef.global.rev171215.mef.global.sls.profiles.Profile.class);
                    final FluentFuture<Optional<org.opendaylight.yang.gen.v1.urn.mef.yang.mef.global.rev171215.mef.global.sls.profiles.Profile>> profileFuture =
                            read.read(store, profileId);
                    return profileFuture.get();

                case LegatoConstants.COS_PROFILES:
                    final InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.mef.yang.mef.global.rev171215.mef.global.cos.profiles.Profile> cosProfileId =
                            child.firstIdentifierOf(org.opendaylight.yang.gen.v1.urn.mef.yang.mef.global.rev171215.mef.global.cos.profiles.Profile.class);
                    final FluentFuture<Optional<org.opendaylight.yang.gen.v1.urn.mef.yang.mef.global.rev171215.mef.global.cos.profiles.Profile>> cosProfileFuture =
                            read.read(store, cosProfileId);
                    return cosProfileFuture.get();

                case LegatoConstants.BWP_PROFILES:
                    final InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.mef.yang.mef.global.rev171215.mef.global.bwp.flow.parameter.profiles.Profile> bwpProfileId =
                            child.firstIdentifierOf(org.opendaylight.yang.gen.v1.urn.mef.yang.mef.global.rev171215.mef.global.bwp.flow.parameter.profiles.Profile.class);
                    final FluentFuture<Optional<org.opendaylight.yang.gen.v1.urn.mef.yang.mef.global.rev171215.mef.global.bwp.flow.parameter.profiles.Profile>> bwpProfileFuture =
                            read.read(store, bwpProfileId);
                    return bwpProfileFuture.get();

                case LegatoConstants.L2CP_EEC_PROFILES:
                    final InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.mef.yang.mef.global.rev171215.mef.global.l2cp.eec.profiles.Profile> l2cpEec_ProfileId =
                            child.firstIdentifierOf(org.opendaylight.yang.gen.v1.urn.mef.yang.mef.global.rev171215.mef.global.l2cp.eec.profiles.Profile.class);
                    final FluentFuture<Optional<org.opendaylight.yang.gen.v1.urn.mef.yang.mef.global.rev171215.mef.global.l2cp.eec.profiles.Profile>> l2cpEecProfileFuture =
                            read.read(store, l2cpEec_ProfileId);
                    return l2cpEecProfileFuture.get();

                case LegatoConstants.L2CP_PEERING_PROFILES:
                    final InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.mef.yang.mef.global.rev171215.mef.global.l2cp.peering.profiles.Profile> l2cpPeering_ProfileId =
                            child.firstIdentifierOf(org.opendaylight.yang.gen.v1.urn.mef.yang.mef.global.rev171215.mef.global.l2cp.peering.profiles.Profile.class);
                    final FluentFuture<Optional<org.opendaylight.yang.gen.v1.urn.mef.yang.mef.global.rev171215.mef.global.l2cp.peering.profiles.Profile>> l2cpPeeringProfileFuture =
                            read.read(store, l2cpPeering_ProfileId);
                    return l2cpPeeringProfileFuture.get();

                case LegatoConstants.EEC_PROFILES:
                    final InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.mef.yang.mef.global.rev171215.mef.global.eec.profiles.Profile> eecProfileId =
                            child.firstIdentifierOf(org.opendaylight.yang.gen.v1.urn.mef.yang.mef.global.rev171215.mef.global.eec.profiles.Profile.class);
                    final FluentFuture<Optional<org.opendaylight.yang.gen.v1.urn.mef.yang.mef.global.rev171215.mef.global.eec.profiles.Profile>> eecProfileFuture =
                            read.read(store, eecProfileId);
                    return eecProfileFuture.get();

                case LegatoConstants.CMP_PROFILES:
                    final InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.mef.yang.mef.global.rev171215.mef.global.color.mapping.profiles.Profile> cmpProfileId =
                            child.firstIdentifierOf(org.opendaylight.yang.gen.v1.urn.mef.yang.mef.global.rev171215.mef.global.color.mapping.profiles.Profile.class);
                    final FluentFuture<Optional<org.opendaylight.yang.gen.v1.urn.mef.yang.mef.global.rev171215.mef.global.color.mapping.profiles.Profile>> cmpProfileFuture =
                            read.read(store, cmpProfileId);
                    return cmpProfileFuture.get();

                default:
                    LOG.trace("IN DEFAULT CASE :  NO MATCH");
            }
        } catch (final InterruptedException | ExecutionException e) {
            LOG.error("Unable to read node ", e);
        }
        return Optional.empty();
    }

    public static boolean deleteFromOperationalDB(InstanceIdentifier<?> nodeIdentifier,
            DataBroker dataBroker) {

        LOG.debug("Received a request to delete node {}", nodeIdentifier);
        boolean result = false;

        final WriteTransaction transaction = dataBroker.newWriteOnlyTransaction();
        transaction.delete(LogicalDatastoreType.OPERATIONAL, nodeIdentifier);

        try {
            transaction.commit().get();
            result = true;
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Unable to remove node ", nodeIdentifier, e);
        }
        return result;
    }

    public static <T extends DataObject> void addToOperationalDB(T typeOfProfile,
            InstanceIdentifier<T> profilesTx, DataBroker dataBroker) {
        LOG.trace("Received a request to add node {}", profilesTx);

        WriteTransaction transaction = dataBroker.newWriteOnlyTransaction();
        transaction.merge(LogicalDatastoreType.OPERATIONAL, profilesTx, typeOfProfile);

        try {
            transaction.commit().get();
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Unable to add node in OperationalDB(). Err: ", e);
        }

    }

    public static boolean updateEvcInOperationalDB(Evc evc,
            InstanceIdentifier<SubscriberServices> nodeIdentifier, DataBroker dataBroker) {
        LOG.trace("Received a request to add node {}", nodeIdentifier);
        boolean result = false;

        Optional<Evc> optionalEvc =
                LegatoUtils.readEvc(dataBroker, LogicalDatastoreType.OPERATIONAL,
                        InstanceIdentifier.create(MefServices.class).child(CarrierEthernet.class)
                                .child(SubscriberServices.class)
                                .child(Evc.class, new EvcKey(new EvcIdType(evc.getEvcId()))));

        List<Evc> evcList = new ArrayList<Evc>();
        evcList.add(evc);

        final WriteTransaction transaction = dataBroker.newWriteOnlyTransaction();
        // if EVC Id present in operational DB
        if (optionalEvc.isPresent()) {
            transaction.put(LogicalDatastoreType.OPERATIONAL, nodeIdentifier,
                    new SubscriberServicesBuilder().setEvc(evcList).build());
        } else {
            transaction.merge(LogicalDatastoreType.OPERATIONAL, nodeIdentifier,
                    new SubscriberServicesBuilder().setEvc(evcList).build());
        }

        try {
            transaction.commit().get();
            result = true;
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Unable to update node in OperationalDB() ", nodeIdentifier, e);
        }
        return result;

    }


    public static List<String> validateVlanTag(EVCDao evcDao) {
        List<String> vlanIdList = new ArrayList<String>();
        List<String> vlanTagList = new ArrayList<String>();

        for (String uniId : evcDao.getUniIdList()) {
            vlanTagList = evcDao.getUniVlanIdList().get(uniId);
            if (vlanIdList.size() == 0) {
                vlanIdList = vlanTagList;
            } else if (vlanIdList.size() != vlanTagList.size()) {
                LOG.error("All end points should have equal number of vlan tags");
                vlanIdList = new ArrayList<String>();
            } else if (!vlanIdList.equals(vlanTagList)) {
                LOG.error("All end points should have same vlan tags");
                vlanIdList = new ArrayList<String>();

                return vlanIdList;
            }
        }
        if (Collections.frequency(vlanIdList, "0") == vlanIdList.size()) {
            vlanIdList.clear();
        }
        return vlanIdList;
    }


    public static boolean removeFlowFromConfigDatastore(InstanceIdentifier<?> nodeIdentifier,
            DataBroker dataBroker) {

        LOG.trace("Removing EVC from CONFIGURATION datastore ", nodeIdentifier);
        boolean result = false;

        final WriteTransaction transaction = dataBroker.newWriteOnlyTransaction();
        transaction.delete(LogicalDatastoreType.CONFIGURATION, nodeIdentifier);

        try {
            transaction.commit().get();
            result = true;
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Unable to remove evc from CONFIGURATION datastore  ", nodeIdentifier, e);
        }
        return result;
    }

}
