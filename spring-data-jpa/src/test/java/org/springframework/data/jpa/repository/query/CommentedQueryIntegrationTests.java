package org.springframework.data.jpa.repository.query;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.data.jpa.domain.sample.User;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import org.springframework.data.repository.Repository;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for parsing JPQL queries containing block comments.
 *
 * @see <https://github.com/spring-projects/spring-data-jpa/issues/3997>
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration("classpath:infrastructure.xml")
@Transactional
class CommentedQueryIntegrationTests {

    @PersistenceContext
    EntityManager em;

    /**
     * Test repository interface with various commented query methods.
     */
    interface CommentedQueryRepository extends Repository<User, Integer> {

        /**
         * A query method that contains a simple inline block comment.
         */
        @Query("SELECT u FROM User u WHERE /* A simple inline comment */ u.firstname = :firstname")
        List<User> findUsersWithInlineComment(String firstname);

        /**
         * A query method that contains a multi-line block comment and another inline comment.
         */
        @Query("""
                SELECT /*
                 * This is a multi-line
                 * block comment.
                 */ u
                FROM User u
                WHERE u.lastname = :lastname /* Another inline comment */
                """)
        List<User> findUsersWithMultiLineAndInlineComments(String lastname);
    }

    @BeforeEach
    void setup() {
        // Persist a sample user for the tests
        if (em.createQuery("select count(u) from User u where u.firstname = 'Dave'", Long.class).getSingleResult() == 0) {
            User dave = new User("Dave", "Matthews", "dave@dmband.com");
            em.persist(dave);
        }
    }

    @Test
    void shouldParseQueriesWithVariousCommentStyles() {

        CommentedQueryRepository repository = new JpaRepositoryFactory(em).getRepository(CommentedQueryRepository.class);

        // --- 1. Test a query with an inline comment ---
        List<User> result1 = repository.findUsersWithInlineComment("Dave");

        assertThat(result1).hasSize(1);
        assertThat(result1.get(0).getFirstname()).isEqualTo("Dave");
        System.out.println(" Query with inline comment parsed successfully!");

        // --- 2. Test a query with multi-line and mixed comments ---
        List<User> result2 = repository.findUsersWithMultiLineAndInlineComments("Matthews");

        assertThat(result2).hasSize(1);
        assertThat(result2.get(0).getLastname()).isEqualTo("Matthews");
        System.out.println(" Query with multi-line and mixed comments parsed successfully!");
    }
}