/*
 * Copyright (c) 2016 Cisco Systems Inc and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.unimgr.mef.nrp.cisco.rest.core;

import java.io.IOException;
import java.net.InetAddress;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.opendaylight.unimgr.mef.nrp.cisco.rest.response.AuthResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

public class CiscoRestExecutor implements RestExecutor {

    private static final Logger LOGGER = LoggerFactory.getLogger(CiscoRestExecutor.class);
    private static final String AUTH_TOKEN_SERVICES = "auth/token-services";

    private AuthResponse authResponse;
    private ClientProvider clientProvider;

    private final InetAddress destination;
    private final String userName;
    private final String password;

    public CiscoRestExecutor(InetAddress destination, String userName, String password) {
        this.destination = destination;
        this.userName = userName;
        this.password = password;
    }

    @Override
    public void setUpConnection(){
        LOGGER.info("Setting up a REST connection to Cisco: " + destination);

        clientProvider = new ClientProvider.DefaultClientProvider();

        TargetProvider defaultTargetProvider = new TargetProvider.DefaultTargetProvider(clientProvider, destination);
        TargetProvider basicAuthTargetProvider = new TargetProvider.BasicAuthTargetProvider(defaultTargetProvider, userName, password);

        WebTarget target = basicAuthTargetProvider.get().path(AUTH_TOKEN_SERVICES);
        Response post = target.request(MediaType.APPLICATION_JSON_TYPE).post(Entity.text(""));

        String responseString = post.readEntity(String.class);
        authResponse = mapToObject(responseString, AuthResponse.class);

        LOGGER.info("REST Connection successfully set up for Cisco: " + destination);
    }

    @Override
    public WebTarget getWebTarget() {
        checkPreconditions();

        TargetProvider targetProvider = new TargetProvider.DefaultTargetProvider(clientProvider, destination);
        return new TargetProvider.XauthTargetProvider(targetProvider, authResponse.getTokenId()).get();
    }

    @Override
    public void close() throws IOException {
        LOGGER.info("Closing connection for Cisco: " + destination);
        if(clientProvider!=null){
            clientProvider.get().close();
        }
        clientProvider = null;
        authResponse = null;
        LOGGER.info("Successfully closed connection for Cisco: " + destination);
    }

    private <T> T mapToObject(String input, Class<T> result){
        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.readValue(input, result);
        } catch (IOException e) {
            throw new RuntimeException("Cannot map input " + input + " to class " + result, e);
        }
    }

    private void checkPreconditions(){
        if(!isConnectionSetUp()){
            LOGGER.error("Connection is not set up for " + destination);
            throw new RuntimeException("Connection is not set up for " + destination);
        }
    }

    private boolean isConnectionSetUp(){
        if(authResponse != null) {
            return true;
        }
        return false;
    }

}
