# spring-boot-openfilter

A Spring Boot-based dynamic data query tool for the frontend that adheres to the OpenAPI specification.

对 Srping data 进行扩展，实现了基于 OPEN API 的前端数据查询 filter 范式。

# 依赖加载

加载 Maven 依赖，并在 Appliaction 文件上表注配置 OpenFilterConfig。

```java
@Import(OpenFilterConfig.class)
@SpringBootApplication
public class DemoApplication {

  public static void main(String[] args) {
    SpringApplication.run(DemoApplication.class, args);
  }

}
```

# 使用方式

使用@OFRepository 注解标注 Repository 接口。

OpenFilterRepository 继承 Spring Data 的 JpaRepository 接口，并继承默认实现类 SimpleJpaRepository，如果是自定义实现类则需要自行代码改造。

```java
@OFRepository
public interface CompanyRepository extends OpenFilterRepository<Company, String> {

}
```

## query 方法

扩展出 query 方法，传入 Filter 范式的查询对象，返回查询结果 ArrayNode。

```java
  /**
   * 数据查询
   *
   * @param jsonNode Open API 查询条件
   * @return
   */
  public ArrayNode query(JsonNode jsonNode);
```

# filter 范式

## filter 范式的核心概念

Filter 范式是一个 JSON 对象，用于描述查询的条件和操作。它支持以下主要功能：

- **过滤（Where）**：根据条件筛选数据。

- **排序（Order）**：对结果进行排序。

- **分页（Limit/Skip）**：控制返回结果的数量和偏移量。(只对主表生效)

- **字段选择（Fields）**：指定返回的字段。

- **包含关联数据（Include）**：加载关联模型的数据。

## Filter 范式的 JSON 结构

一个典型的 Filter JSON 对象如下：

```json
{
  "where": {
    "condition": "value"
  },
  "order": ["field1 ASC", "field2 DESC"],
  "limit": 10,
  "offset": 0,
  "fields": ["id", "name", "email"],
  "include": [
    {
      "relation": "orders",
      "scope": {
        "where": { "status": "shipped" }
      }
    }
  ]
}
```

## 过滤（Where）

- **作用**：根据条件筛选数据。

- **语法**：

```json
{
  "where": {
    "property": "value",
    "and": [...],
    "or": [...],
    "gt": ">",
    "gte": ">=",
    "lt": "<",
    "lte": "<=",
    "neq": "!=",
    "inq": ["value1", "value2"],
    "nin": ["value1", "value2"],
    "like": "pattern",
    "nlike": "pattern",
    "between": [start, end],
    "isnull": true/false
  }
}
```

- **示例**：

```json
{
  "where": {
    "age": { "gt": 18 },
    "status": "active",
    "or": [
      { "name": { "like": "John%" } },
      { "email": { "like": "%@example.com" } }
    ]
  }
}
```

## 排序（Order）

- **作用**：对结果进行排序。（可作用于关联查询）

- **语法**：

···json
{
"order": ["field1 ASC", "field2 DESC"]
}
···

- **示例**：

```json
{
  "order": ["name ASC", "createdAt DESC"]
}
```

## 分页（Limit/Skip）

- **作用**：控制返回结果的数量和偏移量。当查询有关联关系时只作用于主表。

- **语法**：

```json
{
  "limit": 10, // 返回10条记录
  "skip": 5 // 跳过前5条记录（等效于offset）
}
```

- **示例**：同上

## 包含关联数据（Include）

- **作用**：加载关联模型的数据。

- **语法**：

```json

{
  "include": [
    {
      "relation": "relationName",
      "scope": { ... }  // 可选的子查询条件
    }
  ]
}

```

- **示例**：

```json
{
  "include": [
    {
      "relation": "orders",
      "scope": {
        "where": { "status": "shipped" }
      }
    }
  ]
}
```
