package com.orangstar.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.File;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.orangstar.server.repository.CompanyRepository;
import com.orangstar.server.repository.DepartmentRepository;
import com.orangstar.server.service.InitDataService;

import jakarta.transaction.Transactional;

@SpringBootTest
@Transactional
class DemoApplicationTests {

  @Autowired
  InitDataService initDataService;

  @Autowired
  CompanyRepository companyRepo;

  @Autowired
  DepartmentRepository departmentRepo;

  @BeforeEach
  void setUp() {
    initDataService.initData();
  }

  private ObjectMapper objectMapper = new ObjectMapper();

  @Test
  void testStatus() {
    try {
      JsonNode filter = objectMapper.readTree(new File("test-status.json"));
      ArrayNode result = companyRepo.query(filter);

      Integer size = result.size();
      assertNotNull(size);
      assertEquals(8, size);
    } catch (Exception e) {
      // TODO: handle exception
    }

  }

  @Test
  void testLimit() {

    try {
      JsonNode filter = objectMapper.readTree(new File("test-limit.json"));
      ArrayNode result = companyRepo.query(filter);
      Integer size = result.size();
      assertNotNull(size);
      assertEquals(6, size);
    } catch (Exception e) {
      // TODO: handle exception
    }

    Long deptCount = departmentRepo.count();
    assertNotNull(deptCount);
  }

  @Test
  void testInclude() {
    try {
      JsonNode filter = objectMapper.readTree(new File("test-include.json"));
      ArrayNode results = companyRepo.query(filter);
      JsonNode res = results.get(0);
      assertNotNull(res.get("departments"));
    } catch (Exception e) {
      // TODO: handle exception
    }
  }
}
