/*
 * Copyright 2008-2012 the original author or authors.
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
package org.springframework.data.jpa.repository.config;

import static org.springframework.beans.factory.support.BeanDefinitionBuilder.*;

import org.springframework.aop.framework.ProxyFactoryBean;
import org.springframework.aop.target.LazyInitTargetSource;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.parsing.BeanComponentDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.xml.BeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.util.StringUtils;
import org.w3c.dom.Element;

/**
 * {@link BeanDefinitionParser} for the {@code auditing} element. Sets up an AudE
 * 
 * @author Oliver Gierke
 */
public class AuditingBeanDefinitionParser implements BeanDefinitionParser {

	static final String AUDITING_ENTITY_LISTENER_CLASS_NAME = "org.springframework.data.jpa.domain.support.AuditingEntityListener";
	private static final String AUDITING_BFPP_CLASS_NAME = "org.springframework.data.jpa.domain.support.AuditingBeanFactoryPostProcessor";

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.springframework.beans.factory.xml.BeanDefinitionParser#parse(org.
	 * w3c.dom.Element, org.springframework.beans.factory.xml.ParserContext)
	 */
	public BeanDefinition parse(Element element, ParserContext parser) {

		new SpringConfiguredBeanDefinitionParser().parse(element, parser);

		BeanDefinitionBuilder builder = rootBeanDefinition(AUDITING_ENTITY_LISTENER_CLASS_NAME);
		builder.setScope("prototype");

		String auditorAwareRef = element.getAttribute("auditor-aware-ref");

		if (StringUtils.hasText(auditorAwareRef)) {
			builder.addPropertyValue("auditorAware", createLazyInitTargetSourceBeanDefinition(auditorAwareRef));
		}

		builder.addPropertyValue("dateTimeForNow", element.getAttribute("set-dates"));

		String dateTimeProviderRef = element.getAttribute("date-time-provider-ref");
		if (StringUtils.hasText(dateTimeProviderRef)) {
			builder.addPropertyReference("dateTimeProvider", dateTimeProviderRef);
		}

		registerInfrastructureBeanWithId(builder.getRawBeanDefinition(), AUDITING_ENTITY_LISTENER_CLASS_NAME, parser,
				element);

		RootBeanDefinition def = new RootBeanDefinition();
		def.setBeanClassName(AUDITING_BFPP_CLASS_NAME);
		registerInfrastructureBeanWithId(def, AUDITING_BFPP_CLASS_NAME, parser, element);

		return null;
	}

	private BeanDefinition createLazyInitTargetSourceBeanDefinition(String auditorAwareRef) {

		BeanDefinitionBuilder targetSourceBuilder = rootBeanDefinition(LazyInitTargetSource.class);
		targetSourceBuilder.addPropertyValue("targetBeanName", auditorAwareRef);

		BeanDefinitionBuilder builder = rootBeanDefinition(ProxyFactoryBean.class);
		builder.addPropertyValue("targetSource", targetSourceBuilder.getBeanDefinition());

		return builder.getBeanDefinition();
	}

	private void registerInfrastructureBeanWithId(AbstractBeanDefinition def, String id, ParserContext context,
			Element element) {

		def.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
		def.setSource(context.extractSource(element));
		context.registerBeanComponent(new BeanComponentDefinition(def, id));
	}

	/**
	 * Copied code of SpringConfiguredBeanDefinitionParser until this class gets public.
	 * 
	 * @see http://jira.springframework.org/browse/SPR-7340
	 * @author Juergen Hoeller
	 */
	private static class SpringConfiguredBeanDefinitionParser implements BeanDefinitionParser {

		/**
		 * The bean name of the internally managed bean configurer aspect.
		 */
		private static final String BEAN_CONFIGURER_ASPECT_BEAN_NAME = "org.springframework.context.config.internalBeanConfigurerAspect";

		private static final String BEAN_CONFIGURER_ASPECT_CLASS_NAME = "org.springframework.beans.factory.aspectj.AnnotationBeanConfigurerAspect";

		public BeanDefinition parse(Element element, ParserContext parserContext) {

			if (!parserContext.getRegistry().containsBeanDefinition(BEAN_CONFIGURER_ASPECT_BEAN_NAME)) {
				RootBeanDefinition def = new RootBeanDefinition();
				def.setBeanClassName(BEAN_CONFIGURER_ASPECT_CLASS_NAME);
				def.setFactoryMethodName("aspectOf");

				def.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
				def.setSource(parserContext.extractSource(element));
				parserContext.registerBeanComponent(new BeanComponentDefinition(def, BEAN_CONFIGURER_ASPECT_BEAN_NAME));
			}
			return null;
		}
	}
}
