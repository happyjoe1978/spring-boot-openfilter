package com.orangstar.server.openfilter;

import java.io.Serializable;

import org.springframework.data.jpa.repository.JpaRepository;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

public interface OpenFilterRepository<T, ID extends Serializable> extends JpaRepository<T, ID> {
  /**
   * 数据查询
   * 
   * @param jsonNode Open API 查询条件
   * @return
   */
  public ArrayNode query(JsonNode jsonNode);
}
