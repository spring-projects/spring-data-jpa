/*
 * Copyright 2025-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.jpa.repository.query;

import static jakarta.persistence.metamodel.Attribute.PersistentAttributeType.*;

import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.metamodel.Attribute;
import jakarta.persistence.metamodel.Bindable;
import jakarta.persistence.metamodel.ManagedType;
import jakarta.persistence.metamodel.PluralAttribute;
import jakarta.persistence.metamodel.SingularAttribute;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jspecify.annotations.Nullable;

import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.data.core.PropertyPath;
import org.springframework.util.StringUtils;

/**
 * Support class to build expression factories for JPA query creation.
 *
 * @author Mark Paluch
 * @author Seongho Eom
 * @since 4.0
 */
class ExpressionFactorySupport {

	static final Map<Attribute.PersistentAttributeType, @Nullable Class<? extends Annotation>> ASSOCIATION_TYPES;

	static {
		Map<Attribute.PersistentAttributeType, @Nullable Class<? extends Annotation>> persistentAttributeTypes = new HashMap<>();
		persistentAttributeTypes.put(ONE_TO_ONE, OneToOne.class);
		persistentAttributeTypes.put(ONE_TO_MANY, null);
		persistentAttributeTypes.put(MANY_TO_ONE, ManyToOne.class);
		persistentAttributeTypes.put(MANY_TO_MANY, null);
		persistentAttributeTypes.put(ELEMENT_COLLECTION, null);

		ASSOCIATION_TYPES = Collections.unmodifiableMap(persistentAttributeTypes);
	}

	/**
	 * Checks if this attribute requires an outer join. This is the case e.g. if it hadn't already been fetched with an
	 * inner join and if it's an optional association, and if previous paths has already required outer joins. It also
	 * ensures outer joins are used even when Hibernate defaults to inner joins (HHH-12712 and HHH-12999).
	 *
	 * @param resolver the {@link ModelPathResolver} to check for the model.
	 * @param property the property path
	 * @param isForSelection is the property navigated for the selection or ordering part of the query? if true, we need
	 *          to generate an explicit outer join in order to prevent Hibernate to use an inner join instead. see
	 *          https://hibernate.atlassian.net/browse/HHH-12999
	 * @param hasRequiredOuterJoin has a parent already required an outer join?
	 * @param isLeafProperty is leaf property
	 * @param isRelationshipId whether property path refers to relationship id
	 * @return whether an outer join is to be used for integrating this attribute in a query.
	 */
	public boolean requiresOuterJoin(ModelPathResolver resolver, PropertyPath property, boolean isForSelection,
			boolean hasRequiredOuterJoin, boolean isLeafProperty, boolean isRelationshipId) {

		Bindable<?> propertyPathModel = resolver.resolve(property);

		if (!(propertyPathModel instanceof Attribute<?, ?> attribute)) {
			return false;
		}

		// not a persistent attribute type association (@OneToOne, @ManyToOne)
		if (!ASSOCIATION_TYPES.containsKey(attribute.getPersistentAttributeType())) {
			return false;
		}

		boolean isCollection = attribute.isCollection();
		// if this path is an optional one to one attribute navigated from the not owning side we also need an
		// explicit outer join to avoid https://hibernate.atlassian.net/browse/HHH-12712
		// and https://github.com/eclipse-ee4j/jpa-api/issues/170
		boolean isInverseOptionalOneToOne = ONE_TO_ONE == attribute.getPersistentAttributeType()
				&& StringUtils.hasText(getAnnotationProperty(attribute, "mappedBy", ""));

		if ((isLeafProperty || isRelationshipId) && !isForSelection && !isCollection && !isInverseOptionalOneToOne
				&& !hasRequiredOuterJoin) {
			return false;
		}

		return hasRequiredOuterJoin || getAnnotationProperty(attribute, "optional", true);
	}

	/**
	 * Checks if this property path is referencing to relationship id.
	 *
	 * @param resolver the {@link ModelPathResolver resolver}.
	 * @param property the property path.
	 * @return whether in a query is relationship id.
	 */
	public boolean isRelationshipId(ModelPathResolver resolver, PropertyPath property) {

		if (!property.hasNext()) {
			return false;
		}

		Bindable<?> bindable = resolver.resolveNext(property);
		return bindable instanceof SingularAttribute<?, ?> sa && sa.isId();
	}

	@SuppressWarnings("unchecked")
	private static <T> T getAnnotationProperty(Attribute<?, ?> attribute, String propertyName, T defaultValue) {

		Class<? extends Annotation> associationAnnotation = ASSOCIATION_TYPES.get(attribute.getPersistentAttributeType());

		if (associationAnnotation == null) {
			return defaultValue;
		}

		Member member = attribute.getJavaMember();

		if (!(member instanceof AnnotatedElement annotatedMember)) {
			return defaultValue;
		}

		Annotation annotation = AnnotationUtils.getAnnotation(annotatedMember, associationAnnotation);
		if (annotation == null) {
			return defaultValue;
		}

		T value = (T) AnnotationUtils.getValue(annotation, propertyName);
		return value != null ? value : defaultValue;
	}

	/**
	 * Required for EclipseLink: we try to avoid using from.get as EclipseLink produces an inner join regardless of which
	 * join operation is specified next
	 *
	 * @see <a href=
	 *      "https://bugs.eclipse.org/bugs/show_bug.cgi?id=413892">https://bugs.eclipse.org/bugs/show_bug.cgi?id=413892</a>
	 * @param model
	 * @return
	 */
	static @Nullable ManagedType<?> getManagedTypeForModel(@Nullable Object model) {

		if (model instanceof ManagedType<?> managedType) {
			return managedType;
		}

		if (model instanceof PluralAttribute<?, ?, ?> pa) {
			return pa.getElementType() instanceof ManagedType<?> managedType ? managedType : null;
		}

		if (!(model instanceof SingularAttribute<?, ?> singularAttribute)) {
			return null;
		}

		return singularAttribute.getType() instanceof ManagedType<?> managedType ? managedType : null;
	}

	/**
	 * Resolve the {@link Attribute} for a parsed {@link PropertyPath} segment.
	 * <p>
	 * Resolves {@code segment} against {@code managedType} first. If no such attribute exists and {@code segment} is a
	 * Boolean {@code is}-prefixed property name, resolves the attribute that property access derives from the very same
	 * getter: {@code isActive} is mapped onto the {@code active} attribute if that attribute is read through
	 * {@code isActive()}. Names are not treated as aliases otherwise.
	 *
	 * @param managedType the type declaring the attribute, can be {@literal null}.
	 * @param segment the parsed property path segment.
	 * @return the resolved attribute or {@literal null} if the segment cannot be resolved.
	 */
	static @Nullable Attribute<?, ?> findAttribute(@Nullable ManagedType<?> managedType, String segment) {

		if (managedType == null) {
			return null;
		}

		try {
			return managedType.getAttribute(segment);
		} catch (IllegalArgumentException ex) {
			// ManagedType may be erased for some vendor if the attribute is declared as generic, or the attribute is
			// named after the JavaBeans property of a Boolean is-getter.
		}

		if (segment.length() < 3 || !segment.startsWith("is") || !Character.isUpperCase(segment.charAt(2))) {
			return null;
		}

		try {

			Attribute<?, ?> attribute = managedType.getAttribute(StringUtils.uncapitalizeAsProperty(segment.substring(2)));
			return isReadThrough(attribute, segment) ? attribute : null;
		} catch (IllegalArgumentException ex) {
			return null;
		}
	}

	/**
	 * @return {@literal true} if {@code attribute} is read through a zero-argument {@code boolean} or {@code Boolean}
	 *         getter named {@code getterName}.
	 */
	private static boolean isReadThrough(Attribute<?, ?> attribute, String getterName) {

		return attribute.getJavaMember() instanceof Method method && method.getName().equals(getterName)
				&& method.getParameterCount() == 0
				&& (method.getReturnType() == boolean.class || method.getReturnType() == Boolean.class);
	}

	/**
	 * Resolve every segment of {@code path} into its JPA metamodel attribute name, starting at {@code model}. Segments
	 * that cannot be resolved are retained as parsed.
	 *
	 * @param model the model declaring the first segment, can be {@literal null}.
	 * @param path the property path to resolve.
	 * @return the resolved attribute names, one per path segment.
	 */
	static List<String> toAttributeNames(@Nullable Object model, PropertyPath path) {

		List<String> names = new ArrayList<>();
		ManagedType<?> managedType = getManagedTypeForModel(model);

		for (PropertyPath current = path; current != null; current = current.next()) {

			Attribute<?, ?> attribute = findAttribute(managedType, current.getSegment());

			names.add(attribute != null ? attribute.getName() : current.getSegment());
			managedType = attribute != null ? getManagedTypeForModel(attribute) : null;
		}

		return names;
	}

	public interface ModelPathResolver {

		/**
		 * Resolve the {@link Bindable} for the given {@link PropertyPath}.
		 *
		 * @param propertyPath
		 * @return
		 */
		@Nullable
		Bindable<?> resolve(PropertyPath propertyPath);

		/**
		 * Resolve the next {@link Bindable} for the given {@link PropertyPath}. Requires the {@link PropertyPath#hasNext()
		 * to have a next item}.
		 *
		 * @param propertyPath
		 * @return
		 */
		@Nullable
		Bindable<?> resolveNext(PropertyPath propertyPath);

	}

}
