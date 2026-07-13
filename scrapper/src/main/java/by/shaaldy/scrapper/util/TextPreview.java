package by.shaaldy.scrapper.util;

/** Превью текста для сообщения об обновлении: снять HTML-теги и обрезать до 200 символов. */
public final class TextPreview {

  private static final int MAX = 200;

  private TextPreview() {}

  public static String preview(String text) {
    if (text == null || text.isBlank()) {
      return "";
    }
    String stripped = text.replaceAll("<[^>]*>", "").strip();
    return stripped.length() > MAX ? stripped.substring(0, MAX) + "..." : stripped;
  }
}
