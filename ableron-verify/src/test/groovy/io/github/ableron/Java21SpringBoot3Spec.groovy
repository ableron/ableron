package io.github.ableron

import org.testcontainers.containers.GenericContainer
import org.testcontainers.images.builder.ImageFromDockerfile

import java.nio.file.Path

class Java21SpringBoot3Spec extends BaseSpec {

  @Override
  GenericContainer getContainerUnderTest() {
    return new GenericContainer<>(new ImageFromDockerfile()
      .withDockerfile(Path.of("..", "Dockerfile-verify-java21-spring-boot-3")))
  }
}
