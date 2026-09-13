package app;

import data.util.ControladorEnvios;
import data.util.GestorFases;
import data.util.GestorInstancias;
import model.core.Pedido;
import model.entities.business.ZonaCarga;
import model.entities.dealer.Repartidor;

import java.util.ArrayList;
import java.util.Scanner;

public class SpeedFast {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        String rutaPedidos = "resources/pedidos.txt";
        String rutaRepartidores = "resources/repartidores.txt";

        System.out.println("=========================================");
        System.out.println("       INICIANDO SISTEMA SPEEDFAST       ");
        System.out.println("=========================================\n");

        ArrayList<Pedido> listaPedidos = GestorInstancias.cargarPedidos(rutaPedidos);
        ArrayList<Repartidor> listaRepartidores = GestorInstancias.cargarRepartidores(rutaRepartidores);
        ControladorEnvios controlador = new ControladorEnvios();

        // Zona de Carga única, compartida entre la Fase 1 (donde los pedidos
        // se registran y se encolan al quedar CONFIRMADOS) y la Fase 4 (donde
        // los repartidores compiten por retirarlos).
        ZonaCarga zonaCarga = new ZonaCarga();

        GestorFases.ejecutarFaseAsignacion(listaPedidos, listaRepartidores, controlador, scanner, zonaCarga);
        GestorFases.ejecutarFaseDespacho(listaPedidos);
        GestorFases.ejecutarFaseCancelaciones(listaPedidos, scanner);
        GestorFases.ejecutarFaseRutas(listaPedidos, zonaCarga);
        GestorFases.ejecutarFaseReportes(listaPedidos, controlador);

        scanner.close();
    }
}