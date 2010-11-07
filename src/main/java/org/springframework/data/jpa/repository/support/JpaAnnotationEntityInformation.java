package org.springframework.data.jpa.repository.support;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Id;

import org.springframework.data.repository.support.IsNewAware;
import org.springframework.data.repository.support.ReflectiveEntityInformationSupport;
import org.springframework.util.Assert;


/**
 * {@link IsNewAware} implementation that reflectively checks a {@link Field} or
 * {@link Method} annotated with {@link Id}.
 * 
 * @author Oliver Gierke
 */
public class JpaAnnotationEntityInformation extends
        ReflectiveEntityInformationSupport {

    /**
     * Creates a new {@link JpaAnnotationEntityInformation} by inspecting the
     * given class for a {@link Field} or {@link Method} for and {@link Id}
     * annotation.
     * 
     * @param domainClass not {@literal null}, must be annotated with
     *            {@link Entity} and carry an anootation defining the id
     *            property.
     */
    @SuppressWarnings("unchecked")
    public JpaAnnotationEntityInformation(Class<?> domainClass) {

        super(domainClass, Id.class, EmbeddedId.class);

        Assert.isTrue(domainClass.isAnnotationPresent(Entity.class),
                "Given domain class was not annotated with @Entity!");
    }
}