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
package de.codecentric.boot.admin.server.event;

import de.codecentric.boot.admin.server.model.ApplicationId;
import de.codecentric.boot.admin.server.model.Info;

/**
 * This event gets emitted when an application is registered.
 *
 * @author Johannes Stelzer
 */
@lombok.Data
@lombok.EqualsAndHashCode(callSuper = true)
public class ClientApplicationInfoChangedEvent extends ClientApplicationEvent {
    private static final long serialVersionUID = 1L;
    private final Info info;

    public ClientApplicationInfoChangedEvent(ApplicationId application, Info info) {
        super(application, "INFO_CHANGED");
        this.info = info;
    }
}
