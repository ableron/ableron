package io.github.ableron

import org.testcontainers.containers.GenericContainer
import org.testcontainers.images.builder.ImageFromDockerfile

import java.nio.file.Path

class NodeJs22EsModulesSpec extends BaseSpec {

  @Override
  GenericContainer getContainerUnderTest() {
    return new GenericContainer<>(new ImageFromDockerfile()
      .withDockerfile(Path.of("..", "Dockerfile-verify-nodejs22-esm")))
  }
}
