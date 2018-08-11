/*
 * Copyright 2012-2018 the original author or authors.
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

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.persistence.EntityManagerFactory;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactoryBean;
import org.springframework.data.repository.config.BootstrapMode;
import org.springframework.data.repository.config.DefaultRepositoryBaseClass;
import org.springframework.data.repository.query.QueryLookupStrategy;
import org.springframework.data.repository.query.QueryLookupStrategy.Key;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Annotation to enable JPA repositories. Will scan the package of the annotated configuration class for Spring Data
 * repositories by default.
 *
 * @author Oliver Gierke
 * @author Thomas Darimont
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@Import(JpaRepositoriesRegistrar.class)
public @interface EnableJpaRepositories {

	/**
	 * Alias for the {@link #basePackages()} attribute. Allows for more concise annotation declarations e.g.:
	 * {@code @EnableJpaRepositories("org.my.pkg")} instead of {@code @EnableJpaRepositories(basePackages="org.my.pkg")}.
	 */
	String[] value() default {};

	/**
	 * Base packages to scan for annotated components. {@link #value()} is an alias for (and mutually exclusive with) this
	 * attribute. Use {@link #basePackageClasses()} for a type-safe alternative to String-based package names.
	 */
	String[] basePackages() default {};

	/**
	 * Type-safe alternative to {@link #basePackages()} for specifying the packages to scan for annotated components. The
	 * package of each class specified will be scanned. Consider creating a special no-op marker class or interface in
	 * each package that serves no purpose other than being referenced by this attribute.
	 */
	Class<?>[] basePackageClasses() default {};

	/**
	 * Specifies which types are eligible for component scanning. Further narrows the set of candidate components from
	 * everything in {@link #basePackages()} to everything in the base packages that matches the given filter or filters.
	 */
	Filter[] includeFilters() default {};

	/**
	 * Specifies which types are not eligible for component scanning.
	 */
	Filter[] excludeFilters() default {};

	/**
	 * Returns the postfix to be used when looking up custom repository implementations. Defaults to {@literal Impl}. So
	 * for a repository named {@code PersonRepository} the corresponding implementation class will be looked up scanning
	 * for {@code PersonRepositoryImpl}.
	 *
	 * @return
	 */
	String repositoryImplementationPostfix() default "Impl";

	/**
	 * Configures the location of where to find the Spring Data named queries properties file. Will default to
	 * {@code META-INF/jpa-named-queries.properties}.
	 *
	 * @return
	 */
	String namedQueriesLocation() default "";

	/**
	 * Returns the key of the {@link QueryLookupStrategy} to be used for lookup queries for query methods. Defaults to
	 * {@link Key#CREATE_IF_NOT_FOUND}.
	 *
	 * @return
	 */
	Key queryLookupStrategy() default Key.CREATE_IF_NOT_FOUND;

	/**
	 * Returns the {@link FactoryBean} class to be used for each repository instance. Defaults to
	 * {@link JpaRepositoryFactoryBean}.
	 *
	 * @return
	 */
	Class<?> repositoryFactoryBeanClass() default JpaRepositoryFactoryBean.class;

	/**
	 * Configure the repository base class to be used to create repository proxies for this particular configuration.
	 *
	 * @return
	 * @since 1.9
	 */
	Class<?> repositoryBaseClass() default DefaultRepositoryBaseClass.class;

	// JPA specific configuration

	/**
	 * Configures the name of the {@link EntityManagerFactory} bean definition to be used to create repositories
	 * discovered through this annotation. Defaults to {@code entityManagerFactory}.
	 *
	 * @return
	 */
	String entityManagerFactoryRef() default "entityManagerFactory";

	/**
	 * Configures the name of the {@link PlatformTransactionManager} bean definition to be used to create repositories
	 * discovered through this annotation. Defaults to {@code transactionManager}.
	 *
	 * @return
	 */
	String transactionManagerRef() default "transactionManager";

	/**
	 * Configures whether nested repository-interfaces (e.g. defined as inner classes) should be discovered by the
	 * repositories infrastructure.
	 */
	boolean considerNestedRepositories() default false;

	/**
	 * Configures whether to enable default transactions for Spring Data JPA repositories. Defaults to {@literal true}. If
	 * disabled, repositories must be used behind a facade that's configuring transactions (e.g. using Spring's annotation
	 * driven transaction facilities) or repository methods have to be used to demarcate transactions.
	 *
	 * @return whether to enable default transactions, defaults to {@literal true}.
	 */
	boolean enableDefaultTransactions() default true;

	/**
	 * Configures when the repositories are initialized in the bootstrap lifecycle. {@link BootstrapMode#DEFAULT}
	 * (default) means eager initialization except all repository interfaces annotated with {@link Lazy},
	 * {@link BootstrapMode#LAZY} means lazy by default including injection of lazy-initialization proxies into client
	 * beans so that those can be instantiated but will only trigger the initialization upon first repository usage (i.e a
	 * method invocation on it). This means repositories can still be uninitialized when the application context has
	 * completed its bootstrap. {@link BootstrapMode#DEFERRED} is fundamentally the same as {@link BootstrapMode#LAZY},
	 * but triggers repository initialization when the application context finishes its bootstrap.
	 * 
	 * @return
	 * @since 2.1
	 */
	BootstrapMode bootstrapMode() default BootstrapMode.DEFAULT;
}
