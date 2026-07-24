package by.shaaldy.scrapper.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TextPreviewTest {

  @Test
  void preview_stripsHtmlTags() {
    assertThat(TextPreview.preview("<p>Hello <b>world</b></p>")).isEqualTo("Hello world");
  }

  @Test
  void preview_truncatesTo200WithEllipsis() {
    String long300 = "a".repeat(300);
    String result = TextPreview.preview(long300);
    assertThat(result).hasSize(203).endsWith("...");
  }

  @Test
  void preview_nullOrBlank_returnsEmpty() {
    assertThat(TextPreview.preview(null)).isEmpty();
    assertThat(TextPreview.preview("   ")).isEmpty();
  }
}
