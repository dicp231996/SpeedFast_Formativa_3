package model.entities.order;

import model.core.Pedido;
import model.entities.dealer.Repartidor;

public class PedidoEncomienda extends Pedido {

    private double pesoKg;

    public PedidoEncomienda(String idPedido, String direccionEntrega, double distanciaKm, double pesoKg) {
        super(idPedido, direccionEntrega, "Encomienda", distanciaKm);
        this.pesoKg = pesoKg;
    }

    public double getPesoKg() {
        return pesoKg;
    }

    @Override
    public double calcularTiempoEntrega() {
        return 20.0 + (this.getDistanciaKm() * 5.0) + this.pesoKg; // Fórmula de ejemplo
    }

    // =========================================================
    // REGLAS DE NEGOCIO + LÍMITE DE INVENTARIO
    // =========================================================
    @Override
    public boolean validarRequisitos(Repartidor candidato) {
        return candidato.getTipoServicio() == data.enumerate.TipoServicio.ENCOMIENDA &&
                candidato.getCapacidadPesoMax() >= this.pesoKg &&
                candidato.tieneCupoDisponible();
    }

    // =========================================================
    // SOBRECARGA NOMINAL (Rúbrica)
    // =========================================================
    @Override
    public void asignarRepartidor(String nombre) {
        System.out.println("[Protocolo Encomienda] Registrando asignación de emergencia por nombre: " + nombre);
        super.asignarRepartidor(nombre);
    }
}