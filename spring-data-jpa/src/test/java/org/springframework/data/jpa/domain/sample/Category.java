package org.springframework.data.jpa.domain.sample;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

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
