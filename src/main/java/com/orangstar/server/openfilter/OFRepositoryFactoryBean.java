package com.orangstar.server.openfilter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactoryBean;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.core.support.RepositoryFactorySupport;
import org.springframework.lang.NonNull;

import com.orangstar.server.service.OFService;

import jakarta.persistence.EntityManager;

public class OFRepositoryFactoryBean<T extends Repository<S, ID>, S, ID> extends JpaRepositoryFactoryBean<T, S, ID> {
  @Autowired
  private OFService ofService;

  public OFRepositoryFactoryBean(Class<? extends T> repositoryInterface) {
    super(repositoryInterface);
  }

  @Override
  @NonNull
  protected RepositoryFactorySupport createRepositoryFactory(@NonNull EntityManager entityManager) {
    return new OFRepositoryFactory(entityManager, ofService);
  }

}
