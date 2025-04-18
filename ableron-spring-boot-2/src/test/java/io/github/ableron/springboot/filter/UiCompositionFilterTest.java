package io.github.ableron.springboot.filter;

import io.github.ableron.*;
import io.github.ableron.springboot.autoconfigure.AbleronAutoConfiguration;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ContextConfiguration;

import javax.servlet.*;
import javax.servlet.http.HttpServlet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;

@ContextConfiguration(classes = AbleronAutoConfiguration.class)
public class UiCompositionFilterTest {

  @Test
  public void shouldApplyUiComposition() throws ServletException, IOException {
    // given
    Ableron ableron = new Ableron(AbleronConfig.builder().build());
    UiCompositionFilter uiCompositionFilter = new UiCompositionFilter(ableron);
    MockHttpServletRequest request = new MockHttpServletRequest();
    MockHttpServletResponse response = new MockHttpServletResponse();
    MockFilterChain filterChain = new MockFilterChain(mock(HttpServlet.class), uiCompositionFilter, new OutputGeneratingFilter(
      "<ableron-include src=\"foo\">fallback</ableron-include>"
    ));

    // when
    filterChain.doFilter(request, response);

    // then
    assertEquals("fallback", response.getContentAsString());
    assertEquals(8, response.getHeaderValue(HttpHeaders.CONTENT_LENGTH));
  }

  @Test
  public void shouldNotApplyUiCompositionIfDisabled() throws ServletException, IOException {
    // given
    Ableron ableron = new Ableron(AbleronConfig.builder().enabled(false).build());
    UiCompositionFilter uiCompositionFilter = new UiCompositionFilter(ableron);
    MockHttpServletRequest request = new MockHttpServletRequest();
    MockHttpServletResponse response = new MockHttpServletResponse();
    MockFilterChain filterChain = new MockFilterChain(mock(HttpServlet.class), uiCompositionFilter, new OutputGeneratingFilter(
      "<ableron-include src=\"foo\">fallback</ableron-include>"
    ));

    // when
    filterChain.doFilter(request, response);

    // then
    assertEquals("<ableron-include src=\"foo\">fallback</ableron-include>", response.getContentAsString());
  }

  @Test
  public void shouldNotApplyUiCompositionIfContentTypeIsNotTextHtml() throws ServletException, IOException {
    // given
    Ableron ableron = new Ableron(AbleronConfig.builder().build());
    UiCompositionFilter uiCompositionFilter = new UiCompositionFilter(ableron);
    MockHttpServletRequest request = new MockHttpServletRequest();
    MockHttpServletResponse response = new MockHttpServletResponse();
    MockFilterChain filterChain = new MockFilterChain(mock(HttpServlet.class), uiCompositionFilter, new OutputGeneratingFilter(
      "<ableron-include src=\"foo\">fallback</ableron-include>", "application/json"
    ));

    // when
    filterChain.doFilter(request, response);

    // then
    assertEquals("<ableron-include src=\"foo\">fallback</ableron-include>", response.getContentAsString());
  }

  @Test
  public void shouldSetContentLengthHeaderToZeroForEmptyResponse() throws ServletException, IOException {
    // given
    var ableron = new Ableron(AbleronConfig.builder().build());
    var uiCompositionFilter = new UiCompositionFilter(ableron);
    var request = new MockHttpServletRequest();
    var response = new MockHttpServletResponse();
    var filterChain = new MockFilterChain(mock(HttpServlet.class), uiCompositionFilter, new OutputGeneratingFilter(
      "<ableron-include />"
    ));

    // when
    filterChain.doFilter(request, response);

    // then
    assertEquals("", response.getContentAsString());
    assertEquals(0, response.getHeaderValue(HttpHeaders.CONTENT_LENGTH));
  }

  @Test
  public void shouldPassStatusCodeAndResponseHeadersFromPrimaryInclude() throws ServletException, IOException {
    // given
    var transclusionResult = new TransclusionResult("");
    transclusionResult.addResolvedInclude(
      new Include("", Map.of("primary", "")).resolveWith(
        new Fragment(null, 503, "content", Instant.EPOCH, Map.of(HttpHeaders.CONTENT_LANGUAGE, List.of("en"))),
        0,
        "src"
      )
    );
    var ableron = Mockito.mock(Ableron.class);
    Mockito.when(ableron.resolveIncludes(any(), any())).thenReturn(transclusionResult);
    var uiCompositionFilter = new UiCompositionFilter(ableron);
    var request = new MockHttpServletRequest();
    var response = new MockHttpServletResponse();
    var filterChain = new MockFilterChain(mock(HttpServlet.class), uiCompositionFilter, new OutputGeneratingFilter(
      "<ableron-include src=\"foo\" primary>fallback</ableron-include>"
    ));

    // when
    filterChain.doFilter(request, response);

    // then
    assertEquals(503, response.getStatus());
    assertEquals("en", response.getHeaderValue(HttpHeaders.CONTENT_LANGUAGE));
  }

  @Test
  public void shouldReducePageExpirationTimeBasedOnNonCacheableFragment() throws ServletException, IOException {
    // given
    var transclusionResult = new TransclusionResult("");
    transclusionResult.addResolvedInclude(
      new Include("").resolveWith(
        new Fragment(null, 200, "content", Instant.EPOCH, Map.of()),
        0,
        "src"
      )
    );
    var ableron = Mockito.mock(Ableron.class);
    Mockito.when(ableron.resolveIncludes(any(), any())).thenReturn(transclusionResult);
    var uiCompositionFilter = new UiCompositionFilter(ableron);
    var request = new MockHttpServletRequest();
    var response = new MockHttpServletResponse();
    response.addHeader(HttpHeaders.CACHE_CONTROL, "max-age=600");
    var filterChain = new MockFilterChain(mock(HttpServlet.class), uiCompositionFilter, new OutputGeneratingFilter(
      "<ableron-include src=\"foo\"/>"
    ));

    // when
    filterChain.doFilter(request, response);

    // then
    assertEquals(200, response.getStatus());
    assertEquals("no-store", response.getHeaderValue(HttpHeaders.CACHE_CONTROL));
  }

  @Test
  public void shouldReducePageExpirationTimeBasedOnFragmentWithLowerExpirationTime() throws ServletException, IOException {
    // given
    var transclusionResult = new TransclusionResult("");
    transclusionResult.addResolvedInclude(
      new Include("").resolveWith(
        new Fragment(null, 200, "content", Instant.now().plusSeconds(300), Map.of()),
        0,
        "src"
      )
    );
    var ableron = Mockito.mock(Ableron.class);
    Mockito.when(ableron.resolveIncludes(any(), any())).thenReturn(transclusionResult);
    var uiCompositionFilter = new UiCompositionFilter(ableron);
    var request = new MockHttpServletRequest();
    var response = new MockHttpServletResponse();
    response.addHeader(HttpHeaders.CACHE_CONTROL, "max-age=600");
    var filterChain = new MockFilterChain(mock(HttpServlet.class), uiCompositionFilter, new OutputGeneratingFilter(
      "<ableron-include src=\"foo\"/>"
    ));

    // when
    filterChain.doFilter(request, response);

    // then
    assertEquals(200, response.getStatus());
    assertEquals("max-age=300", response.getHeaderValue(HttpHeaders.CACHE_CONTROL));
  }

  @Test
  public void shouldNotReducePageExpirationTimeIfLowerThanFragmentExpirationTime() throws ServletException, IOException {
    // given
    var transclusionResult = new TransclusionResult("");
    transclusionResult.addResolvedInclude(
      new Include("").resolveWith(
        new Fragment(null, 200, "content", Instant.now().plusSeconds(900), Map.of()),
        0,
        "src"
      )
    );
    var ableron = Mockito.mock(Ableron.class);
    Mockito.when(ableron.resolveIncludes(any(), any())).thenReturn(transclusionResult);
    var uiCompositionFilter = new UiCompositionFilter(ableron);
    var request = new MockHttpServletRequest();
    var response = new MockHttpServletResponse();
    response.addHeader(HttpHeaders.CACHE_CONTROL, "max-age=600");
    var filterChain = new MockFilterChain(mock(HttpServlet.class), uiCompositionFilter, new OutputGeneratingFilter(
      "<ableron-include src=\"foo\"/>"
    ));

    // when
    filterChain.doFilter(request, response);

    // then
    assertEquals(200, response.getStatus());
    assertEquals("max-age=600", response.getHeaderValue(HttpHeaders.CACHE_CONTROL));
  }

  static class OutputGeneratingFilter implements Filter {

    private final String content;
    private final String contentType;

    public OutputGeneratingFilter(String content) {
      this(content, MediaType.TEXT_HTML_VALUE);
    }

    public OutputGeneratingFilter(String content, String contentType) {
      this.content = content;
      this.contentType = contentType;
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException {
      servletResponse.setContentType(contentType);
      servletResponse.getOutputStream().print(content);
      servletResponse.setContentLength(content.length());
    }
  }
}
