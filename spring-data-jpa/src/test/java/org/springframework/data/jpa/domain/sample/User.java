/*
 * Copyright 2008-2025 the original author or authors.
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
package org.springframework.data.jpa.domain.sample;

import jakarta.persistence.*;

import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

/**
 * Domain class representing a person emphasizing the use of {@code AbstractEntity}. No declaration of an id is
 * required. The id is typed by the parameterizable superclass.
 *
 * @author Oliver Gierke
 * @author Thomas Darimont
 * @author Christoph Strobl
 * @author Jens Schauder
 * @author Jeff Sheets
 * @author JyotirmoyVS
 * @author Greg Turnquist
 * @author Yanming Zhou
 */
@Entity
@NamedEntityGraphs({ @NamedEntityGraph(name = "User.overview", attributeNodes = { @NamedAttributeNode("roles") }),
		@NamedEntityGraph(name = "User.detail",
				attributeNodes = { @NamedAttributeNode("roles"), @NamedAttributeNode("manager"),
						@NamedAttributeNode("colleagues") }),
		@NamedEntityGraph(name = "User.getOneWithDefinedEntityGraphById",
				attributeNodes = { @NamedAttributeNode("roles"), @NamedAttributeNode("manager"),
						@NamedAttributeNode("colleagues") }),
		@NamedEntityGraph(name = "User.withSubGraph",
				attributeNodes = { @NamedAttributeNode("roles"), @NamedAttributeNode(value = "colleagues",
						subgraph = "User.colleagues") },
				subgraphs = { @NamedSubgraph(name = "User.colleagues",
						attributeNodes = { @NamedAttributeNode("colleagues"), @NamedAttributeNode("roles") }) }),
		@NamedEntityGraph(name = "User.deepGraph",
				attributeNodes = { @NamedAttributeNode("roles"),
						@NamedAttributeNode(value = "colleagues", subgraph = "User.colleagues") },
				subgraphs = {
						@NamedSubgraph(name = "User.colleagues",
								attributeNodes = { @NamedAttributeNode("roles"),
										@NamedAttributeNode(value = "colleagues", subgraph = "User.colleaguesOfColleagues") }),
						@NamedSubgraph(name = "User.colleaguesOfColleagues",
								attributeNodes = { @NamedAttributeNode("roles"), }) }) })
@NamedQueries({ //
		@NamedQuery(name = "User.findByEmailAddress", //
				query = "SELECT u FROM User u WHERE u.emailAddress = ?1"), //
		@NamedQuery(name = "User.findByEmailAddress.count-provided", //
				query = "SELECT count(u) FROM User u WHERE u.emailAddress = ?1"), //
		@NamedQuery(name = "User.findByNamedQueryWithAliasInInvertedOrder", //
				query = "SELECT u.lastname AS lastname, u.firstname AS firstname FROM User u ORDER BY u.lastname ASC"),
		@NamedQuery(name = "User.findByNamedQueryWithConstructorExpression",
				query = "SELECT new org.springframework.data.jpa.repository.sample.NameOnlyDto(u.firstname, u.lastname) from User u") })

@NamedStoredProcedureQueries({ //
		@NamedStoredProcedureQuery(name = "User.plus1", procedureName = "plus1inout",
				parameters = { @StoredProcedureParameter(mode = ParameterMode.IN, name = "arg", type = Integer.class),
						@StoredProcedureParameter(mode = ParameterMode.OUT, name = "res", type = Integer.class) }), //
		@NamedStoredProcedureQuery(name = "User.plus1IO2", procedureName = "plus1inout2",
				parameters = { @StoredProcedureParameter(mode = ParameterMode.IN, name = "arg", type = Integer.class),
						@StoredProcedureParameter(mode = ParameterMode.OUT, name = "res", type = Integer.class),
						@StoredProcedureParameter(mode = ParameterMode.OUT, name = "res2", type = Integer.class) }), //
		@NamedStoredProcedureQuery(name = "User.plus1IOoptional", procedureName = "plus1inoutoptional",
				parameters = { @StoredProcedureParameter(mode = ParameterMode.IN, name = "arg", type = Integer.class),
						@StoredProcedureParameter(mode = ParameterMode.OUT, name = "res", type = Integer.class),
						@StoredProcedureParameter(mode = ParameterMode.OUT, name = "res2", type = Integer.class) }) // DATAJPA-1579
})
@NamedStoredProcedureQuery(name = "User.plus1IO", procedureName = "plus1inout",
		parameters = { @StoredProcedureParameter(mode = ParameterMode.IN, name = "arg", type = Integer.class),
				@StoredProcedureParameter(mode = ParameterMode.OUT, name = "res", type = Integer.class) })

// Annotations for native Query with pageable
@SqlResultSetMappings({ @SqlResultSetMapping(name = "SqlResultSetMapping.count", columns = @ColumnResult(name = "cnt")),
		@SqlResultSetMapping(name = "emailDto",
				classes = { @ConstructorResult(targetClass = User.EmailDto.class,
						columns = { @ColumnResult(name = "emailaddress", type = String.class),
								@ColumnResult(name = "secondary_email_address", type = String.class) }) }) })
@NamedNativeQueries({
		@NamedNativeQuery(name = "User.findByNativeNamedQueryWithPageable", resultClass = User.class,
				query = "SELECT * FROM SD_USER ORDER BY UCASE(firstname)"),
		@NamedNativeQuery(name = "User.findByNativeNamedQueryWithPageable.count",
				resultSetMapping = "SqlResultSetMapping.count", query = "SELECT count(*) AS cnt FROM SD_USER") })
@Table(name = "SD_User")
public class User {

	public static class EmailDto {
		private final String one;
		private final String two;

		public EmailDto(String one, String two) {
			this.one = one;
			this.two = two;
		}

		public String getOne() {
			return one;
		}

		public String getTwo() {
			return two;
		}
	}

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO) private Integer id;
	private String firstname;
	private String lastname;
	private int age;
	private boolean active;
	@Temporal(TemporalType.TIMESTAMP) private Date createdAt;

	@Column(nullable = false, unique = true) private String emailAddress;

	@Column(name = "secondary_email_address") private String secondaryEmailAddress;

	@ManyToMany(cascade = { CascadeType.PERSIST, CascadeType.MERGE }) private Set<User> colleagues;

	@ManyToMany private Set<Role> roles;

	@ManyToOne(fetch = FetchType.LAZY) private User manager;

	@Embedded private Address address;

	@Lob private byte[] binaryData;

	@ElementCollection private Set<String> attributes;

	@Temporal(TemporalType.DATE) private Date dateOfBirth;

	public User() {
		this(null, null, null);
	}

	public User(String firstname, String lastname, String emailAddress, Role... roles) {

		this.firstname = firstname;
		this.lastname = lastname;
		this.emailAddress = emailAddress;
		this.active = true;
		this.roles = new HashSet<>(Arrays.asList(roles));
		this.colleagues = new HashSet<>();
		this.attributes = new HashSet<>();
		this.createdAt = new Date();
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getFirstname() {
		return firstname;
	}

	public void setFirstname(final String firstname) {
		this.firstname = firstname;
	}

	public String getLastname() {
		return lastname;
	}

	public void setLastname(String lastname) {
		this.lastname = lastname;
	}

	public int getAge() {
		return age;
	}

	public void setAge(int age) {
		this.age = age;
	}

	public String getEmailAddress() {

		return emailAddress;
	}

	public void setEmailAddress(String emailAddress) {

		this.emailAddress = emailAddress;
	}

	public String getSecondaryEmailAddress() {
		return secondaryEmailAddress;
	}

	public void setSecondaryEmailAddress(String secondaryEmailAddress) {
		this.secondaryEmailAddress = secondaryEmailAddress;
	}

	public void setActive(boolean active) {
		this.active = active;
	}

	public boolean isActive() {
		return active;
	}

	public Set<Role> getRoles() {

		return roles;
	}

	public void addRole(Role role) {

		roles.add(role);
	}

	public void removeRole(Role role) {

		roles.remove(role);
	}

	public Set<User> getColleagues() {

		return colleagues;
	}

	public void addColleague(User colleague) {

		// Prevent from adding the user himself as colleague.
		if (this.equals(colleague)) {
			return;
		}

		colleagues.add(colleague);
		colleague.getColleagues().add(this);
	}

	public void removeColleague(User colleague) {

		colleagues.remove(colleague);
		colleague.getColleagues().remove(this);
	}

	public User getManager() {

		return manager;
	}

	public void setManager(User manager) {

		this.manager = manager;
	}

	public Date getCreatedAt() {
		return createdAt;
	}

	public Address getAddress() {
		return address;
	}

	public void setAddress(Address address) {
		this.address = address;
	}

	public void setBinaryData(byte[] binaryData) {
		this.binaryData = binaryData;
	}

	public byte[] getBinaryData() {
		return binaryData;
	}

	@Override
	public boolean equals(Object obj) {

		if (!(obj instanceof User that)) {
			return false;
		}

		if ((null == this.getId()) || (null == that.getId())) {
			return false;
		}

		return this.getId().equals(that.getId());
	}

	public Set<String> getAttributes() {
		return attributes;
	}

	public void setAttributes(Set<String> attributes) {
		this.attributes = attributes;
	}

	public Date getDateOfBirth() {
		return dateOfBirth;
	}

	public void setDateOfBirth(Date dateOfBirth) {
		this.dateOfBirth = dateOfBirth;
	}

	public void setCreatedAt(Date createdAt) {
		this.createdAt = createdAt;
	}

	@Override
	public String toString() {
		return "User: " + getId() + ", " + getFirstname() + " " + getLastname() + ", " + getEmailAddress();
	}
}
