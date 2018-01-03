/*
 * Copyright 2008-2018 the original author or authors.
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

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.parsing.BeanComponentDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.xml.BeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.data.auditing.config.AuditingHandlerBeanDefinitionParser;
import org.springframework.data.config.ParsingUtils;
import org.springframework.util.ClassUtils;
import org.w3c.dom.Element;

/**
 * {@link BeanDefinitionParser} for the {@code auditing} element.
 *
 * @author Oliver Gierke
 * @author Thomas Darimont
 */
public class AuditingBeanDefinitionParser implements BeanDefinitionParser {

	static final String AUDITING_ENTITY_LISTENER_CLASS_NAME = "org.springframework.data.jpa.domain.support.AuditingEntityListener";
	private static final String AUDITING_BFPP_CLASS_NAME = "org.springframework.data.jpa.domain.support.AuditingBeanFactoryPostProcessor";

	private final AuditingHandlerBeanDefinitionParser auditingHandlerParser = new AuditingHandlerBeanDefinitionParser(
			BeanDefinitionNames.JPA_MAPPING_CONTEXT_BEAN_NAME);
	private final SpringConfiguredBeanDefinitionParser springConfiguredParser = new SpringConfiguredBeanDefinitionParser();

	/*
	 * (non-Javadoc)
	 * @see org.springframework.beans.factory.xml.BeanDefinitionParser#parse(org.w3c.dom.Element, org.springframework.beans.factory.xml.ParserContext)
	 */
	public BeanDefinition parse(Element element, ParserContext parser) {

		springConfiguredParser.parse(element, parser);
		auditingHandlerParser.parse(element, parser);

		Object source = parser.getReaderContext().extractSource(element);

		BeanDefinitionBuilder builder = rootBeanDefinition(AUDITING_ENTITY_LISTENER_CLASS_NAME);
		builder.addPropertyValue("auditingHandler",
				ParsingUtils.getObjectFactoryBeanDefinition(auditingHandlerParser.getResolvedBeanName(), source));
		builder.setScope("prototype");

		registerInfrastructureBeanWithId(builder.getRawBeanDefinition(), AUDITING_ENTITY_LISTENER_CLASS_NAME, parser,
				element);

		RootBeanDefinition def = new RootBeanDefinition(AUDITING_BFPP_CLASS_NAME);
		registerInfrastructureBeanWithId(def, AUDITING_BFPP_CLASS_NAME, parser, element);

		return null;
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
	 * @see <a href="http://jira.springframework.org/browse/SPR-7340">SPR-7340</a>
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

				if (!ClassUtils.isPresent(BEAN_CONFIGURER_ASPECT_CLASS_NAME, getClass().getClassLoader())) {
					parserContext.getReaderContext().error(
							"Could not configure Spring Data JPA auditing-feature because"
									+ " spring-aspects.jar is not on the classpath!\n"
									+ "If you want to use auditing please add spring-aspects.jar to the classpath.", element);
				}

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
