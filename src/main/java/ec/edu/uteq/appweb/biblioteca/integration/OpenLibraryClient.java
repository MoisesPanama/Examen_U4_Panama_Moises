package ec.edu.uteq.appweb.biblioteca.integration;

import ec.edu.uteq.appweb.biblioteca.config.CacheConfig;
import ec.edu.uteq.appweb.biblioteca.exception.ServicioExternoException;
import java.util.Optional;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class OpenLibraryClient {

    private final RestClient restClient;

    public OpenLibraryClient(RestClient restClientExterno) {
        this.restClient = restClientExterno;
    }

    @Cacheable(value = CacheConfig.CACHE_OPENLIBRARY, key = "#isbn", unless = "#result == null || #result.isEmpty()")
    public Optional<OpenLibraryResponse> consultarPorIsbn(String isbn) {
        try {
            OpenLibraryResponse respuesta = restClient.get()
                    .uri("/isbn/{isbn}.json", isbn)
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, (peticion, respuestaHttp) -> {
                        if (respuestaHttp.getStatusCode().value() == 404) {
                            return;
                        }
                        throw new ServicioExternoException("Open Library respondio con error " + respuestaHttp.getStatusCode().value());
                    })
                    .onStatus(HttpStatusCode::is5xxServerError, (peticion, respuestaHttp) -> {
                        throw new ServicioExternoException("Open Library respondio con error " + respuestaHttp.getStatusCode().value());
                    })
                    .body(OpenLibraryResponse.class);
            return Optional.ofNullable(respuesta);
        } catch (ServicioExternoException e) {
            throw e;
        } catch (Exception e) {
            throw new ServicioExternoException("Error al conectar con Open Library: " + e.getMessage(), e);
        }
    }
}
