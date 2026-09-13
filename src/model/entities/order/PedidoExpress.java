package model.entities.order;

import model.core.Pedido;
import model.entities.dealer.Repartidor;

public class PedidoExpress extends Pedido {

    public PedidoExpress(String idPedido, String direccionEntrega, double distanciaKm) {
        super(idPedido, direccionEntrega, "Express", distanciaKm);
    }

    @Override
    public double calcularTiempoEntrega() {
        return 5.0 + (this.getDistanciaKm() * 1.5); // Fórmula de ejemplo
    }

    // =========================================================
    // REGLAS DE NEGOCIO + LÍMITE DE INVENTARIO
    // =========================================================
    @Override
    public boolean validarRequisitos(Repartidor candidato) {
        return candidato.getTipoServicio() == data.enumerate.TipoServicio.COMPRA_EXPRESS &&
                candidato.tieneCupoDisponible();
    }

    // =========================================================
    // SOBRECARGA NOMINAL (Rúbrica)
    // =========================================================
    @Override
    public void asignarRepartidor(String nombre) {
        System.out.println("[Protocolo Express] Registrando asignación de emergencia por nombre: " + nombre);
        super.asignarRepartidor(nombre);
    }
}