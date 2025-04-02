package com.banka1.banking.service;

import com.banka1.banking.dto.CreateCompanyDTO;
import com.banka1.banking.models.Company;
import com.banka1.banking.models.helper.BusinessActivityCode;
import com.banka1.banking.repository.CompanyRepository;
import com.banka1.banking.services.CompanyService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;


import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CompanyServiceTest {
    @Mock
    private CompanyRepository companyRepository;
    @Mock
    private ModelMapper modelMapper;
    @InjectMocks
    private CompanyService companyService;

    @Test
    public void testAddCompany() {
        CreateCompanyDTO companyDTO = new CreateCompanyDTO();
        companyDTO.setCompanyNumber("123455678");
        companyDTO.setVatNumber("123456789");
        companyDTO.setAddress("Bulevar Banke 1");
        companyDTO.setName("Test Company");
        companyDTO.setBas(BusinessActivityCode.COMPUTER_PROGRAMMING);
        companyDTO.setOwnerId(1L);

        Company company = new Company();
        company.setCompanyNumber("87654321");
        company.setVatNumber("123456789");
        company.setAddress("Bulevar Banke 1");
        company.setName("Test Company");
        company.setBas(BusinessActivityCode.COMPUTER_PROGRAMMING);
        company.setOwnerID(1L);

        when(modelMapper.map(companyDTO, Company.class)).thenReturn(company);
        when(companyRepository.save(any(Company.class))).thenReturn(company);

        Company result = companyService.createCompany(companyDTO);
        assertNotNull(result);
        Mockito.verify(companyRepository, times(1)).save(company);
    }
}
