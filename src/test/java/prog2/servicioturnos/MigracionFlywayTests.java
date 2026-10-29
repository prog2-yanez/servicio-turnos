package prog2.servicioturnos;

import static org.assertj.core.api.Assertions.assertThat;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfoService;
import org.flywaydb.core.api.MigrationState;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class MigracionFlywayTests {

    @Autowired
    private Flyway flyway;

    @Test
    void aplicaLaMigracionInicialYNoQuedanPendientes() {
        MigrationInfoService info = flyway.info();

        assertThat(info.applied())
                .anySatisfy(migracion -> {
                    assertThat(migracion.getVersion().getVersion()).isEqualTo("1");
                    assertThat(migracion.getState()).isEqualTo(MigrationState.SUCCESS);
                });
        assertThat(info.pending()).isEmpty();
    }

}
