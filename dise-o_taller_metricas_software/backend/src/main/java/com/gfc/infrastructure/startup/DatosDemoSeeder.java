package com.gfc.infrastructure.startup;

import com.gfc.domain.model.Cliente;
import com.gfc.domain.model.Comida;
import com.gfc.domain.model.Ingrediente;
import com.gfc.domain.model.Rol;
import com.gfc.domain.model.TipoDocumento;
import com.gfc.domain.model.UnidadMedida;
import com.gfc.domain.model.Usuario;
import com.gfc.domain.port.CodificadorContrasena;
import com.gfc.infrastructure.config.GfcSeedProperties;
import com.gfc.infrastructure.persistence.ClienteRepository;
import com.gfc.infrastructure.persistence.ComidaRepository;
import com.gfc.infrastructure.persistence.IngredienteRepository;
import com.gfc.infrastructure.persistence.UsuarioRepository;
import java.math.BigDecimal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DatosDemoSeeder implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(DatosDemoSeeder.class);

    private final UsuarioRepository usuarioRepository;
    private final ClienteRepository clienteRepository;
    private final ComidaRepository comidaRepository;
    private final IngredienteRepository ingredienteRepository;
    private final CodificadorContrasena codificador;
    private final GfcSeedProperties props;

    public DatosDemoSeeder(UsuarioRepository usuarioRepository, ClienteRepository clienteRepository,
                            ComidaRepository comidaRepository, IngredienteRepository ingredienteRepository,
                            CodificadorContrasena codificador, GfcSeedProperties props) {
        this.usuarioRepository = usuarioRepository;
        this.clienteRepository = clienteRepository;
        this.comidaRepository = comidaRepository;
        this.ingredienteRepository = ingredienteRepository;
        this.codificador = codificador;
        this.props = props;
    }

    @Override
    public void run(String... args) {
        sembrarUsuarios();
        sembrarCatalogo();
    }

    private void sembrarUsuarios() {
        if (usuarioRepository.findByNombreUsuario(props.empleado().usuario()).isEmpty()) {
            usuarioRepository.save(new Usuario(null, props.empleado().usuario(), props.empleado().clave(),
                    Rol.EMPLEADO, codificador));
            log.info("Usuario demo creado -> '{}' (EMPLEADO)", props.empleado().usuario());
        }

        if (usuarioRepository.findByNombreUsuario(props.cliente().usuario()).isEmpty()) {
            clienteRepository.save(new Cliente(null, props.cliente().usuario(), props.cliente().clave(),
                    TipoDocumento.CC, props.cliente().documento(), props.cliente().email(),
                    props.cliente().nombre(), codificador));
            log.info("Usuario demo creado -> '{}' (CLIENTE)", props.cliente().usuario());
        }

        if (usuarioRepository.findByNombreUsuario(props.empresa().usuario()).isEmpty()) {
            clienteRepository.save(new Cliente(null, props.empresa().usuario(), props.empresa().clave(),
                    TipoDocumento.NIT, props.empresa().documento(), props.empresa().email(),
                    props.empresa().nombre(), codificador));
            log.info("Usuario demo creado -> '{}' (CLIENTE/EMPRESA)", props.empresa().usuario());
        }
    }

    private void sembrarCatalogo() {
        if (comidaRepository.count() > 0) {
            return;
        }

        Ingrediente queso = ingredienteRepository.save(new Ingrediente(null, "Queso", 100, UnidadMedida.UNIDAD));
        Ingrediente pan = ingredienteRepository.save(new Ingrediente(null, "Pan", 100, UnidadMedida.UNIDAD));
        Ingrediente carneRes = ingredienteRepository.save(new Ingrediente(null, "Carne de res", 5000, UnidadMedida.GRAMO));
        Ingrediente lechuga = ingredienteRepository.save(new Ingrediente(null, "Lechuga", 200, UnidadMedida.UNIDAD));
        Ingrediente tomate = ingredienteRepository.save(new Ingrediente(null, "Tomate", 3000, UnidadMedida.GRAMO));
        Ingrediente salchicha = ingredienteRepository.save(new Ingrediente(null, "Salchicha", 100, UnidadMedida.UNIDAD));
        Ingrediente cebolla = ingredienteRepository.save(new Ingrediente(null, "Cebolla", 2000, UnidadMedida.GRAMO));
        Ingrediente salsaTomate = ingredienteRepository.save(new Ingrediente(null, "Salsa de tomate", 5000, UnidadMedida.MILILITRO));
        Ingrediente papas = ingredienteRepository.save(new Ingrediente(null, "Papas", 5000, UnidadMedida.GRAMO));
        Ingrediente pepperoni = ingredienteRepository.save(new Ingrediente(null, "Pepperoni", 1000, UnidadMedida.GRAMO));
        Ingrediente pepinillos = ingredienteRepository.save(new Ingrediente(null, "Pepinillos", 100, UnidadMedida.UNIDAD));
        Ingrediente masa = ingredienteRepository.save(new Ingrediente(null, "Masa", 50, UnidadMedida.UNIDAD));
        Ingrediente tocineta = ingredienteRepository.save(new Ingrediente(null, "Tocineta", 1000, UnidadMedida.GRAMO));
        Ingrediente champinones = ingredienteRepository.save(new Ingrediente(null, "Championes", 1000, UnidadMedida.GRAMO));
        Ingrediente jalapenos = ingredienteRepository.save(new Ingrediente(null, "Jalapenos", 500, UnidadMedida.GRAMO));
        Ingrediente pollo = ingredienteRepository.save(new Ingrediente(null, "Pollo desmechado", 3000, UnidadMedida.GRAMO));
        Ingrediente pina = ingredienteRepository.save(new Ingrediente(null, "Pina", 50, UnidadMedida.UNIDAD));
        Ingrediente alitas = ingredienteRepository.save(new Ingrediente(null, "Alitas", 100, UnidadMedida.UNIDAD));
        Ingrediente salsaPicante = ingredienteRepository.save(new Ingrediente(null, "Salsa picante", 2000, UnidadMedida.MILILITRO));

        Comida hamburguesaClasica = new Comida(null, "Hamburguesa Clasica", new BigDecimal("18000"), "/imagenes/hamburguesa-clasica.webp");
        hamburguesaClasica.configurarIngrediente(null, pan, 1, true, false, false, new BigDecimal("0"));
        hamburguesaClasica.configurarIngrediente(null, carneRes, 200, true, true, false, new BigDecimal("4000"));
        hamburguesaClasica.configurarIngrediente(null, queso, 2, true, true, true, new BigDecimal("1500"));
        hamburguesaClasica.configurarIngrediente(null, lechuga, 1, true, true, true, new BigDecimal("500"));
        hamburguesaClasica.configurarIngrediente(null, tomate, 100, true, true, true, new BigDecimal("500"));
        hamburguesaClasica.configurarIngrediente(null, pepinillos, 1, true, true, true, new BigDecimal("500"));
        hamburguesaClasica.configurarIngrediente(null, tocineta, 0, false, true, false, new BigDecimal("2500"));
        hamburguesaClasica.configurarIngrediente(null, jalapenos, 0, false, true, false, new BigDecimal("800"));
        comidaRepository.save(hamburguesaClasica);

        Comida hamburguesaTocineta = new Comida(null, "Hamburguesa Tocineta", new BigDecimal("24000"), "/imagenes/hamburguesa-tocineta.webp");
        hamburguesaTocineta.configurarIngrediente(null, pan, 1, true, false, false, new BigDecimal("0"));
        hamburguesaTocineta.configurarIngrediente(null, carneRes, 200, true, true, false, new BigDecimal("4000"));
        hamburguesaTocineta.configurarIngrediente(null, tocineta, 80, true, true, true, new BigDecimal("2500"));
        hamburguesaTocineta.configurarIngrediente(null, queso, 1, true, true, true, new BigDecimal("1500"));
        hamburguesaTocineta.configurarIngrediente(null, lechuga, 1, true, true, true, new BigDecimal("500"));
        hamburguesaTocineta.configurarIngrediente(null, tomate, 100, true, true, true, new BigDecimal("500"));
        hamburguesaTocineta.configurarIngrediente(null, jalapenos, 0, false, true, false, new BigDecimal("800"));
        comidaRepository.save(hamburguesaTocineta);

        Comida hamburguesaHawaiana = new Comida(null, "Hamburguesa Hawaiana", new BigDecimal("23000"), "/imagenes/hamburguesa-hawaiana.webp");
        hamburguesaHawaiana.configurarIngrediente(null, pan, 1, true, false, false, new BigDecimal("0"));
        hamburguesaHawaiana.configurarIngrediente(null, carneRes, 200, true, true, false, new BigDecimal("4000"));
        hamburguesaHawaiana.configurarIngrediente(null, pina, 1, true, true, true, new BigDecimal("1000"));
        hamburguesaHawaiana.configurarIngrediente(null, queso, 1, true, true, true, new BigDecimal("1500"));
        hamburguesaHawaiana.configurarIngrediente(null, lechuga, 1, true, true, true, new BigDecimal("500"));
        hamburguesaHawaiana.configurarIngrediente(null, tomate, 100, true, true, true, new BigDecimal("500"));
        hamburguesaHawaiana.configurarIngrediente(null, jalapenos, 0, false, true, false, new BigDecimal("800"));
        comidaRepository.save(hamburguesaHawaiana);

        Comida perroClasico = new Comida(null, "Perro Caliente Clasico", new BigDecimal("12000"), "/imagenes/perro-clasico.webp");
        perroClasico.configurarIngrediente(null, pan, 1, true, false, false, new BigDecimal("0"));
        perroClasico.configurarIngrediente(null, salchicha, 1, true, true, false, new BigDecimal("2000"));
        perroClasico.configurarIngrediente(null, queso, 1, true, true, true, new BigDecimal("1000"));
        perroClasico.configurarIngrediente(null, cebolla, 50, true, true, true, new BigDecimal("500"));
        perroClasico.configurarIngrediente(null, salsaTomate, 30, true, true, true, new BigDecimal("500"));
        perroClasico.configurarIngrediente(null, tocineta, 0, false, true, false, new BigDecimal("2000"));
        comidaRepository.save(perroClasico);

        Comida perroTocino = new Comida(null, "Perro Caliente Tocino", new BigDecimal("16000"), "/imagenes/perro-tocino.webp");
        perroTocino.configurarIngrediente(null, pan, 1, true, false, false, new BigDecimal("0"));
        perroTocino.configurarIngrediente(null, salchicha, 1, true, true, false, new BigDecimal("2000"));
        perroTocino.configurarIngrediente(null, tocineta, 50, true, true, true, new BigDecimal("2000"));
        perroTocino.configurarIngrediente(null, queso, 1, true, true, true, new BigDecimal("1000"));
        perroTocino.configurarIngrediente(null, cebolla, 50, true, true, true, new BigDecimal("500"));
        perroTocino.configurarIngrediente(null, salsaTomate, 30, true, true, true, new BigDecimal("500"));
        comidaRepository.save(perroTocino);

        Comida pizzaPersonal = new Comida(null, "Pizza Personal", new BigDecimal("22000"), "/imagenes/pizza-personal.webp");
        pizzaPersonal.configurarIngrediente(null, masa, 1, true, false, false, new BigDecimal("0"));
        pizzaPersonal.configurarIngrediente(null, queso, 3, true, true, true, new BigDecimal("2000"));
        pizzaPersonal.configurarIngrediente(null, salsaTomate, 50, true, true, true, new BigDecimal("1000"));
        pizzaPersonal.configurarIngrediente(null, pepperoni, 100, true, true, true, new BigDecimal("3000"));
        pizzaPersonal.configurarIngrediente(null, cebolla, 0, false, true, false, new BigDecimal("500"));
        pizzaPersonal.configurarIngrediente(null, champinones, 0, false, true, false, new BigDecimal("1500"));
        pizzaPersonal.configurarIngrediente(null, jalapenos, 0, false, true, false, new BigDecimal("800"));
        pizzaPersonal.configurarIngrediente(null, pollo, 0, false, true, false, new BigDecimal("2500"));
        comidaRepository.save(pizzaPersonal);

        Comida pizzaHawaiana = new Comida(null, "Pizza Hawaiana", new BigDecimal("26000"), "/imagenes/pizza-hawaiana.webp");
        pizzaHawaiana.configurarIngrediente(null, masa, 1, true, false, false, new BigDecimal("0"));
        pizzaHawaiana.configurarIngrediente(null, queso, 3, true, true, true, new BigDecimal("2000"));
        pizzaHawaiana.configurarIngrediente(null, salsaTomate, 50, true, true, true, new BigDecimal("1000"));
        pizzaHawaiana.configurarIngrediente(null, pina, 1, true, true, true, new BigDecimal("1000"));
        pizzaHawaiana.configurarIngrediente(null, pollo, 100, true, true, true, new BigDecimal("2500"));
        pizzaHawaiana.configurarIngrediente(null, champinones, 0, false, true, false, new BigDecimal("1500"));
        pizzaHawaiana.configurarIngrediente(null, jalapenos, 0, false, true, false, new BigDecimal("800"));
        comidaRepository.save(pizzaHawaiana);

        Comida pizzaVegetariana = new Comida(null, "Pizza Vegetariana", new BigDecimal("20000"), "/imagenes/pizza-vegetariana.webp");
        pizzaVegetariana.configurarIngrediente(null, masa, 1, true, false, false, new BigDecimal("0"));
        pizzaVegetariana.configurarIngrediente(null, queso, 3, true, true, true, new BigDecimal("2000"));
        pizzaVegetariana.configurarIngrediente(null, salsaTomate, 50, true, true, true, new BigDecimal("1000"));
        pizzaVegetariana.configurarIngrediente(null, champinones, 80, true, true, true, new BigDecimal("1500"));
        pizzaVegetariana.configurarIngrediente(null, cebolla, 50, true, true, true, new BigDecimal("500"));
        pizzaVegetariana.configurarIngrediente(null, jalapenos, 40, true, true, true, new BigDecimal("800"));
        comidaRepository.save(pizzaVegetariana);

        Comida papasFrancesa = new Comida(null, "Papas a la Francesa", new BigDecimal("9000"), "/imagenes/papas-francesa.webp");
        papasFrancesa.configurarIngrediente(null, papas, 300, true, false, false, new BigDecimal("0"));
        papasFrancesa.configurarIngrediente(null, salsaTomate, 40, true, true, true, new BigDecimal("500"));
        papasFrancesa.configurarIngrediente(null, queso, 0, false, true, false, new BigDecimal("1500"));
        papasFrancesa.configurarIngrediente(null, tocineta, 0, false, true, false, new BigDecimal("2000"));
        papasFrancesa.configurarIngrediente(null, jalapenos, 0, false, true, false, new BigDecimal("800"));
        comidaRepository.save(papasFrancesa);

        Comida papasCargadas = new Comida(null, "Papas Cargadas", new BigDecimal("14000"), "/imagenes/papas-cargadas.webp");
        papasCargadas.configurarIngrediente(null, papas, 300, true, false, false, new BigDecimal("0"));
        papasCargadas.configurarIngrediente(null, queso, 2, true, true, true, new BigDecimal("1500"));
        papasCargadas.configurarIngrediente(null, tocineta, 80, true, true, true, new BigDecimal("2000"));
        papasCargadas.configurarIngrediente(null, salsaTomate, 30, true, true, true, new BigDecimal("500"));
        papasCargadas.configurarIngrediente(null, jalapenos, 0, false, true, false, new BigDecimal("800"));
        comidaRepository.save(papasCargadas);

        Comida alitasPicantes = new Comida(null, "Alitas Picantes", new BigDecimal("21000"), "/imagenes/alitas-picantes.webp");
        alitasPicantes.configurarIngrediente(null, alitas, 6, true, false, false, new BigDecimal("0"));
        alitasPicantes.configurarIngrediente(null, salsaPicante, 40, true, true, true, new BigDecimal("1000"));
        alitasPicantes.configurarIngrediente(null, cebolla, 30, true, true, true, new BigDecimal("500"));
        alitasPicantes.configurarIngrediente(null, jalapenos, 0, false, true, false, new BigDecimal("800"));
        comidaRepository.save(alitasPicantes);

        Comida comboGfcCrunch = new Comida(null, "Combo GFC Crunch", new BigDecimal("28000"), "/imagenes/combo-gfc-crunch.webp");
        comboGfcCrunch.configurarIngrediente(null, pan, 1, true, false, false, new BigDecimal("0"));
        comboGfcCrunch.configurarIngrediente(null, carneRes, 200, true, true, false, new BigDecimal("4000"));
        comboGfcCrunch.configurarIngrediente(null, queso, 1, true, true, true, new BigDecimal("1500"));
        comboGfcCrunch.configurarIngrediente(null, papas, 200, true, true, true, new BigDecimal("2000"));
        comboGfcCrunch.configurarIngrediente(null, salsaTomate, 30, true, true, true, new BigDecimal("500"));
        comboGfcCrunch.configurarIngrediente(null, tocineta, 0, false, true, false, new BigDecimal("2500"));
        comboGfcCrunch.configurarIngrediente(null, jalapenos, 0, false, true, false, new BigDecimal("800"));
        comidaRepository.save(comboGfcCrunch);

        log.info("Catalogo demo creado: 12 comidas, 19 ingredientes");
    }
}
