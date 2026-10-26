package prog2.servicioturnos;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class EndpointSaludTests {

    @Autowired
    private MockMvcTester mvc;

    @Test
    void respondeUpSinMostrarDetalles() {
        MvcTestResult respuesta = mvc.get().uri("/actuator/health").exchange();

        assertThat(respuesta).hasStatusOk();
        assertThat(respuesta).bodyJson().extractingPath("$.status").isEqualTo("UP");
        assertThat(respuesta).bodyJson().doesNotHavePath("$.components");
    }

}
