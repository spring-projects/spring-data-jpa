package org.springframework.data.jpa.repository.config;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.EntityManagerFactory;
import javax.persistence.metamodel.ManagedType;
import javax.persistence.metamodel.Metamodel;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.config.AbstractFactoryBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;

/**
 * {@link FactoryBean} to setup {@link JpaMetamodelMappingContext} instances from Spring configuration.
 * 
 * @author Oliver Gierke
 * @since 1.6
 */
class JpaMetamodelMappingContextFactoryBean extends AbstractFactoryBean<JpaMetamodelMappingContext> implements
		ApplicationContextAware {

	private ListableBeanFactory beanFactory;

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.context.ApplicationContextAware#setApplicationContext(org.springframework.context.ApplicationContext)
	 */
	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.beanFactory = applicationContext;
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.beans.factory.config.AbstractFactoryBean#getObjectType()
	 */
	@Override
	public Class<?> getObjectType() {
		return JpaMetamodelMappingContext.class;
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.beans.factory.config.AbstractFactoryBean#createInstance()
	 */
	@Override
	protected JpaMetamodelMappingContext createInstance() throws Exception {

		Set<Metamodel> models = getMetamodels();
		Set<Class<?>> entitySources = new HashSet<Class<?>>();

		for (Metamodel metamodel : models) {

			for (ManagedType<?> type : metamodel.getManagedTypes()) {

				Class<?> javaType = type.getJavaType();

				if (javaType != null) {
					entitySources.add(javaType);
				}
			}
		}

		JpaMetamodelMappingContext context = new JpaMetamodelMappingContext(models);
		context.setInitialEntitySet(entitySources);
		context.initialize();

		return context;
	}

	/**
	 * Obtains all {@link Metamodel} instances of the current {@link ApplicationContext}.
	 * 
	 * @return
	 */
	private Set<Metamodel> getMetamodels() {

		Collection<EntityManagerFactory> factories = BeanFactoryUtils.beansOfTypeIncludingAncestors(beanFactory,
				EntityManagerFactory.class).values();
		Set<Metamodel> metamodels = new HashSet<Metamodel>(factories.size());

		for (EntityManagerFactory emf : factories) {
			metamodels.add(emf.getMetamodel());
		}

		return metamodels;
	}
}
