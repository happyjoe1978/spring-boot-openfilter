package com.orangstar.server.openfilter;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EnableJpaRepositories(repositoryFactoryBeanClass = OFRepositoryFactoryBean.class, basePackages = "com.orangstar.server.repository")
public class OpenFilterConfig {

}
