package com.orangstar.server.openfilter;

import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import org.springframework.data.jpa.repository.support.JpaRepositoryImplementation;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.core.RepositoryMetadata;

import com.orangstar.server.service.OFService;

import io.micrometer.common.lang.NonNull;
import jakarta.persistence.EntityManager;

public class OFRepositoryFactory extends JpaRepositoryFactory {
  private OFService ofService;

  public OFRepositoryFactory(EntityManager entityManager, OFService oService) {
    super(entityManager);
    ofService = oService;
  }

  @Override
  @NonNull
  protected JpaRepositoryImplementation<?, ?> getTargetRepository(@NonNull RepositoryInformation information,
      @NonNull EntityManager entityManager) {
    // 检查是否有自定义注解
    OFRepository annotation = information.getRepositoryInterface()
        .getAnnotation(OFRepository.class);
    if (annotation != null) {
      // 如果有自定义注解，使用自定义实现类
      // System.out.println("自定义实现类。" + queryService);
      return new OpenFilterRepositoryImpl<>(information.getDomainType(), entityManager, ofService);
    }
    // 使用默认实现
    return super.getTargetRepository(information, entityManager);
  }

  @Override
  @NonNull
  protected Class<?> getRepositoryBaseClass(@NonNull RepositoryMetadata metadata) {
    // 如果接口有自定义注解，使用自定义实现类
    if (metadata.getRepositoryInterface().isAnnotationPresent(OFRepository.class)) {
      return OpenFilterRepositoryImpl.class;
    }
    return super.getRepositoryBaseClass(metadata);
  }

}
