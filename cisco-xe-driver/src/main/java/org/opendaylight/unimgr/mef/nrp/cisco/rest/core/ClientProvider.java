/*
 * Copyright (c) 2016 Cisco Systems Inc and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.unimgr.mef.nrp.cisco.rest.core;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public interface ClientProvider {

    Client get();

    public static class DefaultClientProvider implements ClientProvider {
        private static final String SECURE_PROTOCOL_INSTANCE = "TLS";

        private Client client;

        public DefaultClientProvider() {
            client = buildClient();
        }

        @Override
        public Client get() {
            return client;
        };

        public Client buildClient()  {
            return ClientBuilder.newBuilder().sslContext(SslContextProvider.INSTANCE.get()).hostnameVerifier((s1, s2) -> true).build();
        }


        static class EmptyTrustManager implements X509TrustManager {
            @Override
            public void checkClientTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {
            }
            @Override
            public void checkServerTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {
            }
            @Override
            public X509Certificate[] getAcceptedIssuers() {
                return new X509Certificate[0];
            }
        }

        enum SslContextProvider {
            INSTANCE;

            private static final Logger LOGGER = LoggerFactory.getLogger(SslContextProvider.class);

            private SSLContext sslContext = initSslContext();

            SSLContext get() {
                if(sslContext == null) {
                    throw new RuntimeException("SSL Context not initializated", new Throwable());
                }
                return sslContext;
            }

            private static SSLContext initSslContext() {
                SSLContext sslcontext = null;
                try {
                    sslcontext = SSLContext.getInstance(SECURE_PROTOCOL_INSTANCE);
                    sslcontext.init(null, new TrustManager[] { new EmptyTrustManager() }, new java.security.SecureRandom());
                    return sslcontext;
                } catch (KeyManagementException | NoSuchAlgorithmException e) {
                    LOGGER.error("Cannot initialize SSL Context", e);
                }
                return sslcontext;
            }
        }
    }
}
