/*
 * Copyright 2014-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.codecentric.boot.admin.client.registration;

import de.codecentric.boot.admin.client.config.InstanceProperties;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;
import org.springframework.boot.actuate.autoconfigure.endpoint.web.EndpointPathProvider;
import org.springframework.boot.actuate.autoconfigure.web.server.ManagementServerProperties;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.web.server.Ssl;
import org.springframework.context.event.EventListener;
import org.springframework.util.StringUtils;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Default implementation for creating the {@link Application} instance which gets registered at the
 * admin server.
 *
 * @author Johannes Edmeier
 * @author Rene Felgenträger
 */
public class DefaultApplicationFactory implements ApplicationFactory {
    private InstanceProperties instance;
    private ServerProperties server;
    private ManagementServerProperties management;
    private Integer localServerPort;
    private Integer localManagementPort;
    private EndpointPathProvider endpointPathProvider;

    public DefaultApplicationFactory(InstanceProperties instance,
                                     ManagementServerProperties management,
                                     ServerProperties server,
                                     EndpointPathProvider endpointPathProvider) {
        this.instance = instance;
        this.management = management;
        this.server = server;
        this.endpointPathProvider = endpointPathProvider;
    }

    @Override
    public Application createApplication() {
        return Application.create(getName())
                          .healthUrl(getHealthUrl())
                          .managementUrl(getManagementUrl())
                          .serviceUrl(getServiceUrl())
                          .metadata(getMetadata())
                          .build();
    }

    protected String getName() {
        return instance.getName();
    }

    protected String getServiceUrl() {
        if (instance.getServiceUrl() != null) {
            return UriComponentsBuilder.fromUriString(instance.getServiceUrl()).toUriString();
        }

        String baseUrl = instance.getServiceBaseUrl();
        if (getLocalServerPort() == null && StringUtils.isEmpty(baseUrl)) {
            throw new IllegalStateException("service-base-url must be set when deployed to servlet-container");
        }

        UriComponentsBuilder builder;
        if (!StringUtils.isEmpty(baseUrl)) {
            builder = UriComponentsBuilder.fromUriString(baseUrl);
        } else {
            builder = UriComponentsBuilder.newInstance()
                                          .scheme(getScheme(server.getSsl()))
                                          .host(getServiceHost())
                                          .port(getLocalServerPort());
        }

        return builder.path("/").path(getServerContextPath()).toUriString();
    }

    protected String getServerContextPath() {
        return "";
    }

    protected String getManagementUrl() {
        if (instance.getManagementUrl() != null) {
            return instance.getManagementUrl();
        }

        return UriComponentsBuilder.fromUriString(getManagementBaseUrl())
                                   .path("/")
                                   .path(getManagementContextPath())
                                   .toUriString();
    }

    protected String getManagementBaseUrl() {
        String baseUrl = instance.getManagementBaseUrl();

        if (!StringUtils.isEmpty(baseUrl)) {
            return baseUrl;
        }

        if (isManagementPortEqual()) {
            return UriComponentsBuilder.fromHttpUrl(getServiceUrl())
                                       .path("/")
                                       .path(getDispatcherServletPrefix())
                                       .toUriString();
        }

        Ssl ssl = management.getSsl() != null ? management.getSsl() : server.getSsl();
        return UriComponentsBuilder.newInstance()
                                   .scheme(getScheme(ssl))
                                   .host(getManagementHost())
                                   .port(getLocalManagementPort())
                                   .toUriString();
    }

    protected String getDispatcherServletPrefix() {
        return "";
    }

    protected boolean isManagementPortEqual() {
        return getLocalManagementPort() == null || getLocalManagementPort().equals(getLocalServerPort());
    }

    protected String getManagementContextPath() {
        return management.getContextPath();
    }

    protected String getHealthUrl() {
        if (instance.getHealthUrl() != null) {
            return instance.getHealthUrl();
        }
        return UriComponentsBuilder.fromHttpUrl(getManagementBaseUrl())
                                   .path("/")
                                   .path(getHealthEndpointPath())
                                   .toUriString();
    }

    protected Map<String, String> getMetadata() {
        return instance.getMetadata();
    }

    protected String getServiceHost() {
        InetAddress address = server.getAddress();
        if (address == null) {
            address = getLocalHost();
        }
        return getHost(address);
    }

    protected String getManagementHost() {
        InetAddress address = management.getAddress();
        if (address != null) {
            return getHost(address);
        }
        return getServiceHost();
    }

    protected InetAddress getLocalHost() {
        try {
            return InetAddress.getLocalHost();
        } catch (UnknownHostException ex) {
            throw new IllegalArgumentException(ex.getMessage(), ex);
        }
    }

    protected Integer getLocalServerPort() {
        return localServerPort;
    }

    protected Integer getLocalManagementPort() {
        return localManagementPort;
    }

    protected String getHealthEndpointPath() {
        String health = endpointPathProvider.getPath("health");
        if (StringUtils.hasText(health)) {
            return health;
        }
        String status = endpointPathProvider.getPath("status");
        if (StringUtils.hasText(status)) {
            return status;
        }
        throw new IllegalStateException("Either health or status endpoint must be enabled!");
    }

    protected String getScheme(Ssl ssl) {
        return ssl != null && ssl.isEnabled() ? "https" : "http";
    }

    protected String getHost(InetAddress address) {
        return instance.isPreferIp() ? address.getHostAddress() : address.getCanonicalHostName();
    }

    @EventListener
    public void onApplicationReady(ApplicationReadyEvent event) {
        if (event.getApplicationContext() instanceof WebApplicationContext) {
            localServerPort = event.getApplicationContext()
                                   .getEnvironment()
                                   .getProperty("local.server.port", Integer.class);
            localManagementPort = event.getApplicationContext()
                                       .getEnvironment()
                                       .getProperty("local.management.port", Integer.class, localServerPort);
        }
    }
}
