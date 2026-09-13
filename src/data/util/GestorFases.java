package data.util;

import model.core.Pedido;
import model.entities.business.ZonaCarga;
import model.entities.dealer.Repartidor;
import model.entities.order.PedidoComida;

import java.util.ArrayList;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Clase encargada de orquestar cada una de las fases del proceso de
 * SpeedFast (asignación, despacho, cancelaciones, ejecución de rutas y
 * reportes), delegando así la lógica de negocio que antes se encontraba
 * directamente en la clase main (SpeedFast).
 */
public class GestorFases {

    // =========================================================
    // FASE 1: RESERVA Y ASIGNACIÓN MÚLTIPLE
    // =========================================================
    public static void ejecutarFaseAsignacion(ArrayList<Pedido> listaPedidos,
                                              ArrayList<Repartidor> listaRepartidores,
                                              ControladorEnvios controlador,
                                              Scanner scanner,
                                              ZonaCarga zonaCarga) {

        System.out.println("--- FASE 1: RESERVA DE PEDIDOS ---");
        System.out.println("1. Automática (El sistema evalúa y asigna bajo sus reglas)");
        System.out.println("2. Manual (El usuario elige entre los candidatos aptos)");
        System.out.println("3. Nominal (Asignación directa por nombre según rúbrica)");
        System.out.print("Seleccione el método de asignación: ");

        String tipoAsignacion = scanner.nextLine();
        boolean esManual = tipoAsignacion.equals("2");
        boolean esNominal = tipoAsignacion.equals("3");

        for (Pedido pedido : listaPedidos) {
            System.out.println("\n>> Reservando Pedido ID: " + pedido.getIdPedido());

            // Registro en la Zona de Carga: en este punto el pedido aún está
            // PENDIENTE, así que solo queda anotado en el historial.
            zonaCarga.agregarPedido(pedido);

            if (esNominal) {
                asignarNominal(pedido, scanner);
                // Si quedó CONFIRMADO, recién ahora se habilita para retiro.
                zonaCarga.agregarPedido(pedido);
                continue;
            }

            if (esManual) {
                asignarManual(pedido, listaRepartidores, controlador, scanner);
            } else {
                asignarAutomatico(pedido, listaRepartidores);
            }

            // Si quedó CONFIRMADO, recién ahora se habilita para retiro.
            zonaCarga.agregarPedido(pedido);
        }
    }

    private static void asignarNominal(Pedido pedido, Scanner scanner) {
        System.out.print("Ingrese el nombre exacto del repartidor para este pedido: ");
        pedido.asignarRepartidor(scanner.nextLine());
        // Enlace bidireccional al comodín
        if (pedido.getRepartidorAsignado() != null) {
            pedido.getRepartidorAsignado().agregarPedido(pedido);
        }
    }

    private static void asignarManual(Pedido pedido, ArrayList<Repartidor> listaRepartidores,
                                      ControladorEnvios controlador, Scanner scanner) {

        ArrayList<Repartidor> candidatosAptos = controlador.filtrarRepartidoresElegibles(pedido, listaRepartidores);
        if (candidatosAptos.isEmpty()) {
            System.out.println("-> Alerta: No hay repartidores elegibles. Pedido pendiente.");
            return;
        }

        System.out.println("Candidatos disponibles:");
        for (int i = 0; i < candidatosAptos.size(); i++) {
            Repartidor r = candidatosAptos.get(i);
            System.out.println("  [" + i + "] " + r.getNombreCompleto() + " | Carga actual: " + r.getCantidadPedidosAsignados() + "/5");
        }
        System.out.print("Ingrese el número del repartidor a reservar: ");

        try {
            int index = Integer.parseInt(scanner.nextLine());
            if (index >= 0 && index < candidatosAptos.size()) {
                Repartidor seleccionado = candidatosAptos.get(index);
                pedido.asignarRepartidor(seleccionado);
                // Enlace bidireccional
                seleccionado.agregarPedido(pedido);
                // NOTA: Ya no lo removemos de la lista global, permitiendo múltiples asignaciones
            }
        } catch (Exception e) {
            System.out.println("-> Entrada inválida. Omitiendo reserva.");
        }
    }

    private static void asignarAutomatico(Pedido pedido, ArrayList<Repartidor> listaRepartidores) {
        for (Repartidor candidato : listaRepartidores) {
            pedido.asignarRepartidor(candidato);

            if (pedido.getRepartidorAsignado() != null) {
                // Enlace bidireccional
                candidato.agregarPedido(pedido);
                // Ya no lo removemos, lo que significa que en automático el primero elegible
                // podría llevarse muchos pedidos (comportamiento de pool abierto).
                break;
            }
        }
    }

    // =========================================================
    // FASE 2: VERIFICACIÓN Y DESPACHO
    // =========================================================
    public static void ejecutarFaseDespacho(ArrayList<Pedido> listaPedidos) {
        System.out.println("\n=========================================");
        System.out.println("--- FASE 2: VERIFICACIÓN Y DESPACHO ---");
        System.out.println("=========================================");

        for (Pedido pedido : listaPedidos) {
            if (pedido.getRepartidorAsignado() != null && !pedido.isCancelado()) {
                pedido.mostrarResumen();
                pedido.despachar();
                System.out.println("--------------------------------------------------");
            }
        }
    }

    // =========================================================
    // FASE 3: GESTIÓN DE CANCELACIONES (Limpieza de mochila)
    // =========================================================
    public static void ejecutarFaseCancelaciones(ArrayList<Pedido> listaPedidos, Scanner scanner) {
        System.out.println("\n=========================================");
        System.out.println("--- FASE 3: GESTIÓN DE CANCELACIONES ---");
        System.out.println("=========================================");

        boolean enFaseCancelacion = true;

        while (enFaseCancelacion) {
            int activos = contarPedidosCancelables(listaPedidos);

            if (activos == 0) {
                System.out.println("-> No quedan pedidos despachados susceptibles a cancelación.");
                break;
            }

            System.out.print("\nIngrese ID a cancelar (o escriba 'FIN' para iniciar las rutas): ");
            String input = scanner.nextLine();

            if (input.equalsIgnoreCase("FIN")) break;

            procesarCancelacion(listaPedidos, input, scanner);
        }
    }

    private static int contarPedidosCancelables(ArrayList<Pedido> listaPedidos) {
        int activos = 0;
        System.out.println("\nPedidos despachados (Solo Comida admite cancelación tardía):");

        for (Pedido p : listaPedidos) {
            if (p.getRepartidorAsignado() != null && !p.isCancelado() && p instanceof PedidoComida) {
                System.out.println("- " + p.getIdPedido() + " | Repartidor: " + p.getRepartidorAsignado().getNombreCompleto());
                activos++;
            }
        }
        return activos;
    }

    private static void procesarCancelacion(ArrayList<Pedido> listaPedidos, String idBuscado, Scanner scanner) {
        Pedido pedidoACancelar = null;
        for (Pedido p : listaPedidos) {
            if (p.getIdPedido().equalsIgnoreCase(idBuscado)) {
                pedidoACancelar = p;
                break;
            }
        }

        if (pedidoACancelar == null) {
            System.out.println("-> Error: ID no encontrado.");
            return;
        }

        System.out.print("Ingrese el motivo: ");
        String motivo = scanner.nextLine();

        Repartidor liberado = pedidoACancelar.cancelar(motivo);

        if (liberado != null) {
            // ¡CRÍTICO!: Debemos sacarle el pedido de la mochila al repartidor
            liberado.removerPedido(pedidoACancelar);
            System.out.println("-> Se retiró el pedido de la mochila de " + liberado.getNombreCompleto());
        }
    }

    // =========================================================
    // FASE 4: EJECUCIÓN DE LAS RUTAS (CONCURRENCIA CON ExecutorService)
    // =========================================================
    public static void ejecutarFaseRutas(ArrayList<Pedido> listaPedidos, ZonaCarga zonaCarga) {
        System.out.println("\n=========================================");
        System.out.println("--- FASE 4: EJECUCIÓN DE ENTREGAS ---");
        System.out.println("=========================================");

        // 1. Identificamos qué repartidores tienen trabajo real asignado
        ArrayList<Repartidor> repartidoresEnRuta = new ArrayList<>();
        for (Pedido p : listaPedidos) {
            Repartidor r = p.getRepartidorAsignado();
            if (r != null && !repartidoresEnRuta.contains(r)) {
                repartidoresEnRuta.add(r);
            }
        }

        if (repartidoresEnRuta.isEmpty()) {
            System.out.println("-> No hay repartidores con pedidos asignados.");
            return;
        }

        // 2. Vinculamos a todos los repartidores en ruta con la MISMA Zona de
        //    Carga: es el pool compartido desde donde cada uno retirará el
        //    siguiente pedido CONFIRMADO disponible.
        for (Repartidor r : repartidoresEnRuta) {
            r.setZonaCarga(zonaCarga);
        }

        // 3. Creamos un pool con un hilo por repartidor en ruta: cada Repartidor
        //    (que implementa Runnable) se ejecuta de forma CONCURRENTE respecto
        //    a los demás, compitiendo por retirar pedidos de la Zona de Carga.
        ExecutorService pool = Executors.newFixedThreadPool(repartidoresEnRuta.size());

        for (Repartidor r : repartidoresEnRuta) {
            pool.execute(r);
        }

        // 4. Cerramos el pool y esperamos a que todos los repartidores terminen
        //    su recorrido antes de avanzar a la Fase 5 (reportes).
        pool.shutdown();
        try {
            if (!pool.awaitTermination(5, TimeUnit.MINUTES)) {
                System.out.println("-> Alerta: Tiempo de espera agotado, forzando cierre del pool.");
                pool.shutdownNow();
            }
        } catch (InterruptedException e) {
            System.err.println("-> Alerta: La ejecución de las rutas fue interrumpida.");
            pool.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    // =========================================================
    // FASE 5: CONFIRMACIÓN Y REPORTES
    // =========================================================
    public static void ejecutarFaseReportes(ArrayList<Pedido> listaPedidos, ControladorEnvios controlador) {
        System.out.println("\n=========================================");
        System.out.println("--- FASE 5: ESTADO GLOBAL Y RASTREO ---");
        System.out.println("=========================================");

        for (Pedido p : listaPedidos) {
            if (p.getRepartidorAsignado() != null && !p.isCancelado()) {
                controlador.registrarEntregaExitosa(p);
            }
            System.out.println("Seguimiento ID " + p.getIdPedido() + " -> " + p.rastrear());
        }

        controlador.mostrarHistorialEntregas();
    }
}