package com.banka1.banking.services;

import com.banka1.banking.dto.CreateCompanyDTO;
import com.banka1.banking.models.Company;
import com.banka1.banking.models.helper.BusinessActivityCode;
import com.banka1.banking.repository.CompanyRepository;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service

public class CompanyService {
    private final CompanyRepository companyRepository;
    private final ModelMapper modelMapper;

    public CompanyService(CompanyRepository companyRepository, ModelMapper modelMapper) {
        this.companyRepository = companyRepository;
        this.modelMapper = modelMapper;
    }

    public Company createCompany(CreateCompanyDTO createCompanyDTO) {
        Company company = modelMapper.map(createCompanyDTO, Company.class);
        System.out.println(company.getName());
        company = companyRepository.save(company);
        return company;
    }

    public Company getCompany(Long companyId) {
        return companyRepository.findById(companyId).orElse(null);
    }

    public List<Company> getCompanies() {
        return companyRepository.findAll();
    }

    public List<String> getBusinessActivityCodes() {
        return BusinessActivityCode.getAll();
    }

    public List<Company> findAllByOwnerId(Long ownerId) {
        return companyRepository.findByOwnerID(ownerId);
    }

    public Company findByCompanyNumber(String companyNumber) {
        return companyRepository.findByCompanyNumber(companyNumber).orElse(null);
    }

}
