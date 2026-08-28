package ec.edu.uteq.appweb.biblioteca.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ec.edu.uteq.appweb.biblioteca.BaseIntegracionTest;
import ec.edu.uteq.appweb.biblioteca.domain.Usuario;
import ec.edu.uteq.appweb.biblioteca.repository.UsuarioRepository;
import ec.edu.uteq.appweb.biblioteca.security.JwtService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

class LibroControllerIT extends BaseIntegracionTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UsuarioRepository usuarios;

    private String tokenAdmin() {
        Usuario admin = usuarios.findByUsernameAndActivoTrue("admin")
                .orElseThrow(() -> new RuntimeException("Usuario admin no encontrado en semilla"));
        return jwtService.generar(admin);
    }

    private String tokenLector() {
        Usuario lector = usuarios.findByUsernameAndActivoTrue("lector")
                .orElseThrow(() -> new RuntimeException("Usuario lector no encontrado en semilla"));
        return jwtService.generar(lector);
    }

    @Test
    @DisplayName("GET /api/v1/libros responde 200 con envoltorio y metadatos de paginacion")
    void listarLibrosDevuelveEnvoltorio() throws Exception {
        mockMvc.perform(get("/api/v1/libros").param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.errors").isArray())
                .andExpect(jsonPath("$.meta.page").value(0))
                .andExpect(jsonPath("$.meta.size").value(5))
                .andExpect(jsonPath("$.meta.totalElements").isNumber());
    }

    @Test
    @DisplayName("GET /api/v1/libros con filtro titulo devuelve libros que coinciden")
    void listarLibrosConFiltroTitulo() throws Exception {
        mockMvc.perform(get("/api/v1/libros")
                        .param("titulo", "clean")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("GET /api/v1/libros con filtro categoriaId filtra correctamente")
    void listarLibrosConFiltroCategoria() throws Exception {
        mockMvc.perform(get("/api/v1/libros")
                        .param("categoriaId", "1")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("GET /api/v1/libros/999999 responde 404 con ProblemDetail")
    void libroInexistenteDevuelveProblemDetail() throws Exception {
        mockMvc.perform(get("/api/v1/libros/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Recurso no encontrado"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").exists());
    }

    @Test
    @DisplayName("GET /api/v1/libros/1 responde 200 con datos del libro")
    void obtenerLibroPorIdDevuelveDatos() throws Exception {
        mockMvc.perform(get("/api/v1/libros/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.isbn").exists())
                .andExpect(jsonPath("$.data.titulo").exists())
                .andExpect(jsonPath("$.data.anioPublicacion").isNumber())
                .andExpect(jsonPath("$.data.ejemplaresTotales").isNumber())
                .andExpect(jsonPath("$.data.autorNombre").exists())
                .andExpect(jsonPath("$.data.editorialNombre").exists())
                .andExpect(jsonPath("$.data.categoriaNombre").exists());
    }

    @Test
    @DisplayName("POST /api/v1/libros con titulo vacio responde 400 con errores de validacion")
    void crearLibroConTituloVacioDevuelveErrores() throws Exception {
        mockMvc.perform(post("/api/v1/libros")
                        .header("Authorization", "Bearer " + tokenAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"isbn\":\"9781234567890\",\"titulo\":\"\",\"anioPublicacion\":2020,\"ejemplaresTotales\":1,\"autorId\":1,\"editorialId\":1,\"categoriaId\":1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors").isArray())
                .andExpect(jsonPath("$.errors").isNotEmpty());
    }

    @Test
    @DisplayName("POST /api/v1/libros sin Authorization responde 401")
    void crearLibroSinTokenDevuelve401() throws Exception {
        mockMvc.perform(post("/api/v1/libros")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"isbn\":\"9781234567890\",\"titulo\":\"Test\",\"anioPublicacion\":2020,\"ejemplaresTotales\":1,\"autorId\":1,\"editorialId\":1,\"categoriaId\":1}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /api/v1/libros con rol LECTOR responde 403")
    void crearLibroConRolLectorDevuelve403() throws Exception {
        mockMvc.perform(post("/api/v1/libros")
                        .header("Authorization", "Bearer " + tokenLector())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"isbn\":\"9781234567890\",\"titulo\":\"Test\",\"anioPublicacion\":2020,\"ejemplaresTotales\":1,\"autorId\":1,\"editorialId\":1,\"categoriaId\":1}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/v1/libros con ISBN duplicado responde 409")
    void crearLibroConIsbnDuplicadoDevuelve409() throws Exception {
        mockMvc.perform(post("/api/v1/libros")
                        .header("Authorization", "Bearer " + tokenAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"isbn\":\"9780134685991\",\"titulo\":\"Duplicado\",\"anioPublicacion\":2020,\"ejemplaresTotales\":1,\"autorId\":1,\"editorialId\":1,\"categoriaId\":1}"))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("PUT /api/v1/libros/1 actualiza el titulo y responde 200")
    void actualizarLibroDevuelve200() throws Exception {
        mockMvc.perform(put("/api/v1/libros/1")
                        .header("Authorization", "Bearer " + tokenAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"isbn\":\"9780134685991\",\"titulo\":\"Titulo Actualizado\",\"anioPublicacion\":2020,\"ejemplaresTotales\":5,\"autorId\":1,\"editorialId\":1,\"categoriaId\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.titulo").value("Titulo Actualizado"));
    }

    @Test
    @DisplayName("DELETE /api/v1/libros/1 responde 204 y desactiva el libro")
    void desactivarLibroDevuelve204() throws Exception {
        mockMvc.perform(delete("/api/v1/libros/1")
                        .header("Authorization", "Bearer " + tokenAdmin()))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("DELETE /api/v1/libros/1 sin ADMIN responde 403")
    void desactivarLibroSinRolAdminDevuelve403() throws Exception {
        mockMvc.perform(delete("/api/v1/libros/1")
                        .header("Authorization", "Bearer " + tokenLector()))
                .andExpect(status().isForbidden());
    }
}
