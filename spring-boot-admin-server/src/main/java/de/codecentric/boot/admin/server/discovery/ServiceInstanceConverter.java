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
package de.codecentric.boot.admin.server.discovery;


import de.codecentric.boot.admin.server.domain.entities.Application;
import de.codecentric.boot.admin.server.domain.values.Registration;

import org.springframework.cloud.client.ServiceInstance;

/**
 * Converts {@link ServiceInstance}s to {@link Application}s.
 *
 * @author Johannes Edmeier
 */
public interface ServiceInstanceConverter {

    /**
     * Converts a service instance to a application to be registered.
     *
     * @param instance the service instance.
     * @return Application
     */
    Registration convert(ServiceInstance instance);
}
