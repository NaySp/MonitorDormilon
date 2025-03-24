import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadLocalRandom;

class MonitorDormilonSystem {
    private final Semaphore monitorSemaphore = new Semaphore(0, true); // Para despertar al monitor
    private final Semaphore chairsSemaphore; // Controla las sillas disponibles en el corredor
    private final Semaphore monitorMutex = new Semaphore(1, true); // Controla la atención de un estudiante a la vez
    private final Queue<Estudiante> waitingQueue = new LinkedList<>(); // Cola de espera
    private final Object queueLock = new Object(); // Para acceder a la cola de manera segura
    private final int numChairs;

    public MonitorDormilonSystem (int numChairs) {
        this.numChairs = numChairs;
        this.chairsSemaphore = new Semaphore(numChairs, true);
    }

    class Estudiante extends Thread {
        private final int id;

        public Estudiante(int id) {
            this.id = id;
        }

        @Override
        public void run() {
            try {
                while (true) {
                    Thread.sleep(ThreadLocalRandom.current().nextInt(2000, 5000)); // Simula tiempo programando

                    if (chairsSemaphore.tryAcquire()) { // Intenta tomar una silla en el corredor
                        synchronized (queueLock) {
                            waitingQueue.add(this);
                        }
                        System.out.println("Estudiante " + id + " se sienta en una silla del corredor.");
                        monitorSemaphore.release(); // Despierta al monitor si está dormido

                        synchronized (this) {
                            this.wait(); // Espera a ser llamado por el monitor
                        }

                        System.out.println("Estudiante " + id + " esta siendo atendido por el monitor.");
                        Thread.sleep(ThreadLocalRandom.current().nextInt(1000, 3000)); // Simula la consulta
                        chairsSemaphore.release(); // Libera la silla, primero se libera y después se imprime

                        synchronized (this) {
                            System.out.println("Estudiante " + id + " ha sido atendido y se va.");
                            this.notifyAll(); // Notifica al monitor que ya imprimió su mensaje
                        }

                    } else {
                        System.out.println("Estudiante " + id + " no encontro sillas libres y regresa a programar.");
                    }
                }
            } catch (InterruptedException e) {
                System.err.println("Error en Estudiante " + id + ": " + e.getMessage());
            }
        }
    }

    class Monitor extends Thread {
        @Override
        public void run() {
            try {
                while (true) {
                    monitorSemaphore.acquire(); // Espera a que un estudiante lo despierte

                    Estudiante estudiante;
                    synchronized (queueLock) {
                        estudiante = waitingQueue.poll(); // Atiende al primer estudiante en la cola
                    }

                    if (estudiante != null) {
                        System.out.println("El monitor esta despierto y atendera al estudiante " + estudiante.id);

                        synchronized (estudiante) {
                            estudiante.notifyAll(); // Llama al estudiante
                        }

                        Thread.sleep(ThreadLocalRandom.current().nextInt(1000, 3000)); // Simula el tiempo de ayuda

                        System.out.println("El monitor ha terminado de ayudar a un estudiante.");
                    }

                    if (waitingQueue.isEmpty()) {
                        System.out.println("El monitor no encuentra estudiantes y se va a dormir.");
                    }
                }
            } catch (InterruptedException e) {
                System.err.println("Error en Monitor: " + e.getMessage());
            }
        }
    }

    public static void main(String[] args) {
        MonitorDormilonSystem sistema = new MonitorDormilonSystem(3);
        Monitor monitor = sistema.new Monitor();
        monitor.start();

        for (int i = 1; i <= 5; i++) {
            Estudiante estudiante = sistema.new Estudiante(i);
            estudiante.start();
        }
    }
}