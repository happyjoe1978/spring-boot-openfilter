package com.orangstar.server.util;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Date;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;

import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;

public class Utils {

  @SuppressWarnings("unchecked")
  public static <T> T convertJsonNodeToJavaType(JsonNode node, Class<T> targetType) throws IllegalArgumentException {
    if (node == null || node.isNull())
      return null;
    if (targetType == String.class) {
      return (T) node.asText();
    } else if (targetType == int.class || targetType == Integer.class) {
      return (T) Integer.valueOf(node.asInt());
    } else if (targetType == boolean.class || targetType == Boolean.class) {
      return (T) Boolean.valueOf(node.asBoolean());
    } else if (targetType == long.class || targetType == Long.class) {
      return (T) Long.valueOf(node.asLong());
    } else if (targetType == float.class || targetType == Float.class) {
      return (T) Float.valueOf((float) node.asDouble());
    } else if (targetType == double.class || targetType == Double.class) {
      return (T) Double.valueOf(node.asDouble());
    }

    // 处理大数字类型
    else if (targetType == BigInteger.class) {
      return (T) node.bigIntegerValue();
    } else if (targetType == BigDecimal.class) {
      return (T) node.decimalValue();
    }

    // 处理日期时间类型
    else if (targetType == Date.class) {
      return (T) new Date(node.asLong());
    } else if (targetType == java.sql.Date.class) {
      return (T) new java.sql.Date(node.asLong());
    } else if (targetType == java.sql.Timestamp.class) {
      return (T) new java.sql.Timestamp(node.asLong());
    } else if (targetType == LocalDate.class) {
      return (T) LocalDate.parse(node.asText());
    } else if (targetType == LocalTime.class) {
      return (T) LocalTime.parse(node.asText());
    } else if (targetType == LocalDateTime.class) {
      return (T) LocalDateTime.parse(node.asText());
    } else if (targetType == ZonedDateTime.class) {
      return (T) ZonedDateTime.parse(node.asText());
    } else if (targetType == Instant.class) {
      return (T) Instant.ofEpochMilli(node.asLong());
    }

    // 处理数组类型
    else if (targetType.isArray()) {
      return convertToArray(node, targetType);
    }

    // 不支持的类型抛出错误。
    throw new IllegalArgumentException("Unsupported target type: " + targetType.getName());
  }

  @SuppressWarnings("unchecked")
  private static <T> T convertToArray(JsonNode node, Class<T> targetType) {
    if (!node.isArray()) {
      throw new IllegalArgumentException("Expected array for array conversion");
    }

    Class<?> componentType = targetType.getComponentType();
    int size = node.size();
    Object array = java.lang.reflect.Array.newInstance(componentType, size);

    for (int i = 0; i < size; i++) {
      Object value = convertJsonNodeToJavaType(node.get(i), componentType);
      java.lang.reflect.Array.set(array, i, value);
    }

    return (T) array;
  }

  /**
   * 根据属性名称得到类的属性成员
   * 
   * @param clazz
   * @param fieldName
   * @return
   */
  public static Field getFieldByName(Class<?> clazz, String fieldName) {
    List<Field> fields = getAllFields(clazz);
    for (Field field : fields) {
      if (field.getName().equals(fieldName))
        return field;
    }
    return null;
  }

  /**
   * 获取类和其父类字段list
   * 
   * @param clazz
   * @return
   */
  public static List<Field> getAllFields(Class<?> clazz) {
    List<Field> fields = new ArrayList<>();
    while (clazz != null && !clazz.getName().startsWith("java.")) { // 遍历当前类及其所有父类
      Field[] declaredFields = clazz.getDeclaredFields();
      for (Field field : declaredFields) {
        fields.add(field);
      }
      clazz = clazz.getSuperclass(); // 获取父类
    }
    return fields;
  }

  /**
   * 从filter中得到需要显示的字段名
   * 
   * @param filter
   * @return
   */
  public static List<String> getFieldNamesFromFilter(JsonNode filter) {
    List<String> result = new ArrayList<String>();
    if (filter != null && filter.has("fields")) {
      JsonNode fieldsNode = filter.path("fields");
      if (fieldsNode.isArray()) {
        for (JsonNode fieldNode : fieldsNode) {
          if (fieldNode.isTextual())
            result.add(fieldNode.asText());
          else
            throw new IllegalArgumentException("The fields itme must be an string.");
        }
      } else
        throw new IllegalArgumentException("The fields must be string array list.");
    }
    return result;
  }

  /**
   * 从filter里得到需要关联的字段名称
   * 
   * @param filter
   * @return
   */
  public static List<String> getFieldNamesFromInclude(JsonNode filter) {
    List<String> result = new ArrayList<String>();

    if (filter != null && filter.has("include")) {
      JsonNode includeNode = filter.path("include");
      if (includeNode.isArray()) {
        for (JsonNode includeItem : includeNode) {
          if (includeItem.has("relation")) {
            JsonNode relatioNode = includeItem.path("relation");
            if (relatioNode.isTextual()) {
              result.add(relatioNode.asText());
            } else
              throw new IllegalArgumentException("The relation must be string. ");
          } else
            throw new IllegalArgumentException("The include item must has 'relation' ");
        }
      } else
        throw new IllegalArgumentException("The include must be array.");
    }

    return result;
  }

  /**
   * 得到所有字段名称
   * 
   * @param entity
   * @param isRelation
   * @return
   */
  public static List<String> getRelationFieldNamesFromEntity(Class<?> clazz, boolean isRelation) {
    List<String> result = new ArrayList<String>();

    List<Field> fields = getAllFields(clazz);
    for (Field field : fields) {
      field.setAccessible(true);
      String fieldName = field.getName();
      if (isRelationField(field) == isRelation)
        result.add(fieldName);
    }
    return result;
  }

  /**
   * 是否关联关系字段
   * 
   * @param field
   * @return
   */
  public static boolean isRelationField(Field field) {
    return field.isAnnotationPresent(OneToMany.class) ||
        field.isAnnotationPresent(ManyToOne.class) ||
        field.isAnnotationPresent(OneToOne.class) ||
        field.isAnnotationPresent(ManyToMany.class);
  }

  /**
   * 得到需要显示的字段名
   * 
   * @param selectedFieldNames
   * @param includeFieldNames
   * @param relationFieldNames
   * @param noRelationFieldNames
   * @return
   */
  public static List<String> getShowFieldNames(List<String> selectedFieldNames, List<String> includeFieldNames,
      List<String> relationFieldNames, List<String> noRelationFieldNames) {
    List<String> result = new ArrayList<String>();

    if (!noRelationFieldNames.containsAll(selectedFieldNames))
      throw new IllegalArgumentException("The fields have unkown field.");

    if (!relationFieldNames.containsAll(includeFieldNames))
      throw new IllegalArgumentException("The include have unkown relation field.");

    if (selectedFieldNames.size() == 0) {
      if (includeFieldNames.size() == 0) {
        result = noRelationFieldNames;
      } else {
        noRelationFieldNames.addAll(includeFieldNames);
        result = noRelationFieldNames;
      }
    } else {
      if (includeFieldNames.size() == 0) {
        result = selectedFieldNames;
      } else {
        selectedFieldNames.addAll(includeFieldNames);
        result = selectedFieldNames;
      }
    }
    return result;
  }

  public static JsonNode getIncludeScope(String relationName, JsonNode parent) {
    if (parent.has("include")) {
      JsonNode includeNode = parent.path("include");
      if (includeNode.isArray()) {
        for (JsonNode inc : includeNode) {
          if (inc.has("relation")) {
            JsonNode reNode = inc.path("realtion");
            if (reNode.asText() == relationName) {
              if (reNode.has("scope")) {
                return reNode.path("scope");
              }
            }
          }
        }
      }
    }
    return null;
  }
}
