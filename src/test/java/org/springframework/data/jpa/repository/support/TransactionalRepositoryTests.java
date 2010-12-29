package org.springframework.data.jpa.repository.support;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.sample.User;
import org.springframework.data.jpa.repository.sample.UserRepository;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionStatus;


/**
 * Integration test for transactional behaviour of repository operations.
 * 
 * @author Oliver Gierke
 */
@ContextConfiguration({ "classpath:config/namespace-autoconfig-context.xml",
        "classpath:tx-manager.xml" })
public class TransactionalRepositoryTests extends
        AbstractJUnit4SpringContextTests {

    @Autowired
    UserRepository repository;

    @Autowired
    DelegatingTransactionManager transactionManager;


    @Before
    public void setUp() {

        transactionManager.resetCount();
    }


    @After
    public void tearDown() {

        repository.deleteAll();
    }


    @Test
    public void simpleManipulatingOperation() throws Exception {

        repository.saveAndFlush(new User("foo", "bar", "foo@bar.de"));
        assertThat(transactionManager.getTransactionRequests(), is(1));
    }


    @Test
    public void unannotatedFinder() throws Exception {

        repository.findByEmailAddress("foo@bar.de");
        assertThat(transactionManager.getTransactionRequests(), is(0));
    }


    @Test
    public void invokeTransactionalFinder() throws Exception {

        repository.findByAnnotatedQuery("foo@bar.de");
        assertThat(transactionManager.getTransactionRequests(), is(1));
    }


    @Test
    public void invokeRedeclaredMethod() throws Exception {

        repository.findById(1);
        assertFalse(transactionManager.getDefinition().isReadOnly());
    }

    public static class DelegatingTransactionManager implements
            PlatformTransactionManager {

        private PlatformTransactionManager txManager;
        private int transactionRequests;
        private TransactionDefinition definition;


        public DelegatingTransactionManager(PlatformTransactionManager txManager) {

            this.txManager = txManager;
        }


        public void commit(TransactionStatus status)
                throws TransactionException {

            txManager.commit(status);
        }


        public TransactionStatus getTransaction(TransactionDefinition definition)
                throws TransactionException {

            this.transactionRequests++;
            this.definition = definition;

            return txManager.getTransaction(definition);
        }


        public int getTransactionRequests() {

            return transactionRequests;
        }


        public TransactionDefinition getDefinition() {

            return definition;
        }


        public void resetCount() {

            this.transactionRequests = 0;
            this.definition = null;
        }


        public void rollback(TransactionStatus status)
                throws TransactionException {

            txManager.rollback(status);
        }
    }
}
