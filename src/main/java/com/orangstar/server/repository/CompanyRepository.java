package com.orangstar.server.repository;

import com.orangstar.server.entity.Company;
import com.orangstar.server.openfilter.OFRepository;
import com.orangstar.server.openfilter.OpenFilterRepository;

@OFRepository
public interface CompanyRepository extends OpenFilterRepository<Company, String> {

}
