package org.springframework.data.jpa.repository.support;

import com.querydsl.core.types.Constant;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.FactoryExpression;
import com.querydsl.core.types.Operation;
import com.querydsl.core.types.ParamExpression;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.SubQueryExpression;
import com.querydsl.core.types.TemplateExpression;
import com.querydsl.core.types.Visitor;
import org.springframework.data.domain.Sort;

import java.util.Optional;

/**
 * A visitor for QueryDSL expressions that extracts the alias expression
 * for a specific sort property from a factory expression.
 * <p>
 * This visitor traverses the arguments of a {@link FactoryExpression} and finds
 * the expression that corresponds to the property specified in the sort order.
 * It specifically looks for ALIAS operations and returns the aliased expression.
 * </p>
 *
 * @author Kamil Krzywański
 */
public class AliasVisitor implements Visitor<Optional<Expression<?>>, Object> {

    private final Sort.Order order;

    /**
     * Creates a new {@code AliasVisitor} for the given sort order.
     *
     * @param order the sort order containing the property name to search for
     */
    public AliasVisitor(Sort.Order order) {
        this.order = order;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<Expression<?>> visit(Constant<?> constant, Object o) {
        return Optional.empty();
    }

    /**
     * Visits a factory expression and extracts the alias expression for the configured sort property.
     * <p>
     * This method searches through the factory expression's arguments for an ALIAS operation
     * that matches the property name from the sort order. When found, it returns the aliased
     * expression which can be used for sorting.
     * </p>
     *
     * @param factoryExpression the factory expression to search
     * @param o the context object (unused)
     * @return an Optional containing the expression for the property, or empty if not found
     */
    @Override
    public Optional<Expression<?>> visit(FactoryExpression<?> factoryExpression, Object o) {
        String property = order.getProperty();

        return factoryExpression.getArgs().stream()
                .filter(arg -> arg instanceof Operation<?> op
                        && op.getOperator().name().equals("ALIAS")
                        && op.getArg(0) instanceof Path
                        && ((Path<?>) op.getArg(1)).getMetadata().getName().equals(property)
                )
                .findFirst()
                .map(arg -> ((Operation<?>) arg).getArg(1));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<Expression<?>> visit(Operation<?> operation, Object o) {
        return Optional.empty();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<Expression<?>> visit(ParamExpression<?> paramExpression, Object o) {
        return Optional.empty();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<Expression<?>> visit(Path<?> path, Object o) {
        return Optional.empty();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<Expression<?>> visit(SubQueryExpression<?> subQueryExpression, Object o) {
        return Optional.empty();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<Expression<?>> visit(TemplateExpression<?> templateExpression, Object o) {
        return Optional.empty();
    }
}
