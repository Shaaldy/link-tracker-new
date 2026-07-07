package by.shaaldy.bot.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import by.shaaldy.bot.dto.scrapper.ApiErrorResponse;
import by.shaaldy.bot.dto.scrapper.ListLinksResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;
import org.springframework.test.web.client.MockRestServiceServer;

class ScrapperClientErrorHandlingTest {

    private static final String BASE_URL = "http://scrapper";
    private final ObjectMapper objectMapper = new ObjectMapper();

    private MockRestServiceServer server;
    private ScrapperClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder =
                RestClient.builder()
                        .baseUrl(BASE_URL)
                        .defaultStatusHandler(
                                HttpStatusCode::isError,
                                (request, response) -> {
                                    ApiErrorResponse body =
                                            readError(objectMapper, response.getBody(), response.getStatusCode());
                                    throw new ScrapperApiException(response.getStatusCode(), body);
                                });

        server = MockRestServiceServer.bindTo(builder).build();

        RestClientAdapter adapter = RestClientAdapter.create(builder.build());
        client = HttpServiceProxyFactory.builderFor(adapter).build().createClient(ScrapperClient.class);
    }

    @Test
    void listLinks_notFound_throwsScrapperApiExceptionWithDescription() {
        server
                .expect(requestTo(BASE_URL + "/links"))
                .andRespond(
                        withStatus(HttpStatus.NOT_FOUND)
                                .contentType(MediaType.APPLICATION_JSON)
                                .body("{\"description\":\"Чат не найден\"}"));

        assertThatThrownBy(() -> client.listLinks(1L))
                .isInstanceOf(ScrapperApiException.class)
                .satisfies(
                        ex -> {
                            ScrapperApiException e = (ScrapperApiException) ex;
                            assertThat(e.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                            assertThat(e.userMessage()).isEqualTo("Чат не найден");
                        });
    }

    @Test
    void listLinks_errorWithUnparseableBody_throwsWithFallbackDescription() {
        server
                .expect(requestTo(BASE_URL + "/links"))
                .andRespond(
                        withStatus(HttpStatus.INTERNAL_SERVER_ERROR)
                                .contentType(MediaType.APPLICATION_JSON)
                                .body("не json"));

        assertThatThrownBy(() -> client.listLinks(1L))
                .isInstanceOf(ScrapperApiException.class)
                .satisfies(ex -> assertThat(((ScrapperApiException) ex).userMessage()).contains("Ошибка сервиса"));
    }

    @Test
    void listLinks_success_returnsBodyWithoutThrowing() {
        server
                .expect(requestTo(BASE_URL + "/links"))
                .andRespond(
                        withSuccess("{\"links\":[],\"size\":0}", MediaType.APPLICATION_JSON));

        ListLinksResponse response = client.listLinks(1L);

        assertThat(response.getSize()).isEqualTo(0);
    }

    private static ApiErrorResponse readError(
            ObjectMapper mapper, java.io.InputStream body, HttpStatusCode status) {
        try {
            return mapper.readValue(body, ApiErrorResponse.class);
        } catch (IOException e) {
            return new ApiErrorResponse().description("Ошибка сервиса (" + status + ")");
        }
    }
}