/*
 * Copyright (c) 2016 Cisco Systems Inc and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.unimgr.mef.nrp.cisco.rest;

import java.net.InetAddress;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import org.opendaylight.unimgr.mef.nrp.cisco.rest.core.CiscoRestExecutor;
import org.opendaylight.unimgr.mef.nrp.cisco.rest.core.RestExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CiscoRestRunner implements CiscoExecutor{

	private static final Logger LOGGER = LoggerFactory.getLogger(CiscoRestRunner.class);

	private InetAddress destination;
	private String userName;
	private String password;

	public CiscoRestRunner(InetAddress destination, String userName, String password) {
		this.destination = destination;
		this.userName = userName;
		this.password = password;
	}

	@Override
	public String getRunningConfig() throws Exception{
		LOGGER.info("Getting running config for Cisco: " + destination);

		try(RestExecutor restExecutor = new CiscoRestExecutor(destination, userName, password)) {
			restExecutor.setUpConnection();

			WebTarget target = restExecutor.getWebTarget();
			Response response = target.path("global/running-config").request().get();
			String runningConfig = response.readEntity(String.class);

			if(response.getStatus() != 200){
				String responseString = response.readEntity(String.class);
				LOGGER.warn("Problem with getting running config for " + destination + ". Response is:\n" + responseString);
				throw new Exception("Problem with getting running config for " + destination + ". Response is:\n" + responseString);
			} else {
				LOGGER.info("Successfully got running config for Cisco " + destination);
			}

			return runningConfig;
		} catch(Exception e){
			LOGGER.error("Cannot get running config for " + destination, e);
			throw e;
		}
	}

	@Override
	public void setRunningConfig(String runningConfig) throws Exception{
		LOGGER.info("Setting running config for Cisco: " + destination);
		LOGGER.debug("Running connfig: " + runningConfig);

		Response response;

		try(RestExecutor restExecutor = new CiscoRestExecutor(destination, userName, password)) {
			restExecutor.setUpConnection();
			response = restExecutor.getWebTarget().path("global/running-config").request().put(Entity.text(runningConfig));
		} catch(Exception e){
			LOGGER.error("Cannot set running config for " + destination, e);
			throw e;
		}

		if(response.getStatus() != 200 && response.getStatus() != 204){
			//Sth went wrong on device
			String responseString = response.readEntity(String.class);
			LOGGER.warn("Problem with setting running config for " + destination + ". Commands are:\n" + runningConfig + "\nResponse is:\n" + responseString);
			throw new Exception("Problem with setting running config for " + destination + ". Commands are:\n" + runningConfig + "\nResponse is:\n" + responseString);
		} else {
			LOGGER.info("Successfully set running config for Cisco " + destination);
		}
	}

}
