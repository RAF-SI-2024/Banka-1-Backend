package com.banka1.user.mapper;

import com.banka1.user.DTO.request.CreateCustomerRequest;
import com.banka1.user.DTO.response.CustomerResponse;
import com.banka1.user.model.Customer;

/**
 * Mapper class to convert between Customer and CustomerDTO.
 */
public class CustomerMapper {

    /**
     * Converts a CustomerDTO object to a Customer entity.
     *
     * @param dto The CustomerDTO object.
     * @return The converted Customer entity.
     */
    public static Customer dtoToCustomer(CreateCustomerRequest dto) {
        Customer customer = new Customer();
        customer.setFirstName(dto.getFirstName());
        customer.setLastName(dto.getLastName());
        customer.setUsername(dto.getUsername());
        customer.setBirthDate(dto.getBirthDate());
        customer.setGender(dto.getGender());
        customer.setEmail(dto.getEmail());
        customer.setPhoneNumber(dto.getPhoneNumber());
        customer.setAddress(dto.getAddress());

        return customer;
    }

    /**
     * Converts a Customer entity to a CustomerDTO object.
     *
     * @param customer The Customer entity.
     * @return The converted CustomerDTO object.
     */
    public static CustomerResponse customerToDto(Customer customer) {
        return new CustomerResponse(
                customer.getId(),
                customer.getFirstName(),
                customer.getLastName(),
                customer.getUsername(),
                customer.getBirthDate(),
                customer.getGender(),
                customer.getEmail(),
                customer.getPhoneNumber(),
                customer.getAddress(),
                customer.getPermissions());
    }
}
