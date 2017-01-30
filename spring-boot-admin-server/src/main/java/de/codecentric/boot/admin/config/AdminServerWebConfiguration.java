/*
 * Copyright 2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.codecentric.boot.admin.config;

import java.util.List;
import java.util.Map;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.codecentric.boot.admin.event.ClientApplicationDeregisteredEvent;
import de.codecentric.boot.admin.event.ClientApplicationRegisteredEvent;
import de.codecentric.boot.admin.event.RoutesOutdatedEvent;
import de.codecentric.boot.admin.journal.ApplicationEventJournal;
import de.codecentric.boot.admin.journal.web.JournalController;
import de.codecentric.boot.admin.registry.ApplicationRegistry;
import de.codecentric.boot.admin.registry.web.RegistryController;
import de.codecentric.boot.admin.web.AdminController;
import de.codecentric.boot.admin.web.PrefixHandlerMapping;

@Configuration
public class AdminServerWebConfiguration extends WebMvcConfigurerAdapter
		implements ApplicationContextAware {
	private final ApplicationEventPublisher publisher;
	private final AdminServerProperties adminServerProperties;
	private ApplicationContext applicationContext;

	public AdminServerWebConfiguration(ApplicationEventPublisher publisher,
			AdminServerProperties adminServerProperties) {
		this.publisher = publisher;
		this.adminServerProperties = adminServerProperties;
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
	}

	@Override
	public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
		if (!hasConverter(converters, MappingJackson2HttpMessageConverter.class)) {
			ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.json()
					.applicationContext(this.applicationContext).build();
			converters.add(new MappingJackson2HttpMessageConverter(objectMapper));
		}
	}

	private boolean hasConverter(List<HttpMessageConverter<?>> converters,
			Class<? extends HttpMessageConverter<?>> clazz) {
		for (HttpMessageConverter<?> converter : converters) {
			if (clazz.isInstance(converter)) {
				return true;
			}
		}
		return false;
	}

	@Bean
	public PrefixHandlerMapping prefixHandlerMapping() {
		Map<String, Object> beans = applicationContext
				.getBeansWithAnnotation(AdminController.class);
		PrefixHandlerMapping prefixHandlerMapping = new PrefixHandlerMapping(
				beans.values().toArray(new Object[beans.size()]));
		prefixHandlerMapping.setPrefix(adminServerProperties.getContextPath());
		return prefixHandlerMapping;
	}

	@Bean
	@ConditionalOnMissingBean
	public RegistryController registryController(ApplicationRegistry applicationRegistry) {
		return new RegistryController(applicationRegistry);
	}

	@Bean
	@ConditionalOnMissingBean
	public JournalController journalController(ApplicationEventJournal applicationEventJournal) {
		return new JournalController(applicationEventJournal);
	}

	@EventListener
	public void onClientApplicationRegistered(ClientApplicationRegisteredEvent event) {
		publisher.publishEvent(new RoutesOutdatedEvent());
	}

	@EventListener
	public void onClientApplicationDeregistered(ClientApplicationDeregisteredEvent event) {
		publisher.publishEvent(new RoutesOutdatedEvent());
	}

}
