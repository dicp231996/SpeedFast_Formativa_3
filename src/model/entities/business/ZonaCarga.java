package model.entities.business;

import data.enumerate.EstadoPedido;
import model.core.Pedido;
import model.entities.dealer.Repartidor;

import java.util.ArrayList;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Representa la zona de carga física del sistema: el punto en común donde
 * ingresan todas las instancias de Pedido creadas, y desde donde cada
 * repartidor retira ÚNICAMENTE los pedidos que ya le fueron asignados a él
 * (según las reglas de negocio de la Fase 1) y que se encuentran CONFIRMADOS.
 *
 * - El ArrayList "pedidosRegistrados" actúa como registro histórico de TODOS
 *   los pedidos que han pasado por la zona de carga, sin importar su estado.
 *   Su acceso está protegido con "synchronized".
 *
 * - El BlockingQueue "pedidosConfirmados" es el canal real de entrega: solo
 *   contiene pedidos CONFIRMADOS, listos para ser retirados por el repartidor
 *   al que le corresponden. Al estar toda la clase protegida con
 *   "synchronized", la verificación de a quién pertenece un pedido y su
 *   extracción ocurren de forma atómica: un mismo pedido jamás puede ser
 *   devuelto en dos llamadas distintas, ni retirado por un repartidor que no
 *   sea el que tiene asignado.
 */
public class ZonaCarga {

    private final ArrayList<Pedido> pedidosRegistrados;
    private final BlockingQueue<Pedido> pedidosConfirmados;

    public ZonaCarga() {
        this.pedidosRegistrados = new ArrayList<>();
        this.pedidosConfirmados = new LinkedBlockingQueue<>();
    }

    // =========================================================
    // INGRESO DE PEDIDOS A LA ZONA DE CARGA
    // =========================================================
    // Se espera invocar este método:
    //   1) Al crear cada Pedido (o al iniciar su evaluación en la Fase 1),
    //      para dejarlo registrado en el ArrayList mientras aún está PENDIENTE.
    //   2) Nuevamente cuando el pedido pase a CONFIRMADO (repartidor ya
    //      asignado), momento en el que recién se habilita para ser retirado
    //      por el repartidor que tiene asignado (se encola en el BlockingQueue).
    // El método completo es "synchronized" para proteger tanto el ArrayList
    // como la verificación de duplicados antes de encolar.
    public synchronized void agregarPedido(Pedido pedido) {
        if (pedido == null) {
            return;
        }

        // Registro histórico: cada instancia ingresa una sola vez.
        if (!pedidosRegistrados.contains(pedido)) {
            pedidosRegistrados.add(pedido);
        }

        // Solo se habilita para retiro si ya está CONFIRMADO, y solo una vez
        // (se evita que quede duplicado en la cola si el método se invoca
        // más de una vez para el mismo pedido).
        if (pedido.getEstado() == EstadoPedido.CONFIRMADO && !pedidosConfirmados.contains(pedido)) {
            pedidosConfirmados.offer(pedido);
        }
    }

    // =========================================================
    // RETIRO DE PEDIDOS DESDE LA ZONA DE CARGA
    // =========================================================
    // Un repartidor invoca este método pasándose a sí mismo para tomar,
    // de entre los pedidos CONFIRMADOS disponibles, aquel que le fue
    // asignado a ÉL específicamente (el primer repartidor que cumplió con
    // validarRequisitos en la Fase 1). Al ser toda la operación atómica
    // (método synchronized), ningún otro repartidor puede llevarse ese mismo
    // pedido, ni este repartidor puede retirar un pedido ajeno. Devuelve
    // null si en este instante no tiene ningún pedido propio disponible.
    public synchronized Pedido retirarPedido(Repartidor repartidor) {
        if (repartidor == null) {
            return null;
        }

        for (Pedido pedido : pedidosConfirmados) {
            if (pedido.getRepartidorAsignado() == repartidor) {
                pedidosConfirmados.remove(pedido);
                return pedido;
            }
        }

        return null;
    }
}