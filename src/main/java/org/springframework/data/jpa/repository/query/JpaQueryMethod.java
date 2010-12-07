package org.springframework.data.jpa.repository.query;

import static org.springframework.core.annotation.AnnotationUtils.*;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.QueryHint;

import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.jpa.repository.query.JpaQueryExecution.CollectionExecution;
import org.springframework.data.jpa.repository.query.JpaQueryExecution.ModifyingExecution;
import org.springframework.data.jpa.repository.query.JpaQueryExecution.PagedExecution;
import org.springframework.data.jpa.repository.query.JpaQueryExecution.SingleEntityExecution;
import org.springframework.data.repository.query.Parameters;
import org.springframework.data.repository.query.QueryMethod;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;


/**
 * JPA specific extension of {@link QueryMethod}.
 * 
 * @author Oliver Gierke
 */
public class JpaQueryMethod extends QueryMethod {

    private final QueryExtractor extractor;
    private final EntityManager em;
    private final Method method;


    /**
     * Creates a {@link JpaQueryMethod}.
     * 
     * @param method must not be {@literal null}
     * @param extractor must not be {@literal null}
     * @param em must not be {@literal null}
     */
    public JpaQueryMethod(Method method, QueryExtractor extractor,
            EntityManager em) {

        super(method);

        Assert.notNull(method, "Method must not be null!");
        Assert.notNull(extractor, "Query extractor must not be null!");
        Assert.notNull(em, "EntityManager must not be null!");

        this.method = method;
        this.extractor = extractor;
        this.em = em;

        Assert.isTrue(!(isModifyingQuery() && getParameters()
                .hasSpecialParameter()), String.format(
                "Modifying method must not contain %s!", Parameters.TYPES));

        if (getParameters().hasPageableParameter()
                && !extractor.canExtractQuery()) {
            throw new IllegalArgumentException(
                    "You cannot use Pageable as method parameter if your "
                            + "persistence provider cannot extract queries!");
        }
    }


    /**
     * Returns the {@link JpaQueryExecution}.
     * 
     * @return
     */
    public JpaQueryExecution getExecution() {

        if (isCollectionQuery()) {
            return new CollectionExecution();
        }

        if (isPageQuery()) {
            return new PagedExecution(getParameters());
        }

        if (isModifyingQuery()) {
            return getClearAutomatically() ? new ModifyingExecution(method, em)
                    : new ModifyingExecution(method, null);
        }

        return new SingleEntityExecution();
    }


    /**
     * Returns whether the finder is a modifying one.
     * 
     * @return
     */
    boolean isModifyingQuery() {

        return null != AnnotationUtils.findAnnotation(method, Modifying.class);
    }


    /**
     * Returns all {@link QueryHint}s annotated at this class. Note, that
     * {@link QueryHints}
     * 
     * @return
     */
    List<QueryHint> getHints() {

        List<QueryHint> result = new ArrayList<QueryHint>();

        QueryHints hints = getAnnotation(method, QueryHints.class);
        if (hints != null) {
            result.addAll(Arrays.asList(hints.value()));
        }

        return result;
    }


    /**
     * Returns the {@link QueryExtractor}.
     * 
     * @return
     */
    QueryExtractor getQueryExtractor() {

        return extractor;
    }


    /**
     * Returns the name of the {@link javax.persistence.NamedQuery} this method
     * belongs to.
     * 
     * @return
     */
    String getNamedQueryName() {

        return String.format("%s.%s", getDomainClass().getSimpleName(),
                method.getName());
    }


    /**
     * Returns the query string declared in a {@link Query} annotation or
     * {@literal null} if neither the annotation found nor the attribute was
     * specified.
     * 
     * @return
     */
    String getAnnotatedQuery() {

        String query = (String) AnnotationUtils.getValue(getQueryAnnotation());
        return StringUtils.hasText(query) ? query : null;
    }


    /**
     * Returns the countQuery string declared in a {@link Query} annotation or
     * {@literal null} if neither the annotation found nor the attribute was
     * specified.
     * 
     * @return
     */
    String getCountQuery() {

        String countQuery =
                (String) AnnotationUtils.getValue(getQueryAnnotation(),
                        "countQuery");
        return StringUtils.hasText(countQuery) ? countQuery : null;
    }


    /**
     * Returns the {@link Query} annotation that is applied to the method or
     * {@code null} if none available.
     * 
     * @return
     */
    private Query getQueryAnnotation() {

        return method.getAnnotation(Query.class);
    }


    /**
     * Returns whether we should clear automatically for modifying queries.
     * 
     * @return
     */
    private boolean getClearAutomatically() {

        return (Boolean) AnnotationUtils.getValue(
                method.getAnnotation(Modifying.class), "clearAutomatically");
    }
}
