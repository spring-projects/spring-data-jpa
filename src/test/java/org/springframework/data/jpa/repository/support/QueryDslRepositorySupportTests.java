package org.springframework.data.jpa.repository.support;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.data.jpa.domain.sample.QUser;
import org.springframework.data.jpa.domain.sample.User;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;


/**
 * Integration test for {@link QueryDslRepositorySupport}.
 * 
 * @author Oliver Gierke
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({ "classpath:infrastructure.xml" })
@Transactional
public class QueryDslRepositorySupportTests {

    @PersistenceContext
    EntityManager em;

    UserRepository repository;
    User dave, carter;


    @Before
    public void setup() {

        dave = new User("Dave", "Matthews", "dave@matthews.com");
        em.persist(dave);

        carter = new User("Carter", "Beauford", "carter@beauford.com");
        em.persist(carter);

        UserRepositoryImpl repository = new UserRepositoryImpl();
        repository.setEntityManager(em);
        repository.validate();

        this.repository = repository;
    }


    @Test
    public void readsUsersCorrectly() throws Exception {

        List<User> result = repository.findUsersByLastname("Matthews");
        assertThat(result.size(), is(1));
        assertThat(result.get(0), is(dave));

        result = repository.findUsersByLastname("Beauford");
        assertThat(result.size(), is(1));
        assertThat(result.get(0), is(carter));
    }


    @Test
    public void updatesUsersCorrectly() throws Exception {

        long updates = repository.updateLastnamesTo("Foo");
        assertThat(updates, is(2L));

        List<User> result = repository.findUsersByLastname("Matthews");
        assertThat(result.size(), is(0));

        result = repository.findUsersByLastname("Beauford");
        assertThat(result.size(), is(0));

        result = repository.findUsersByLastname("Foo");
        assertThat(result.size(), is(2));
        assertThat(result, hasItems(dave, carter));
    }


    @Test
    public void deletesAllWithLastnameCorrectly() throws Exception {

        long updates = repository.deleteAllWithLastname("Matthews");
        assertThat(updates, is(1L));

        List<User> result = repository.findUsersByLastname("Matthews");
        assertThat(result.size(), is(0));

        result = repository.findUsersByLastname("Beauford");
        assertThat(result.size(), is(1));
        assertThat(result.get(0), is(carter));
    }


    @Test(expected = IllegalArgumentException.class)
    public void rejectsUnsetEntityManager() throws Exception {

        UserRepositoryImpl repositoryImpl = new UserRepositoryImpl();
        repositoryImpl.validate();
    }

    private static interface UserRepository {

        List<User> findUsersByLastname(String firstname);


        long updateLastnamesTo(String lastname);


        long deleteAllWithLastname(String lastname);
    }

    private static class UserRepositoryImpl extends QueryDslRepositorySupport
            implements UserRepository {

        private static final QUser user = QUser.user;


        public List<User> findUsersByLastname(String lastname) {

            return from(user).where(user.lastname.eq(lastname)).list(user);
        }


        public long updateLastnamesTo(String lastname) {

            return update(user).set(user.lastname, lastname).execute();
        }


        public long deleteAllWithLastname(String lastname) {

            return delete(user).where(user.lastname.eq(lastname)).execute();
        }
    }
}
