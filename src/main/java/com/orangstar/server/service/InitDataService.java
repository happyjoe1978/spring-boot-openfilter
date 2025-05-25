package com.orangstar.server.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.orangstar.server.repository.CompanyRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Service
public class InitDataService {
  @PersistenceContext
  private EntityManager entityManager;

  @Autowired
  private CompanyRepository companyRepository;
}
