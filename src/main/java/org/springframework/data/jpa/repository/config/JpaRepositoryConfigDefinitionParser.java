/*
 * Copyright 2008-2010 the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.springframework.data.jpa.repository.config;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.data.jpa.repository.config.SimpleJpaRepositoryConfiguration.JpaRepositoryConfiguration;
import org.springframework.data.repository.config.AbstractRepositoryConfigDefinitionParser;
import org.springframework.orm.jpa.support.PersistenceAnnotationBeanPostProcessor;
import org.springframework.util.StringUtils;
import org.w3c.dom.Element;


/**
 * Parser to create bean definitions for dao-config namespace. Registers bean
 * definitions for repositories as well as
 * {@code PersistenceAnnotationBeanPostProcessor} and
 * {@code PersistenceExceptionTranslationPostProcessor} to transparently inject
 * entity manager factory instance and apply exception translation.
 * <p>
 * The definition parser allows two ways of configuration. Either it looks up
 * the manually defined repository instances or scans the defined domain package
 * for candidates for repositories.
 * 
 * @author Oliver Gierke
 * @author Eberhard Wolff
 * @author Gil Markham
 */
class JpaRepositoryConfigDefinitionParser
        extends
        AbstractRepositoryConfigDefinitionParser<SimpleJpaRepositoryConfiguration, JpaRepositoryConfiguration> {

    private static final Class<?> PAB_POST_PROCESSOR =
            PersistenceAnnotationBeanPostProcessor.class;


    /*
     * (non-Javadoc)
     * 
     * @see org.springframework.data.jpa.repository.config.
     * AbstractRepositoryConfigDefinitionParser#getDaoConfigContext()
     */
    @Override
    protected SimpleJpaRepositoryConfiguration getGlobalRepositoryConfigInformation(
            Element element) {

        return new SimpleJpaRepositoryConfiguration(element);
    }


    /*
     * (non-Javadoc)
     * 
     * @see org.springframework.data.repository.config.
     * AbstractRepositoryConfigDefinitionParser
     * #postProcessBeanDefinition(org.springframework
     * .data.repository.config.SingleRepositoryConfigInformation,
     * org.springframework.beans.factory.support.BeanDefinitionBuilder,
     * java.lang.Object)
     */
    @Override
    protected void postProcessBeanDefinition(JpaRepositoryConfiguration ctx,
            BeanDefinitionBuilder builder, Object beanSource) {

        String entityManagerRef = ctx.getEntityManagerFactoryRef();

        if (StringUtils.hasText(entityManagerRef)) {
            builder.addPropertyValue(
                    "entityManager",
                    getEntityManagerBeanDefinitionFor(entityManagerRef,
                            beanSource));
        }
    }


    /**
     * Creates an anonymous factory to extract the actual
     * {@link javax.persistence.EntityManager} from the
     * {@link javax.persistence.EntityManagerFactory} bean name reference.
     * 
     * @param entityManagerFactoryBeanName
     * @param source
     * @return
     */
    private BeanDefinition getEntityManagerBeanDefinitionFor(
            String entityManagerFactoryBeanName, Object source) {

        BeanDefinitionBuilder builder =
                BeanDefinitionBuilder
                        .rootBeanDefinition("org.springframework.orm.jpa.SharedEntityManagerCreator");
        builder.setFactoryMethod("createSharedEntityManager");
        builder.addConstructorArgReference(entityManagerFactoryBeanName);

        AbstractBeanDefinition bean = builder.getRawBeanDefinition();
        bean.setSource(source);

        return bean;
    }


    /**
     * Registers an additional
     * {@link org.springframework.orm.jpa.support.PersistenceAnnotationBeanPostProcessor}
     * to trigger automatic injextion of {@link javax.persistence.EntityManager}
     * .
     * 
     * @param registry
     * @param source
     */
    @Override
    protected void registerBeansForRoot(BeanDefinitionRegistry registry,
            Object source) {

        super.registerBeansForRoot(registry, source);

        if (!hasBean(PAB_POST_PROCESSOR, registry)) {

            AbstractBeanDefinition definition =
                    BeanDefinitionBuilder
                            .rootBeanDefinition(PAB_POST_PROCESSOR)
                            .getBeanDefinition();

            registerWithSourceAndGeneratedBeanName(registry, definition, source);
        }
    }
}
