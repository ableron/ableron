package io.github.ableron.springboot.autoconfigure;

import io.github.ableron.AbleronConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(
  classes = { AbleronAutoConfiguration.class },
  properties = { "ableron.fragment-request-timeout=4s" }
)
public class FragmentRequestTimeoutPropertyTest {

  @Autowired
  private AbleronConfig ableronConfig;

  @Test
  public void shouldInterpretSimpleDurationFormatting() {
    assertEquals(Duration.ofSeconds(4), ableronConfig.getFragmentRequestTimeout());
  }
}
