package com.orangstar.server.repository;

import com.orangstar.server.entity.Department;
import com.orangstar.server.openfilter.OFRepository;
import com.orangstar.server.openfilter.OpenFilterRepository;

@OFRepository
public interface DepartmentRepository extends OpenFilterRepository<Department, String> {

}
