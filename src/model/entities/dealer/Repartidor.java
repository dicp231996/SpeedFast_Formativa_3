package model.entities.dealer;

import data.enumerate.EstadoPedido;
import data.enumerate.TipoServicio;
import model.core.Pedido;
import model.core.Persona;
import model.entities.business.ZonaCarga;
import model.interfaces.IRunnable;

// Clase Repartidor hereda de Persona e implementa tanto tu interfaz como la nativa de Java
public class Repartidor extends Persona implements IRunnable, Runnable {

    // Capacidad máxima de pedidos que un repartidor puede llevar en su mochila.
    private static final int CAPACIDAD_MAXIMA_PEDIDOS = 5;

    private TipoServicio tipoServicio;
    private boolean tieneMochilaTermica;
    private double capacidadPesoMax;
    private boolean estaCercaUbicacion;

    // Vector de tamaño fijo (en vez de ArrayList): solo caben 5 pedidos.
    private Pedido[] pedidosAsignados;
    private int cantidadPedidosAsignados;

    // Vínculo con el pool compartido desde donde se retiran los pedidos ya
    // CONFIRMADOS. Se inyecta antes de lanzar al repartidor en la Fase 4
    // (ver GestorFases.ejecutarFaseRutas).
    private ZonaCarga zonaCarga;

    public Repartidor() {
        super();
        this.tipoServicio = TipoServicio.COMIDA;
        this.tieneMochilaTermica = false;
        this.capacidadPesoMax = 0.0;
        this.estaCercaUbicacion = false;
        this.pedidosAsignados = new Pedido[CAPACIDAD_MAXIMA_PEDIDOS];
        this.cantidadPedidosAsignados = 0;
    }

    public Repartidor(String nombreCompleto, String telefonoContacto, TipoServicio tipoServicio,
                      boolean tieneMochilaTermica, double capacidadPesoMax, boolean estaCercaUbicacion) {
        super(nombreCompleto, telefonoContacto);
        this.tipoServicio = tipoServicio;
        this.tieneMochilaTermica = tieneMochilaTermica;
        this.capacidadPesoMax = capacidadPesoMax;
        this.estaCercaUbicacion = estaCercaUbicacion;
        this.pedidosAsignados = new Pedido[CAPACIDAD_MAXIMA_PEDIDOS];
        this.cantidadPedidosAsignados = 0;
    }

    // =========================================================
    // VINCULACIÓN CON LA ZONA DE CARGA
    // =========================================================
    public void setZonaCarga(ZonaCarga zonaCarga) {
        this.zonaCarga = zonaCarga;
    }

    // =========================================================
    // ORQUESTACIÓN COMPLETA DEL CICLO DE ENTREGA:
    // Retira, de la Zona de Carga compartida, ÚNICAMENTE los pedidos que le
    // fueron asignados a este repartidor durante la Fase 1 (el primero que
    // cumplió validarRequisitos), hasta que ya no le queden disponibles.
    // Cada repartidor corre en su propio hilo (ver GestorFases, que
    // administra el pool vía ExecutorService), por lo que varios de ellos
    // pueden estar retirando y entregando sus propios pedidos en simultáneo.
    // =========================================================
    @Override
    public void run() {
        String nombreHilo = Thread.currentThread().getName();
        System.out.println("\n>>> [ZONA DE CARGA - " + nombreHilo + "] " + this.getNombreCompleto()
                + " comienza a retirar sus pedidos confirmados.");

        if (this.zonaCarga == null) {
            System.out.println("    -> No se ha vinculado ninguna Zona de Carga a este repartidor.");
            return;
        }

        Pedido pedido;
        boolean atendioAlgunPedido = false;

        while ((pedido = this.zonaCarga.retirarPedido(this)) != null) {
            atendioAlgunPedido = true;
            procesarEntrega(pedido);
        }

        if (!atendioAlgunPedido) {
            System.out.println("    -> No tenía pedidos confirmados disponibles en la zona de carga.");
        }

        System.out.println("\n>>> [FIN DE RUTA] " + this.getNombreCompleto() + " ha finalizado su recorrido.\n");
    }

    // Procesa un único pedido propio retirado de la Zona de Carga: lo marca
    // EN_REPARTO, simula la entrega en un hilo real (HiloEntrega) esperando
    // su fin con join(), lo que a su vez deja el pedido en ENTREGADO al
    // finalizar.
    private void procesarEntrega(Pedido pedido) {
        if (pedido.isCancelado()) {
            System.out.println("    -> Pedido " + pedido.getIdPedido() + " fue cancelado antes de iniciar la entrega. Se omite.");
            return;
        }

        System.out.println("-> [RETIRO] " + this.getNombreCompleto() + " retira su pedido "
                + pedido.getIdPedido() + " desde la zona de carga.");

        pedido.nuevoEstado(EstadoPedido.EN_REPARTO);

        Thread hiloEntrega = new Thread(new model.valueobjects.HiloEntrega(pedido),
                "Entrega-" + pedido.getIdPedido());
        hiloEntrega.start();
        try {
            hiloEntrega.join();
        } catch (InterruptedException e) {
            System.err.println("-> Alerta: El recorrido de " + this.getNombreCompleto() + " fue interrumpido.");
            Thread.currentThread().interrupt();
        }
    }

    // =========================================================
    // GESTIÓN DEL VECTOR FIJO DE PEDIDOS (máx. 5 posiciones)
    // =========================================================
    public Pedido[] getPedidosAsignados() {
        return pedidosAsignados;
    }

    public int getCantidadPedidosAsignados() {
        return cantidadPedidosAsignados;
    }

    // Indica si aún queda espacio libre en la mochila (menos de 5 pedidos).
    public boolean tieneCupoDisponible() {
        return cantidadPedidosAsignados < CAPACIDAD_MAXIMA_PEDIDOS;
    }

    public void agregarPedido(Pedido pedido) {
        if (!tieneCupoDisponible()) {
            System.out.println("-> Alerta: " + this.getNombreCompleto()
                    + " ya alcanzó su capacidad máxima de " + CAPACIDAD_MAXIMA_PEDIDOS + " pedidos.");
            return;
        }
        this.pedidosAsignados[cantidadPedidosAsignados] = pedido;
        cantidadPedidosAsignados++;
    }

    public void removerPedido(Pedido pedido) {
        int indice = -1;
        for (int i = 0; i < cantidadPedidosAsignados; i++) {
            if (this.pedidosAsignados[i] == pedido) {
                indice = i;
                break;
            }
        }

        if (indice == -1) {
            return; // El pedido no estaba en la mochila de este repartidor
        }

        // Compactamos el vector: desplazamos una posición a la izquierda
        // todo lo que venía después del pedido removido.
        for (int i = indice; i < cantidadPedidosAsignados - 1; i++) {
            this.pedidosAsignados[i] = this.pedidosAsignados[i + 1];
        }
        this.pedidosAsignados[cantidadPedidosAsignados - 1] = null;
        cantidadPedidosAsignados--;
    }

    public void limpiarPedidos() {
        for (int i = 0; i < cantidadPedidosAsignados; i++) {
            this.pedidosAsignados[i] = null;
        }
        cantidadPedidosAsignados = 0;
    }

    // Puente para evitar errores de visibilidad en el Main
    public String getTelefono() {
        return super.getTelefonoContacto();
    }

    // Getters
    public TipoServicio getTipoServicio() { return tipoServicio; }
    public boolean isTieneMochilaTermica() { return tieneMochilaTermica; }
    public double getCapacidadPesoMax() { return capacidadPesoMax; }
    public boolean isEstaCercaUbicacion() { return estaCercaUbicacion; }

    // Setters
    public void setTipoServicio(TipoServicio tipoServicio) { this.tipoServicio = tipoServicio; }
    public void setTieneMochilaTermica(boolean tieneMochilaTermica) { this.tieneMochilaTermica = tieneMochilaTermica; }
    public void setCapacidadPesoMax(double capacidadPesoMax) { this.capacidadPesoMax = capacidadPesoMax; }
    public void setEstaCercaUbicacion(boolean estaCercaUbicacion) { this.estaCercaUbicacion = estaCercaUbicacion; }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(super.toString())
                .append("\n   -> Perfil Operativo:")
                .append("\n      | Tipo de Servicio: ").append(this.tipoServicio)
                .append("\n      | Mochila Térmica: ").append(this.tieneMochilaTermica ? "Sí" : "No")
                .append("\n      | Capacidad Máx: ").append(this.capacidadPesoMax).append(" kg")
                .append("\n      | Cerca de ubicación: ").append(this.estaCercaUbicacion ? "Sí" : "No")
                .append("\n      | Carga actual: ").append(this.cantidadPedidosAsignados)
                .append("/").append(CAPACIDAD_MAXIMA_PEDIDOS).append(" pedidos asignados");
        return sb.toString();
    }
}