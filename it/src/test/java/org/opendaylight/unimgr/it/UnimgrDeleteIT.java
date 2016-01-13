package org.opendaylight.unimgr.it;

import static org.junit.Assert.assertNotNull;
import static org.ops4j.pax.exam.CoreOptions.maven;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.configureConsole;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.features;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.karafDistributionConfiguration;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.keepRuntimeFolder;

import java.io.File;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.ConfigurationManager;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.options.MavenArtifactUrlReference;
import org.ops4j.pax.exam.options.MavenUrlReference;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class UnimgrDeleteIT {
    private static final Logger LOG = LoggerFactory.getLogger(UnimgrDeleteIT.class);

    @Inject
    private BundleContext bundleContext;

    @Test
    public void basicInjectTest() {
        assertNotNull(bundleContext);
        LOG.info(">> BundleContext is successfully loaded!");
        System.out.println(">> BundleContext is successfully loaded!");
    }

    @Configuration
    public Option[] config() {
        MavenArtifactUrlReference karafUrl = maven()
            .groupId("org.apache.karaf")
            .artifactId("apache-karaf")
            .version(karafVersion())
            .type("zip");

        MavenUrlReference karafStandardRepo = maven()
            .groupId("org.apache.karaf.features")
            .artifactId("standard")
            .version(karafVersion())
            .classifier("features")
            .type("xml");
        return new Option[] {
            karafDistributionConfiguration()
                .frameworkUrl(karafUrl)
                .unpackDirectory(new File("target", "exam"))
                .useDeployFolder(false),
            keepRuntimeFolder(),
            configureConsole().ignoreLocalConsole(),
            features(karafStandardRepo , "scr"),
       };
    }

    public static String karafVersion() {
        ConfigurationManager cm = new ConfigurationManager();
        String karafVersion = cm.getProperty("pax.exam.karaf.version", "3.0.3");
        return karafVersion;
    }
}