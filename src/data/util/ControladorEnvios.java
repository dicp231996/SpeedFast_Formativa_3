package data.util;

import data.enumerate.EstadoPedido;
import model.core.Pedido;
import model.entities.dealer.Repartidor;
import java.util.ArrayList;

public class ControladorEnvios {

    private ArrayList<Pedido> entregasExitosas;

    public ControladorEnvios() {
        this.entregasExitosas = new ArrayList<>();
    }

    public ArrayList<Repartidor> filtrarRepartidoresElegibles(Pedido pedido, ArrayList<Repartidor> disponibles) {
        ArrayList<Repartidor> elegibles = new ArrayList<>();
        for (Repartidor candidato : disponibles) {
            if (pedido.validarRequisitos(candidato)) {
                elegibles.add(candidato);
            }
        }
        return elegibles;
    }

    // Un pedido se considera entregado exitosamente solo si su estado real
    // es ENTREGADO (es decir, ya completó la simulación de HiloEntrega),
    // no basta con que tenga repartidor y no esté cancelado.
    public void registrarEntregaExitosa(Pedido pedido) {
        if (pedido.getEstado() == EstadoPedido.ENTREGADO && !entregasExitosas.contains(pedido)) {
            this.entregasExitosas.add(pedido);
        }
    }

    public void borrarDeEntregasExitosas(Pedido pedido) {
        this.entregasExitosas.remove(pedido);
    }

    public void mostrarHistorialEntregas() {
        System.out.println("\n--- HISTORIAL DE ENTREGAS EXITOSAS ---");
        if (entregasExitosas.isEmpty()) {
            System.out.println("No hay entregas registradas.");
        } else {
            for (Pedido p : entregasExitosas) {
                System.out.println("ID: " + p.getIdPedido() + " | Entregado por: " + p.getRepartidorAsignado().getNombreCompleto());
            }
        }
        System.out.println("\n-> Total de pedidos entregados correctamente: " + entregasExitosas.size());
    }
}