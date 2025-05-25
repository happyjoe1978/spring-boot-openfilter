package com.orangstar.server.entity;

import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.ConstraintMode;
import jakarta.persistence.Entity;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table
public class Department extends BaseEntity {
  private String code;

  public String getCode() {
    return code;
  }

  public void setCode(String code) {
    this.code = code;
  }

  @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
  @JoinColumn(name = "department_id", foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
  private List<Member> members;

  @Override
  public String toString() {
    return super.toString() + "Department [code=" + code + "]";
  }

}
