package com.orangstar.server.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.orangstar.server.entity.Company;
import com.orangstar.server.entity.Department;
import com.orangstar.server.repository.CompanyRepository;
import com.orangstar.server.repository.DepartmentRepository;
import com.orangstar.server.repository.MemberRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Service
public class InitDataService {
  @PersistenceContext
  private EntityManager entityManager;

  @Autowired
  private CompanyRepository companyRepo;

  @Autowired
  private DepartmentRepository departmentRepo;

  @Autowired
  private MemberRepository memberRepo;

  public void cleanAll() {
    entityManager.createNativeQuery("DELETE FROM company").executeUpdate();
    entityManager.createNativeQuery("DELETE FROM department").executeUpdate();
    entityManager.createNativeQuery("DELETE FROM member").executeUpdate();
    entityManager.createNativeQuery("DELETE FROM note").executeUpdate();
  }

  public void initData() {
    System.out.println("初始数据开始.....");
    cleanAll();
    initCompanys();
    System.out.println("初始数据结束.....");
  }

  private void initCompanys() {
    for (int i = 0; i < 17; i++)
      createCompany(i);
  }

  private void createCompany(Integer index) {
    Company c = new Company();
    c.setNum(index + "");
    c.setName("company-" + index);
    if (index < 8)
      c.setStatus(1);
    else
      c.setStatus(0);
    c.setDepartments(createDepartments());
    companyRepo.save(c);
  }

  private List<Department> createDepartments() {
    List<Department> depts = new ArrayList<>();
    for (int i = 0; i < 16; i++) {
      Department d = new Department();
      d.setName("dept-" + 1);
      d.setCode(i + "");
    }
    return depts;
  }

}
