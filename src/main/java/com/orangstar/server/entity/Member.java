package com.orangstar.server.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table
public class Member extends BaseEntity {
  private String workNum;

  public String getWorkNum() {
    return workNum;
  }

  public void setWorkNum(String workNum) {
    this.workNum = workNum;
  }

  @Override
  public String toString() {
    return super.toString() + "Member [workNum=" + workNum + "]";
  }

}
