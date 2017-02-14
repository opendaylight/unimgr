package org.opendaylight.unimgr.mef.nrp.cisco.xe.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RunningConfig {

    private static final String IP_ADDRESS_LOOPBACK0_PATTERN = "interface Loopback0[\\n\\d]+ ip address (\\d+\\.\\d+\\.\\d+\\.\\d+) \\d+\\.\\d+\\.\\d+\\.\\d+";
    private static final String VC_VALUE_PATTERN = "xconnect \\d+\\.\\d+\\.\\d+\\.\\d+ (\\d+) encapsulation mpls";
    private static final Logger LOG = LoggerFactory.getLogger(RunningConfig.class);
    private String config;


    public RunningConfig (String config){
        this.config = config;
    }

    public Set<Integer> getUsedVcIdValues(){

        Set values = new HashSet();
        Pattern p = Pattern.compile(VC_VALUE_PATTERN);
        Matcher m = p.matcher(config);
        while(m.find()) {
            LOG.debug("found vc id value: " + m.group(1));
            values.add(Integer.parseInt(m.group(1)));
        }
        LOG.debug("values : "+values);
        return values;
    }

    public String getIpAddressLoopback0() throws IpAddressLoopbackNotFoundException {

        Pattern p = Pattern.compile(IP_ADDRESS_LOOPBACK0_PATTERN);
        Matcher m = p.matcher(config);
        if(m.find()) {
            LOG.debug("IpAddressLoopback0 : "+m.group(1));
            return m.group(1);
        }
        LOG.error("Pattern "+p.toString()+" not found in running config :\n"+config);
        throw new IpAddressLoopbackNotFoundException();
    }

    @Override
    public String toString(){
        return config;
    }
}
