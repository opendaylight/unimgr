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

import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.client.WebTarget;

import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;

public interface TargetProvider {

    WebTarget get();

    public static class DefaultTargetProvider implements TargetProvider {
        private ClientProvider clientProvider;
        private InetAddress destination;

        public DefaultTargetProvider(ClientProvider clientProvider,  InetAddress destination) {
            this.clientProvider = clientProvider;
            this.destination = destination;
        }

        @Override
        public WebTarget get() {
            WebTarget target = clientProvider.get().target(buildUriString());
            return target;
        }

        private String buildUriString(){
            return new StringBuilder().append("https://").append(destination.getHostAddress()).append("/api/v1").toString();
        }
    }

    public static class BasicAuthTargetProvider implements TargetProvider {
        private TargetProvider targerProvider;
        private String userName;
        private String password;

        public BasicAuthTargetProvider(TargetProvider targerProvider, String userName, String password) {
            this.targerProvider = targerProvider;
            this.userName = userName;
            this.password = password;
        }

        @Override
        public WebTarget get() {
            WebTarget target = targerProvider.get();
            target.register(HttpAuthenticationFeature.basic(userName, password));
            return target;
        }
    }

    public static class XauthTargetProvider implements TargetProvider {
        private TargetProvider targerProvider;
        private String tokenId;

        public XauthTargetProvider(TargetProvider targerProvider, String tokenId) {
            this.targerProvider = targerProvider;
            this.tokenId = tokenId;
        }

        @Override
        public WebTarget get() {
            WebTarget target = targerProvider.get();
            target.register(new AuthRequestFilter(tokenId));
            return target;
        }


        private static class AuthRequestFilter implements ClientRequestFilter {

            private String tokenId;

            public AuthRequestFilter(String tokenId) {
                this.tokenId = tokenId;
            }

            @Override
            public void filter(ClientRequestContext requestContext) throws IOException {
                requestContext.getHeaders().add("X-Auth-Token", tokenId);

            }
        }
    }
}
