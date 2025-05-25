package com.orangstar.server.openfilter;

import java.io.Serializable;

import org.springframework.data.jpa.repository.support.SimpleJpaRepository;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.orangstar.server.service.OFService;

import jakarta.persistence.EntityManager;

public class OpenFilterRepositoryImpl<T, ID extends Serializable> extends SimpleJpaRepository<T, ID>
    implements OpenFilterRepository<T, ID> {

  private Class<T> entityClass;

  private OFService ofService;

  public OpenFilterRepositoryImpl(Class<T> ec, EntityManager em, OFService os) {
    super(ec, em);
    entityClass = ec;
    ofService = os;
  }

  public ArrayNode query(JsonNode jsonNode) {
    // System.out.println("查询......");
    return ofService.query(jsonNode, entityClass);
  }

}
