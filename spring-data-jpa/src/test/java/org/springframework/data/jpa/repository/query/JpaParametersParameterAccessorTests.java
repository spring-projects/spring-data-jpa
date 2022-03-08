package org.springframework.data.jpa.repository.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.lang.reflect.Method;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

import org.hibernate.jpa.TypedParameterValue;
import org.hibernate.type.StandardBasicTypes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.springframework.data.jpa.domain.sample.User;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 * Unit test for {@link JpaParametersParameterAccessor}.
 *
 * @author Wonchul Heo
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration("classpath:infrastructure.xml")
class JpaParametersParameterAccessorTests {

    @PersistenceContext
    private EntityManager em;
    private Query query;

    @BeforeEach
    void setUp() {
        query = mock(Query.class);
    }

    @Test // GH-2370
    void createsJpaParametersParameterAccessor() throws Exception {

        Method withNativeQuery = SampleRepository.class.getMethod("withNativeQuery", Integer.class);
        Object[] values = { null };
        JpaParameters parameters = new JpaParameters(withNativeQuery);
        JpaParametersParameterAccessor accessor = new JpaParametersParameterAccessor(parameters, values);

        bind(parameters, accessor);

        verify(query).setParameter(eq(1), isNull());
    }

    @Test // GH-2370
    void createsHibernateParametersParameterAccessor() throws Exception {

        Method withNativeQuery = SampleRepository.class.getMethod("withNativeQuery", Integer.class);
        Object[] values = { null };
        JpaParameters parameters = new JpaParameters(withNativeQuery);
        JpaParametersParameterAccessor accessor =
                new HibernateJpaParametersParameterAccessor(parameters, values, em);

        bind(parameters, accessor);

        ArgumentCaptor<TypedParameterValue> captor = ArgumentCaptor.forClass(TypedParameterValue.class);
        verify(query).setParameter(eq(1), captor.capture());
        TypedParameterValue captorValue = captor.getValue();
        assertThat(captorValue.getType()).isEqualTo(StandardBasicTypes.INTEGER);
        assertThat(captorValue.getValue()).isNull();
    }

    private void bind(JpaParameters parameters, JpaParametersParameterAccessor accessor) {
        ParameterBinderFactory.createBinder(parameters).bind(QueryParameterSetter.BindableQuery.from(query),
                                                             accessor,
                                                             QueryParameterSetter.ErrorHandling.LENIENT);
    }

    interface SampleRepository {
        @org.springframework.data.jpa.repository.Query(
                value = "select 1 from user where age = :age",
                nativeQuery = true)
        User withNativeQuery(Integer age);
    }
}
