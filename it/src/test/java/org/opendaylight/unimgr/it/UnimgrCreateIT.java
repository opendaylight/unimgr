/*
 * Copyright (c) 2016 Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.unimgr.it;

import static org.ops4j.pax.exam.CoreOptions.composite;
import static org.ops4j.pax.exam.CoreOptions.maven;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.editConfigurationFilePut;

import java.math.BigInteger;

import javax.inject.Inject;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opendaylight.controller.mdsal.it.base.AbstractMdsalTestBase;
import org.opendaylight.unimgr.api.IUnimgrConsoleProvider;
import org.opendaylight.unimgr.impl.UnimgrProvider;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.UniAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.UniAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.service.speed.speed.Speed10MBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unimgr.rev151012.uni.Speed;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.ConfigurationManager;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.karaf.options.LogLevelOption.LogLevel;
import org.ops4j.pax.exam.options.MavenArtifactUrlReference;
import org.ops4j.pax.exam.options.MavenUrlReference;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class UnimgrCreateIT extends AbstractMdsalTestBase {
    private String macAddress = "any";
    private String macLayer = "IEEE 802.3-2005";
    private String mode = "Full Duplex";
    private String mtuSize = "0";
    private String physicalMedium = "UNI TypeFull Duplex 2 Physical Interface";
    private Object speed = new Speed10MBuilder().build();
    private String type = "";
    private String ipAddress = "any";
    private IUnimgrConsoleProvider provider;
    private static final Logger LOG = LoggerFactory.getLogger(UnimgrCreateIT.class);

    @Inject
    private BundleContext bundleContext;

    @Configuration
    public Option[] config() {
        return super.config();
    }

    @Override
    public String getModuleName() {
        return "unimgr";
    }

    @Override
    public String getInstanceName() {
        return "unimgr-default";
    }

    @Override
    public MavenUrlReference getFeatureRepo() {
        MavenUrlReference featureRepository = maven()
                .groupId("org.opendaylight.unimgr")
                .artifactId("unimgr-features")
                .versionAsInProject()
                .classifier("features")
                .type("xml");
        return featureRepository;
    }

    @Override
    public String getFeatureName() {
        return "odl-unimgr";
    }

    @Override
    public String getKarafDistro() {
        String groupId = System.getProperty("karaf.distro.groupId", "org.apache.karaf");
        String artifactId = System.getProperty("karaf.distro.artifactId", "apache-karaf");
        String version = System.getProperty("karaf.distro.version", karafVersion());
        String type = System.getProperty("karaf.distro.type", "zip");
        MavenArtifactUrlReference karafUrl = maven()
                .groupId(groupId)
                .artifactId(artifactId)
                .version(version)
                .type(type);
        return karafUrl.getURL();
    }

    private String karafVersion() {
        ConfigurationManager cm = new ConfigurationManager();
        String karafVersion = cm.getProperty("pax.exam.karaf.version", "3.0.3");
        return karafVersion;
    }

    @Override
    public Option getLoggingOption() {
        Option option = editConfigurationFilePut(ORG_OPS4J_PAX_LOGGING_CFG,
                        logConfiguration(UnimgrCreateIT.class),
                        LogLevel.INFO.name());
        option = composite(option, super.getLoggingOption());
        return option;
    }

    @Test
    public void isBundleContextOfTestBundleInjected() {
        Assert.assertNotNull(this.bundleContext);
        LOG.info("BundleContext successfully injected");
    }

    @Test
    public void testAddUni() {
        LOG.info("Add uni");
    }

    @Test
    public void testAddEvc() {
        LOG.info("Add evc");
    }
}
