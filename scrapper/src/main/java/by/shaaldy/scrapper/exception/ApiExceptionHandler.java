package by.shaaldy.scrapper.exception;

import by.shaaldy.scrapper.dto.scrapper.ApiErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(ChatNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleChatNotFound(ChatNotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, ex);
    }

    @ExceptionHandler(LinkNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleLinkNotFound(LinkNotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, ex);
    }

    @ExceptionHandler(ChatAlreadyExistsException.class)
    public ResponseEntity<ApiErrorResponse> handleChatExists(ChatAlreadyExistsException ex) {
        return build(HttpStatus.CONFLICT, ex);
    }

    @ExceptionHandler(LinkAlreadyTrackedException.class)
    public ResponseEntity<ApiErrorResponse> handleLinkTracked(LinkAlreadyTrackedException ex) {
        return build(HttpStatus.CONFLICT, ex);
    }

    private ResponseEntity<ApiErrorResponse> build(HttpStatus status, RuntimeException ex) {
        ApiErrorResponse body =
                new ApiErrorResponse()
                        .description(ex.getMessage())
                        .code(String.valueOf(status.value()))
                        .exceptionName(ex.getClass().getSimpleName())
                        .exceptionMessage(ex.getMessage());
        return ResponseEntity.status(status).body(body);
    }
}