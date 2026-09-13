package model.entities.order;

import data.enumerate.EstadoPedido;
import model.core.Pedido;
import model.entities.dealer.Repartidor;

public class PedidoComida extends Pedido {

    public PedidoComida(String idPedido, String direccionEntrega, double distanciaKm) {
        super(idPedido, direccionEntrega, "Comida", distanciaKm);
    }

    @Override
    public double calcularTiempoEntrega() {
        return 10.0 + (this.getDistanciaKm() * 2.0); // Fórmula de ejemplo
    }

    // =========================================================
    // REGLAS DE NEGOCIO + LÍMITE DE INVENTARIO
    // =========================================================
    @Override
    public boolean validarRequisitos(Repartidor candidato) {
        return candidato.getTipoServicio() == data.enumerate.TipoServicio.COMIDA &&
                candidato.isEstaCercaUbicacion() &&
                candidato.tieneCupoDisponible();
    }

    // =========================================================
    // SOBRECARGA NOMINAL (Rúbrica)
    // =========================================================
    @Override
    public void asignarRepartidor(String nombre) {
        System.out.println("[Protocolo Comida] Registrando asignación de emergencia por nombre: " + nombre);
        super.asignarRepartidor(nombre);
    }

    // =========================================================
    // SOBREESCRITURA DE INTERFAZ: EXCEPCIÓN DE CANCELACIÓN
    // Esta excepción NO devuelve el pedido a un pool de espera para una
    // nueva asignación: es un desenlace terminal (repartidorAsignado queda
    // en null y estadoCancelado en true de forma definitiva), por lo que
    // corresponde reflejarlo con nuevoEstado(CANCELADO).
    // =========================================================
    @Override
    public Repartidor cancelar(String motivo) {
        this.estadoCancelado = true;
        this.motivoCancelacion = motivo;
        this.nuevoEstado(EstadoPedido.CANCELADO);

        Repartidor liberado = this.repartidorAsignado;
        this.repartidorAsignado = null;

        if (liberado != null) {
            System.out.println("-> [EXCEPCIÓN COMIDA] Pedido en ruta cancelado. Repartidor recuperado.");
        } else {
            System.out.println("-> Pedido " + this.getIdPedido() + " cancelado exitosamente antes de despacho.");
        }

        return liberado;
    }
}