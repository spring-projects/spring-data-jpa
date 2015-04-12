package org.springframework.data.jpa.domain.sample;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

@Entity
public class Category {

	@Id @GeneratedValue private Long id;

	@ManyToOne(fetch = FetchType.LAZY)//
	private Product product;

	public Category(Product product) {
		this.product = product;
	}

	protected Category() {}

	public Long getId() {
		return id;
	}

	public Product getProduct() {
		return product;
	}
}
