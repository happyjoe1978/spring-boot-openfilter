package com.orangstar.server;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.orangstar.server.repository.CompanyRepository;
import com.orangstar.server.service.InitDataService;

import jakarta.transaction.Transactional;

@SpringBootTest
@Transactional
class DemoApplicationTests {

  // @Autowired
  // InitDataService initDataService;

  // @Autowired
  // CompanyRepository companyRepo;

  // @BeforeEach
  // void setUp() {
  // initDataService.initData();
  // }

  @Test
  void hasData() {
    // Long companyCount = companyRepo.count();
    System.out.println("查询记录数");
    // assertNotNull(companyCount);
    // assertNotEquals(0, companyCount);
  }

}
