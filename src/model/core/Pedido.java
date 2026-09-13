package model.core;

import data.enumerate.EstadoPedido;
import model.entities.dealer.Repartidor;
import model.interfaces.ICancelable;
import model.interfaces.IDespachable;
import model.interfaces.IRastreable;

public abstract class Pedido implements IDespachable, ICancelable, IRastreable {

    // Contador estático compartido por todas las instancias: genera el
    // correlativo interno de manera automática cada vez que se crea un Pedido.
    private static int contadorId = 1;

    private final int id;
    private String idPedido;
    private String direccionEntrega;
    private String tipoPedido;
    private double distanciaKm;

    protected Repartidor repartidorAsignado;

    // Nuevas variables internas para encapsular la interfaz ICancelable
    protected boolean estadoCancelado;
    protected String motivoCancelacion;

    // Estado operativo del pedido dentro de su ciclo de vida (PENDIENTE, CONFIRMADO, EN_REPARTO, ENTREGADO)
    protected EstadoPedido estado;

    public Pedido() {
        this.id = contadorId++;
        this.idPedido = "GEN-0000";
        this.direccionEntrega = "Dirección no especificada";
        this.tipoPedido = "Estándar";
        this.distanciaKm = 0.0;
        this.repartidorAsignado = null;
        this.estadoCancelado = false;
        this.motivoCancelacion = "N/A";
        this.estado = EstadoPedido.PENDIENTE;
    }

    public Pedido(String idPedido, String direccionEntrega, String tipoPedido, double distanciaKm) {
        this.id = contadorId++;
        this.idPedido = idPedido;
        this.direccionEntrega = direccionEntrega;
        this.tipoPedido = tipoPedido;
        this.distanciaKm = distanciaKm;
        this.repartidorAsignado = null;
        this.estadoCancelado = false;
        this.motivoCancelacion = "N/A";
        this.estado = EstadoPedido.PENDIENTE;
    }

    public int getId() { return id; }
    public String getIdPedido() { return idPedido; }
    public String getDireccionEntrega() { return direccionEntrega; }
    public String getTipoPedido() { return tipoPedido; }
    public double getDistanciaKm() { return distanciaKm; }
    public Repartidor getRepartidorAsignado() { return repartidorAsignado; }
    public EstadoPedido getEstado() { return estado; }

    public void setIdPedido(String idPedido) { this.idPedido = idPedido; }
    public void setDireccionEntrega(String direccionEntrega) { this.direccionEntrega = direccionEntrega; }
    public void setTipoPedido(String tipoPedido) { this.tipoPedido = tipoPedido; }
    public void setDistanciaKm(double distanciaKm) { this.distanciaKm = distanciaKm; }
    public void setRepartidorAsignado(Repartidor repartidorAsignado) { this.repartidorAsignado = repartidorAsignado; }

    public abstract double calcularTiempoEntrega();
    public abstract boolean validarRequisitos(Repartidor candidato);

    // =========================================================
    // LÓGICA DE ASIGNACIÓN
    // =========================================================
    public void asignarRepartidor(Repartidor candidato) {
        System.out.println("Evaluando al repartidor " + candidato.getNombreCompleto() +
                " para el pedido " + this.idPedido + "...");

        if (validarRequisitos(candidato)) {
            this.repartidorAsignado = candidato;
            this.nuevoEstado(EstadoPedido.CONFIRMADO);
            System.out.println("-> ÉXITO: Repartidor asignado correctamente.\n");
        } else {
            System.out.println("-> RECHAZADO: El repartidor no cumple con los requisitos del pedido.\n");
        }
    }

    public void asignarRepartidor(String nombre) {
        System.out.println("Forzando asignación nominal para el pedido " + this.idPedido + "...");
        Repartidor comodin = new Repartidor(nombre, "N/A", data.enumerate.TipoServicio.COMIDA, true, 999.0, true);
        this.repartidorAsignado = comodin;
        this.nuevoEstado(EstadoPedido.CONFIRMADO);
        System.out.println("-> ÉXITO: Asignado directamente al repartidor: " + nombre + "\n");
    }

    // =========================================================
    // ACTUALIZACIÓN CONTROLADA DEL ESTADO DEL PEDIDO
    // Punto único de entrada para ir avanzando el pedido a través de su
    // ciclo de vida (PENDIENTE -> CONFIRMADO -> EN_REPARTO -> ENTREGADO) durante la
    // ejecución del programa, dejando trazabilidad del cambio en consola.
    // =========================================================
    public void nuevoEstado(EstadoPedido estado) {
        if (this.estado == estado) {
            return; // Sin cambios reales, evitamos ruido en el log
        }
        System.out.println("-> [ESTADO] Pedido " + this.idPedido + ": " + this.estado.name() +
                " => " + estado.name());
        this.estado = estado;
    }

    // =========================================================
    // MARCA EL PEDIDO COMO ENTREGADO (invocado al finalizar HiloEntrega)
    // =========================================================
    public void marcarEntregado() {
        this.nuevoEstado(EstadoPedido.ENTREGADO);
    }

    // =========================================================
    // IMPLEMENTACIÓN DE INTERFACES FUNCIONALES DISTRIBUIDAS
    // =========================================================
    @Override
    public void despachar() {
        if (this.repartidorAsignado != null && !this.estadoCancelado) {
            System.out.println("-> [ESTADO] Pedido " + this.idPedido + " despachado con éxito.");
        }
    }

    @Override
    public String rastrear() {
        if (this.estadoCancelado) {
            return "CANCELADO (" + this.motivoCancelacion + ")";
        } else if (this.repartidorAsignado != null) {
            return "EN RUTA (A cargo de: " + this.repartidorAsignado.getNombreCompleto() + ")";
        } else {
            return "PENDIENTE (Esperando repartidor)";
        }
    }

    @Override
    public Repartidor cancelar(String motivo) {
        // Regla base: Ningún pedido general se puede cancelar si ya se asignó
        if (this.repartidorAsignado == null) {
            this.estadoCancelado = true;
            this.motivoCancelacion = motivo;
            this.nuevoEstado(EstadoPedido.CANCELADO);
            System.out.println("-> Pedido " + this.idPedido + " cancelado exitosamente antes de despacho.");
            return null;
        } else {
            System.out.println("-> Error: El pedido " + this.idPedido + " ya fue despachado y no admite cancelación tardía.");
            return null;
        }
    }

    @Override
    public boolean isCancelado() {
        return this.estadoCancelado;
    }

    @Override
    public String getMotivoCancelacion() {
        return this.motivoCancelacion;
    }

    public void mostrarResumen() {
        System.out.println("--- RESUMEN DEL PEDIDO ---");
        System.out.println("ID interno: " + this.id + " | ID: " + this.idPedido + " | Tipo: " + this.tipoPedido);
        System.out.println("Dirección: " + this.direccionEntrega);
        System.out.println("Estado: " + this.estado);
        if (this.repartidorAsignado != null) {
            System.out.println("Tiempo estimado de entrega: " + calcularTiempoEntrega() + " minutos");
        }
    }
}