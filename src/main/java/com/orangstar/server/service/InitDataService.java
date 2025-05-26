package com.orangstar.server.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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
    System.out.println("初始数据结束.....");
  }

}
