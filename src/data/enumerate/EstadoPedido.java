package data.enumerate;

// Enumerador que representa el ciclo de vida operativo de un Pedido
public enum EstadoPedido {
    PENDIENTE("Esperando asignación de repartidor"),
    CONFIRMADO("Repartidor asignado; el pedido aguarda en la zona de carga"),
    EN_REPARTO("El repartidor va en camino con tu pedido"),
    ENTREGADO("El pedido fue entregado con éxito"),
    CANCELADO("El pedido fue cancelado y no continuará su recorrido");

    // Atributo privado para guardar el mensaje amigable asociado a la clave
    private final String mensajeAmigable;

    EstadoPedido(String mensajeAmigable) {
        this.mensajeAmigable = mensajeAmigable;
    }

    public String getMensajeAmigable() {
        return mensajeAmigable;
    }

    // toString() muestra la clave del enumerador junto al mensaje amigable,
    // por ejemplo: "EN_REPARTO -> El repartidor va en camino con tu pedido"
    @Override
    public String toString() {
        return this.name() + " -> " + mensajeAmigable;
    }
}