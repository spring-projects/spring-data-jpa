package org.springframework.data.jpa.repository.config;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.config.AutomaticRepositoryConfigInformation;
import org.springframework.data.repository.config.ManualRepositoryConfigInformation;
import org.springframework.data.repository.config.RepositoryConfig;
import org.springframework.data.repository.config.SingleRepositoryConfigInformation;
import org.w3c.dom.Element;


/**
 * @author Oliver Gierke
 */
public class SimpleJpaRepositoryConfiguration
        extends
        RepositoryConfig<SimpleJpaRepositoryConfiguration.JpaRepositoryConfiguration, SimpleJpaRepositoryConfiguration> {

    private static final String FACTORY_CLASS =
            "org.springframework.data.jpa.repository.support.JpaRepositoryFactoryBean";
    private static final String ENTITY_MANAGER_FACTORY_REF =
            "entity-manager-factory-ref";


    /**
     * @param repositoriesElement
     */
    public SimpleJpaRepositoryConfiguration(Element repositoriesElement) {

        super(repositoriesElement, FACTORY_CLASS);
    }


    /*
     * (non-Javadoc)
     * 
     * @see
     * org.springframework.data.repository.config.GlobalRepositoryConfigInformation
     * #getRepositoryBaseInterface()
     */
    public Class<?> getRepositoryBaseInterface() {

        return JpaRepository.class;
    }


    /*
     * (non-Javadoc)
     * 
     * @see
     * org.springframework.data.repository.config.GlobalRepositoryConfigInformation
     * #getAutoconfigRepositoryInformation(java.lang.String)
     */
    public JpaRepositoryConfiguration getAutoconfigRepositoryInformation(
            String interfaceName) {

        return new AutomaticJpaRepositoryConfigInformation(interfaceName, this);
    }


    /*
     * (non-Javadoc)
     * 
     * @see
     * org.springframework.data.jpa.repository.config.RepositoryConfigContext
     * #getManualRepositoryInformation(org.w3c.dom.Element,
     * org.springframework.data
     * .jpa.repository.config.CommonRepositoryInformation)
     */
    @Override
    public JpaRepositoryConfiguration createSingleRepositoryConfigInformationFor(
            Element element) {

        return new ManualJpaRepositoryConfigInformation(element, this);
    }


    /*
     * (non-Javadoc)
     * 
     * @see org.synyx.hades.dao.config.DaoConfigContext#getEntityManagerRef()
     */
    public String getEntityManagerFactoryRef() {

        return getSource().getAttribute(ENTITY_MANAGER_FACTORY_REF);
    }

    private static class AutomaticJpaRepositoryConfigInformation
            extends
            AutomaticRepositoryConfigInformation<SimpleJpaRepositoryConfiguration>
            implements JpaRepositoryConfiguration {

        /**
         * @param interfaceName
         * @param parent
         */
        public AutomaticJpaRepositoryConfigInformation(String interfaceName,
                SimpleJpaRepositoryConfiguration parent) {

            super(interfaceName, parent);
        }


        /**
         * Returns the {@link javax.persistence.EntityManagerFactory} reference
         * to be used for all the DAO instances configured.
         * 
         * @return
         */
        public String getEntityManagerFactoryRef() {

            return getParent().getEntityManagerFactoryRef();
        }
    }

    private static class ManualJpaRepositoryConfigInformation extends
            ManualRepositoryConfigInformation<SimpleJpaRepositoryConfiguration>
            implements JpaRepositoryConfiguration {

        /**
         * @param element
         * @param parent
         */
        public ManualJpaRepositoryConfigInformation(Element element,
                SimpleJpaRepositoryConfiguration parent) {

            super(element, parent);
        }


        /*
         * (non-Javadoc)
         * 
         * @see
         * org.synyx.hades.dao.config.DaoConfigContext#getEntityManagerRef()
         */
        public String getEntityManagerFactoryRef() {

            return getAttribute(ENTITY_MANAGER_FACTORY_REF);
        }
    }

    static interface JpaRepositoryConfiguration extends
            SingleRepositoryConfigInformation<SimpleJpaRepositoryConfiguration> {

        String getEntityManagerFactoryRef();
    }
}
