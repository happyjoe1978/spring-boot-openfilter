package com.orangstar.server.service;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.orangstar.server.util.Utils;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Fetch;
import jakarta.persistence.criteria.FetchParent;
import jakarta.persistence.criteria.From;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.Metamodel;
import jakarta.persistence.metamodel.SingularAttribute;

@Service
public class OFService {

  @PersistenceContext
  private EntityManager entityManager;

  private ObjectMapper objMapper = new ObjectMapper();

  public ArrayNode query(JsonNode filterNode, Class<?> entityClass) {
    if (!validateCondition(filterNode)) {
      throw new IllegalArgumentException("condition is not a valid JSON object.");
    }

    // 如果有分页且有关联，则需要进行分页子查询
    if (filterNode.has("skip") && filterNode.has("limit") && filterNode.has("include")) {
      ArrayNode pkIds = objMapper.createArrayNode();
      // 处理根的关联查询分页，获取分页后的id列表
      CriteriaBuilder cb = entityManager.getCriteriaBuilder();
      CriteriaQuery<?> pageQuery = cb.createQuery(Object.class);

      String pkName = getPrimaryKeyFieldName(entityClass);
      System.out.println("主键字段名：" + pkName);
      if (pkName == null || pkName.trim() == "")
        throw new IllegalArgumentException("No primary key.");

      Root<?> pageRoot = pageQuery.from(entityClass);

      pageQuery.select(pageRoot.get(pkName));

      if (filterNode.has("where")) {
        JsonNode whereNode = filterNode.path("where");
        Predicate predicate = parseWhere(pageRoot, whereNode, cb);
        pageQuery.where(predicate);
      }

      if (filterNode.has("order")) {
        JsonNode orderNode = filterNode.path("order");
        List<Order> pageOrders = new ArrayList<Order>();
        parseOrder(pageRoot, orderNode, cb, pageOrders);
        pageQuery.orderBy(pageOrders);
      }

      TypedQuery<?> idQuery = entityManager.createQuery(pageQuery);
      Integer skip = filterNode.path("skip").asInt();
      Integer limit = filterNode.path("limit").asInt();
      idQuery.setFirstResult(skip);
      idQuery.setMaxResults(limit);
      List<?> idList = idQuery.getResultList();
      for (Object oid : idList) {
        pkIds.add(objMapper.valueToTree(oid));
      }
      System.out.println("分页ids：" + pkIds);

      // 如果存在pkIds，则替换掉filter中的where条件，并删除order
      if (!pkIds.isEmpty()) {
        // where条件用 in pkids，删除order
        ObjectNode modifiedFilter = objMapper.createObjectNode();
        modifiedFilter.setAll((ObjectNode) filterNode);
        ObjectNode inNode = objMapper.createObjectNode();
        inNode.set("in", pkIds);
        ObjectNode newWhereNode = objMapper.createObjectNode();
        newWhereNode.set(pkName, inNode);
        modifiedFilter.set("where", newWhereNode);
        modifiedFilter.remove("order");
        modifiedFilter.remove("skip");
        modifiedFilter.remove("limit");
        filterNode = modifiedFilter;
        System.out.println("Updated filterNode: " + filterNode);
      }
    }

    // 进入查询处理
    CriteriaBuilder builder = entityManager.getCriteriaBuilder();
    CriteriaQuery<?> query = builder.createQuery(Object.class);
    Root<?> root = query.from(entityClass);

    // 排序
    List<Order> orders = new ArrayList<>();
    buildeQuery(root, filterNode, query, orders, builder);
    query.orderBy(orders.toArray(new Order[0]));
    TypedQuery<?> tQuery = entityManager.createQuery(query);

    // 一般分页处理，如果进行过关联分页子查询处理，此处不会再有limit和skip
    if (filterNode.has("limit") && filterNode.has("skip")) {
      // 处理limit、skip子句
      JsonNode limitNode = filterNode.path("limit");
      // System.out.println("limit子句：" + limitNode.toString());
      JsonNode skipNode = filterNode.path("skip");
      // System.out.println("skip子句：" + skipNode.toString());
      // 实现分页查询
      int limit = limitNode.asInt();
      int skip = skipNode.asInt();
      tQuery.setFirstResult(skip);
      tQuery.setMaxResults(limit);
    }

    List<?> result = tQuery.getResultList();
    ArrayNode arrayNode = objMapper.createArrayNode();

    converEntitys(result, filterNode, arrayNode);
    return arrayNode;
  }

  /**
   * 构建查询
   * 
   * @param <X>
   * @param <Y>
   * @param fetchObj
   * @param condition
   * @param query
   * @param orders
   * @param builder
   */
  private <X, Y> void buildeQuery(FetchParent<X, Y> fetchObj, JsonNode condition, CriteriaQuery<?> query,
      List<Order> orders, CriteriaBuilder builder) {
    parseIncludes(fetchObj, condition, query, orders, builder);
    if (condition.has("where")) {
      // 处理where子句，分两种情况：
      // 1. from为 root，query.where(predicate);
      // 2. from为 join, join.on(predicate) 目前先只实现左连接
      JsonNode whereNode = condition.path("where");
      System.out.println("where子句：" + whereNode.toString());
      Predicate predicate = parseWhere(fetchObj, whereNode, builder);
      if (fetchObj instanceof Root)
        query.where(predicate);
      else {
        Join<X, Y> join = (Join<X, Y>) fetchObj;
        join.on(predicate);
      }
    }

    if (condition.has("order")) {
      JsonNode oderNode = condition.path("order");
      System.out.println("order子句：" + oderNode.toString());
      parseOrder(fetchObj, oderNode, builder, orders);
    }
  }

  /**
   * 解析关联关系
   * 
   * @param <X>
   * @param <Y>
   * @param fetchObj
   * @param condition
   * @param query
   * @param orders
   * @param builder
   */
  private <X, Y> void parseIncludes(FetchParent<X, Y> fetchObj, JsonNode condition, CriteriaQuery<?> query,
      List<Order> orders, CriteriaBuilder builder) {
    if (condition.has("include")) {
      JsonNode includesNode = condition.path("include");
      if (includesNode.isArray()) {
        includesNode.forEach(includeNode -> {
          if (includeNode.has("relation")) {
            // 处理include条件
            parseInclude(fetchObj, includeNode, query, orders, builder);
          } else
            throw new IllegalArgumentException(
                "The include object must be in the format { \"relation\": \"fieldName\" }.");
        });
      } else
        throw new IllegalArgumentException(
            "The include object must be in the format { \"relation\": \"fieldName\" }.");
    }
  }

  /**
   * 解析单个关联
   * 
   * @param <X>
   * @param <Y>
   * @param fetchObj
   * @param includeNode
   * @param query
   * @param orders
   * @param builder
   */
  private <X, Y> void parseInclude(FetchParent<X, Y> fetchObj, JsonNode includeNode, CriteriaQuery<?> query,
      List<Order> orders, CriteriaBuilder builder) {
    // 校验必须包含 relation 字段
    if (!includeNode.has("relation")) {
      throw new IllegalArgumentException("// TODO include 条件必须包含 'relation' 字段");
    }
    // 获取关联关系名称
    String relationName = includeNode.path("relation").asText();
    // 创建关联查询（默认使用左连接）
    Fetch<X, Y> fetchAll = fetchObj.fetch(relationName, JoinType.LEFT);
    // 处理 scope 条件（如果存在）,如果没有scope条件，则直接通过关联关系全量访问。
    if (includeNode.has("scope")) {
      JsonNode scopeNode = includeNode.path("scope");
      buildeQuery(fetchAll, scopeNode, query, orders, builder);
    }
  }

  /**
   * 解析排序条件（仅支持数组格式）
   * 
   * @param <X>       源表实体类
   * @param <Y>       目标表实体类
   * @param fetchObj  实体类关联信息
   * @param orderNode 排序条件JSON节点
   * @return 排序条件列表
   */
  private <X, Y> void parseOrder(FetchParent<X, Y> fetchObj, JsonNode orderNode, CriteriaBuilder builder,
      List<Order> orders) {
    // 仅支持数组格式: ["fieldName ASC", "fieldName DESC"]
    if (!orderNode.isArray()) {
      throw new IllegalArgumentException("Order must be an array of strings");
    }
    if (fetchObj instanceof From) {
      From<X, Y> from = (From<X, Y>) fetchObj;
      for (JsonNode node : orderNode) {
        if (!node.isTextual()) {
          throw new IllegalArgumentException("Each order item must be a string");
        }

        String orderStr = node.asText();
        String[] parts = orderStr.trim().split("\\s+");
        if (parts.length != 1 && parts.length != 2) {
          throw new IllegalArgumentException("Invalid order format: " + orderStr +
              ". Expected 'fieldName [ASC|DESC]'");
        }

        String fieldName = parts[0];
        String direction = parts.length == 2 ? parts[1] : "ASC";

        if (!"ASC".equalsIgnoreCase(direction) && !"DESC".equalsIgnoreCase(direction)) {
          throw new IllegalArgumentException("Invalid order direction: " + direction +
              ". Only ASC or DESC are allowed");
        }

        // 解析字段路径，支持关联表字段排序
        Path<?> path = from;
        String[] fieldPathParts = fieldName.split("\\.");
        for (String part : fieldPathParts) {
          path = path.get(part);
        }

        Order order = "ASC".equalsIgnoreCase(direction) ? builder.asc(path) : builder.desc(path);
        orders.add(order);
      }
    }

    if (orders.isEmpty()) {
      throw new IllegalArgumentException("Order array must not be empty");
    }
  }

  /**
   * 解析并输出where条件
   * 
   * @param <X>
   * @param <Y>
   * @param fetchObj
   * @param whereNode
   * @param builder
   * @return
   */
  private <X, Y> Predicate parseWhere(FetchParent<X, Y> fetchObj, JsonNode whereNode, CriteriaBuilder builder) {
    Predicate result = null;
    if (fetchObj instanceof From) {
      From<X, Y> from = (From<X, Y>) fetchObj;
      if (whereNode.has("and")) {
        JsonNode andNode = whereNode.path("and");
        // 处理and逻辑
        result = whereAnd(andNode, from, builder);
      } else if (whereNode.has("or")) {
        JsonNode orNode = whereNode.path("or");
        // 处理or逻辑
        result = whereOr(orNode, from, builder);
      } else if (whereNode.has("not")) {
        JsonNode notNode = whereNode.path("not");
        // 处理not逻辑
        result = whereNot(notNode, from, builder);
      } else {
        // 处理数据库字段比较操作
        result = whereFields(whereNode, from, builder);
      }
    }
    return result;
  }

  /**
   * 处理 and 逻辑
   * 
   * @param <X>
   * @param <Y>
   * @param andNode
   * @param from
   * @param builder
   * @return
   */
  private <X, Y> Predicate whereAnd(JsonNode andNode, From<X, Y> from, CriteriaBuilder builder) {
    Predicate result = null;
    // 处理逻辑操作符，与
    // 校验必须是数组格式
    if (!andNode.isArray()) {
      throw new IllegalArgumentException("'and'条件必须是条件数组");
    }
    // 收集所有子条件
    List<Predicate> predicates = new ArrayList<>();
    for (JsonNode conditionNode : andNode) {
      // 递归解析每个子条件
      Predicate predicate = parseWhere(from, conditionNode, builder);
      if (predicate != null) {
        predicates.add(predicate);
      }
    }
    // 组合所有条件用AND连接
    if (predicates.isEmpty()) {
      return null; // 无有效条件返回null
    }
    if (predicates.size() == 1) {
      return predicates.get(0); // 单个条件直接返回
    }
    result = builder.and(predicates.toArray(new Predicate[0]));
    return result;
  }

  /**
   * 处理 or 逻辑
   * 
   * @param <X>     源表实体类
   * @param <Y>     目标表实体类
   * @param orNode  or 逻辑体
   * @param from    实体类关联信息
   * @param builder
   * @return
   */
  private <X, Y> Predicate whereOr(JsonNode orNode, From<X, Y> from, CriteriaBuilder builder) {
    Predicate result = null;
    // 处理逻辑操作符，或
    // 校验必须是数组格式
    if (!orNode.isArray()) {
      throw new IllegalArgumentException("'or'条件必须是条件数组");
    }
    // 收集所有子条件
    List<Predicate> predicates = new ArrayList<>();
    for (JsonNode conditionNode : orNode) {
      // 递归解析每个子条件（支持嵌套AND/OR/NOT）
      Predicate predicate = parseWhere(from, conditionNode, builder);
      if (predicate != null) {
        predicates.add(predicate);
      }
    }

    // 组合所有条件用OR连接
    if (predicates.isEmpty()) {
      return null; // 无有效条件返回null
    }
    if (predicates.size() == 1) {
      return predicates.get(0); // 单个条件直接返回
    }
    result = builder.or(predicates.toArray(new Predicate[0]));
    return result;
  }

  /**
   * 处理数据库字段比较操作
   * 
   * {
   * "status": {"gt":1},
   * "no": 0,
   * "name": "dd"
   * }
   * 
   * 
   * @param <X>
   * @param <Y>
   * @param fieldsNode
   * @param from
   * @param query
   * @param builder
   * @return
   */
  private <X, Y> Predicate whereFields(JsonNode fieldsNode, From<X, Y> from, CriteriaBuilder builder) {
    // 处理数据库字段比较操作
    Predicate result = null;
    Iterator<Entry<String, JsonNode>> fields = fieldsNode.fields();
    List<Predicate> predicates = new ArrayList<>();

    // 循环处理where条件里面的每一个字段
    while (fields.hasNext()) {
      Entry<String, JsonNode> field = fields.next();
      Predicate fieldPredicate = whereFieldComparison(field, from, builder);
      if (fieldPredicate != null)
        predicates.add(fieldPredicate);
    }
    // 处理key value逻辑，默认and
    if (predicates.size() > 0)
      result = builder.and(predicates.toArray(new Predicate[0]));
    return result;
  }

  /**
   * not条件
   * 
   * @param <X>
   * @param <Y>
   * @param notNode
   * @param from
   * @param builder
   * @return
   */
  private <X, Y> Predicate whereNot(JsonNode notNode, From<X, Y> from, CriteriaBuilder builder) {
    // 校验必须是对象格式（不能是数组或基本值）
    if (!notNode.isObject() || notNode.isEmpty()) {
      throw new IllegalArgumentException("'not'条件必须是非空的JSON对象");
    }

    // 递归解析被取反的条件（支持嵌套AND/OR/NOT）
    Predicate innerPredicate = parseWhere(from, notNode, builder);

    // 对条件取反
    if (innerPredicate == null) {
      throw new IllegalArgumentException("'not'条件内没有有效的查询条件");
    }
    return builder.not(innerPredicate);
  }

  /**
   * 处理字段比较条件，包含两种情况：
   * 
   * 1. 处理操作符条件{ "gt": 1, "lt": 5 } 等形式
   * 
   * 2. 处理键值对等于条件 { "no": 0 } 或 { "name": "dd" }等形式
   * 
   * 
   * @param <X>
   * @param <Y>
   * @param field
   * @param from
   * @param query
   * @param builder
   * @return
   */
  private <X, Y> Predicate whereFieldComparison(Entry<String, JsonNode> field, From<X, Y> from,
      CriteriaBuilder builder) {
    Predicate result = null;
    String fieldName = field.getKey();
    JsonNode conditionNode = field.getValue();
    System.out.println("字段名：" + fieldName);
    System.out.println("字段条件：" + conditionNode.toString());
    // 需要匹配class得到比较值的类型，然后将之转换成对应对象
    // 这里需要验证一下
    Field classField = Utils.getFieldByName(from.getJavaType(), fieldName);
    if (classField == null)
      throw new IllegalArgumentException(
          "The field name '" + fieldName + "' in the query condition did not match any database field.");
    Class<?> fieldType = classField.getType();

    if (conditionNode.isObject()) {
      // 处理操作符条件{ "gt": 1, "lt": 5 } 等形式
      result = operatorCondition(conditionNode, from, builder, fieldType, fieldName);
    } else {
      // 处理键值对等于条件
      // System.out.println("处理键值对等于条件");
      result = kvCondition(conditionNode, from, builder, fieldType, fieldName);
    }
    return result;
  }

  /**
   * 判断json filter里存在有必要的查询关键字
   * 
   * @param condition
   * @return
   */
  private static Boolean validateCondition(JsonNode condition) {
    if (!condition.isObject()) {
      return false;
    }
    if (condition.has("where") || condition.has("fields") || condition.has("order") || condition.has("limit")
        || condition.has("skip") || condition.has("include"))
      return true;
    else
      return false;
  }

  /**
   * 将对象转换为JsonNode
   * 
   * @param entitys
   * @param filter
   * @param dataList
   */
  private void converEntitys(List<?> entitys, JsonNode filter, ArrayNode dataList) {
    List<String> selectedFieldNames = Utils.getFieldNamesFromFilter(filter);
    // System.out.println("显示字段：" + selectedFieldNames.toString());
    List<String> includeFieldNames = Utils.getFieldNamesFromInclude(filter);
    // System.out.println("关联字段：" + includeFieldNames);

    Class<?> entityClass = entitys.getFirst().getClass();
    List<String> noRelationFieldNames = Utils.getRelationFieldNamesFromEntity(entityClass, false);
    // System.out.println("entity非关联字段：" + noRelationFieldNames.toString());
    List<String> relationFieldNames = Utils.getRelationFieldNamesFromEntity(entityClass, true);
    // System.out.println("entity关联字段：" + relationFieldNames.toString());
    List<String> showFieldNames = Utils.getShowFieldNames(selectedFieldNames, includeFieldNames,
        relationFieldNames, noRelationFieldNames);
    // System.out.println("最终需要显示的字段：" + showFieldNames.toString());

    for (Object entity : entitys) {
      dataList.add(converEntity(entity, showFieldNames, includeFieldNames, filter));
    }
  }

  private ObjectNode converEntity(Object entity, List<String> showFieldNames, List<String> includeFieldNames,
      JsonNode filterNode) {
    ObjectNode node = objMapper.createObjectNode();
    Class<?> entityClass = entity.getClass();
    List<Field> fields = Utils.getAllFields(entityClass);
    for (Field field : fields) {
      try {
        field.setAccessible(true); // 允许访问私有字段
        String fieldName = field.getName();
        Object value = field.get(entity);
        if (includeFieldNames.contains(fieldName)) {
          // 处理关联对象
          // 获取include里的scope
          JsonNode scopeNode = Utils.getIncludeScope(fieldName, filterNode);
          // System.out.println("include scope 对象" + scopeNode);
          if (value instanceof List) {
            // 一对多、多对多关联
            ArrayNode subNodes = objMapper.createArrayNode();
            node.set(fieldName, subNodes);

            List<?> subEntitys = (List<?>) value;
            converEntitys(subEntitys, scopeNode, subNodes);
          } else {
            // 一对一，多对一关联
            addValueToNode(node, fieldName, value);
          }
        } else if (showFieldNames.contains(fieldName)) {
          // 处理一般对象
          addValueToNode(node, fieldName, value);
        }
      } catch (Exception e) {
        // TODO: handle exception
      }
    }
    return node;
  }

  /**
   * 处理操作符条件
   * 
   * { "gt": 1, "lt": 5 } 等形式
   * 
   * @param <X>
   * @param <Y>
   * @param conditionNode
   * @param from
   * @param query
   * @param builder
   * @return
   */
  @SuppressWarnings("unchecked")
  private static <X, Y, T extends Comparable<T>> Predicate operatorCondition(JsonNode conditionNode,
      From<X, Y> from,
      CriteriaBuilder builder, Class<?> fieldType, String fieldName) {
    Predicate result = null;
    List<Predicate> predicates = new ArrayList<>();
    // Path<?> fieldPath = from.get(fieldName);
    // 遍历所有操作符
    Iterator<Map.Entry<String, JsonNode>> fields = conditionNode.fields();
    while (fields.hasNext()) {
      Entry<String, JsonNode> entry = fields.next();
      String operator = entry.getKey();
      JsonNode valueNode = entry.getValue();
      Object value = Utils.convertJsonNodeToJavaType(valueNode, fieldType);

      // 根据操作符创建对应的Predicate
      switch (operator.toLowerCase()) {
        case "gt":
          if (value instanceof Comparable) {
            predicates.add(builder.greaterThan(from.get(fieldName), (T) value));
          }
          break;
        case "lt":
          if (value instanceof Comparable) {
            predicates.add(builder.lessThan(from.get(fieldName), (T) value));
          }
          break;
        case "gte":
          if (value instanceof Comparable) {
            predicates.add(builder.greaterThanOrEqualTo(from.get(fieldName), (T) value));
          }
          break;
        case "eq":
          if (value instanceof Comparable) {
            predicates.add(builder.equal(from.get(fieldName), value));
          }
          break;
        case "neq":
          if (value instanceof Comparable) {
            predicates.add(builder.notEqual(from.get(fieldName), value));
          }
          break;
        case "like":
          predicates.add(builder.like(from.get(fieldName), "%" + value.toString() + "%"));
          break;
        case "startswith":
          predicates.add(builder.like(from.get(fieldName), "%" + value.toString()));
          break;
        case "endswith":
          predicates.add(builder.like(from.get(fieldName), value.toString() + "%"));
          break;
        case "in":
          if (valueNode.isArray()) {
            CriteriaBuilder.In<Object> in = builder.in(from.get(fieldName));
            for (JsonNode element : valueNode) {
              in.value(Utils.convertJsonNodeToJavaType(element, fieldType));
            }
            predicates.add(in);
          }
          break;
        case "nin":
          if (valueNode.isArray()) {
            CriteriaBuilder.In<Object> in = builder.in(from.get(fieldName));
            for (JsonNode element : valueNode) {
              in.value(Utils.convertJsonNodeToJavaType(element, fieldType));
            }
            predicates.add(builder.not(in));
          }
          break;
        case "isnull":
          predicates.add(builder.isNull(from.get(fieldName)));
          break;
        case "isnotnull":
          predicates.add(builder.isNotNull(from.get(fieldName)));
          break;
        case "between": // 在范围内
          if (valueNode.isArray() && valueNode.size() == 2) {
            Object start = Utils.convertJsonNodeToJavaType(valueNode.get(0), fieldType);
            Object end = Utils.convertJsonNodeToJavaType(valueNode.get(1), fieldType);
            predicates.add(builder.between(from.get(fieldName), (T) start, (T) end));
          }
          break;
        // case "exists": // 存在字段
        // // JPA中通常通过isNotNull判断存在性
        // predicates.add(builder.isNotNull(from.get(fieldName)));
        // break;
        default:
          throw new IllegalArgumentException("Unsupported query operator: " + operator);
      }
      result = builder.and(predicates.toArray(new Predicate[0]));
    }
    return result;
  }

  /**
   * 获取主键字段名称
   * 
   * @param entityManager
   * @param entityClass
   * @return
   */
  public String getPrimaryKeyFieldName(Class<?> entityClass) {
    Metamodel metamodel = entityManager.getMetamodel();
    EntityType<?> entityType = metamodel.entity(entityClass);
    SingularAttribute<?, ?> idAttribute = entityType.getId(entityType.getIdType().getJavaType());
    return idAttribute.getName();
  }

  /**
   * 处理键值对等于条件
   * 
   * { "no": 0 } 或 { "name": "dd" }等形式
   * 
   * @param <X>
   * @param <Y>
   * @param <T>
   * @param conditionNode
   * @param from
   * @param builder
   * @param fieldType
   * @return
   */
  // @SuppressWarnings("unchecked")
  private static <X, Y, T extends Comparable<T>> Predicate kvCondition(JsonNode conditionNode,
      From<X, Y> from,
      CriteriaBuilder builder, Class<?> fieldType, String fieldName) {
    Predicate result = null;
    Object value = Utils.convertJsonNodeToJavaType(conditionNode, fieldType);
    if (value instanceof Comparable) {
      result = builder.equal(from.get(fieldName), value);
    }
    return result;
  }

  /**
   * 将值添加到JsonNode中，处理不同类型
   */
  private void addValueToNode(ObjectNode node, String fieldName, Object value) {
    if (value == null) {
      node.putNull(fieldName);
    } else if (value instanceof String) {
      node.put(fieldName, (String) value);
    } else if (value instanceof Integer) {
      node.put(fieldName, (Integer) value);
    } else if (value instanceof Long) {
      node.put(fieldName, (Long) value);
    } else if (value instanceof Double) {
      node.put(fieldName, (Double) value);
    } else if (value instanceof Boolean) {
      node.put(fieldName, (Boolean) value);
    } else {
      // TODO 复杂对象处理
      System.out.println("复杂对象处理");
    }
  }

}
