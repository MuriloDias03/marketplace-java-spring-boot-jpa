package com.murilocdias.marketplace.registration.infrastructure.persistence.entity.projection;

import com.murilocdias.marketplace.registration.infrastructure.persistence.entity.Customer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.rest.core.config.Projection;

@Projection(name = "excerpt", types = Customer.class)
public interface CustomerExcerpt {

    String getFirstName();

    String getLastName();

    @Value("#{target.address?.toString()}")
    String getAddress();

}