package prog2.servicioturnos;

import org.springframework.boot.SpringApplication;

public class TestServicioTurnosApplication {

    public static void main(String[] args) {
        SpringApplication.from(ServicioTurnosApplication::main).with(TestcontainersConfiguration.class).run(args);
    }

}
