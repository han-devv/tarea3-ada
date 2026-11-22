import java.util.*;

public class VertexCoverSolver {

    public static class Resultado {
        private final boolean existeSolucion;
        private final List<String> cobertura;
        private final long tiempoNanos;
        private final long nodosExplorados;

        public Resultado(boolean existeSolucion, List<String> cobertura, long tiempoNanos, long nodosExplorados) {
            this.existeSolucion = existeSolucion;
            this.cobertura = cobertura;
            this.tiempoNanos = tiempoNanos;
            this.nodosExplorados = nodosExplorados;
        }

        public boolean existeSolucion() { return existeSolucion; }
        public List<String> getCobertura() { return cobertura; }
        public long getTiempoNanos() { return tiempoNanos; }
        public double getTiempoMs() { return tiempoNanos / 1_000_000.0; }
        public long getNodosExplorados() { return nodosExplorados; }
    }

    // =========================================================================
    // ALGORITMO V1: Cobertura de vértices de tamaño exacto k (0 < k <= |V|)
    // =========================================================================

    private static class EstadoV1 {
        long nodosExplorados = 0;
    }

    public static Resultado resolverV1(Grafo grafo, int k) {
        int n = grafo.getNumVertices();
        if (k <= 0 || k > n) {
            throw new IllegalArgumentException(
                    "El parámetro k debe cumplir 0 < k <= |V| (recibido k=" + k + ", |V|=" + n + ")."
            );
        }

        boolean[] enCobertura = new boolean[n];
        EstadoV1 estado = new EstadoV1();

        long inicio = System.nanoTime();
        boolean encontrada = backtrackV1(grafo.getAdyacencia(), enCobertura, 0, k, estado);
        long fin = System.nanoTime();

        if (!encontrada) {
            return new Resultado(false, Collections.emptyList(), fin - inicio, estado.nodosExplorados);
        }

        // Si se cubrieron todas las aristas con menos de k vértices, completar hasta tamaño exacto k
        int tamActual = contarMarcados(enCobertura);
        for (int i = 0; i < n && tamActual < k; i++) {
            if (!enCobertura[i]) {
                enCobertura[i] = true;
                tamActual++;
            }
        }

        return new Resultado(true, construirListaEtiquetas(grafo, enCobertura), fin - inicio, estado.nodosExplorados);
    }

    private static boolean backtrackV1(int[][] adj, boolean[] enCobertura, int tamActual, int k, EstadoV1 estado) {
        estado.nodosExplorados++;

        int[] seleccion = buscarVerticeMayorGradoNoCubierto(adj, enCobertura);
        int u = seleccion[0];
        int maxGrado = seleccion[1];

        // Caso base: todas las aristas están cubiertas
        if (maxGrado == 0) {
            return true;
        }

        // Poda: quedan aristas sin cubrir pero ya alcanzamos el límite k
        if (tamActual == k) {
            return false;
        }

        // Caso grado máximo = 1: las aristas restantes son disjuntas; basta elegir u sin ramificar
        if (maxGrado == 1) {
            enCobertura[u] = true;
            boolean ok = backtrackV1(adj, enCobertura, tamActual + 1, k, estado);
            if (ok) return true;
            enCobertura[u] = false;
            return false;
        }

        // Rama 1: incluir u en la cobertura
        enCobertura[u] = true;
        if (backtrackV1(adj, enCobertura, tamActual + 1, k, estado)) {
            return true;
        }
        enCobertura[u] = false;

        // Rama 2: excluir u => obligatoriamente incluir todos sus vecinos no cubiertos
        if (tamActual + maxGrado <= k) {
            int[] vecinos = obtenerVecinosNoCubiertos(adj[u], enCobertura, maxGrado);
            for (int v : vecinos) enCobertura[v] = true;

            if (backtrackV1(adj, enCobertura, tamActual + maxGrado, k, estado)) {
                return true;
            }

            for (int v : vecinos) enCobertura[v] = false;
        }

        return false;
    }

    // =========================================================================
    // ALGORITMO V2: Cobertura de vértices mínima (Branch & Bound)
    // =========================================================================

    private static class EstadoV2 {
        int mejorTam;
        boolean[] mejorCobertura;
        long nodosExplorados = 0;

        EstadoV2(int n) {
            this.mejorTam = n + 1;
            this.mejorCobertura = new boolean[n];
        }
    }

    public static Resultado resolverV2(Grafo grafo) {
        int n = grafo.getNumVertices();
        boolean[] enCobertura = new boolean[n];
        EstadoV2 estado = new EstadoV2(n);

        long inicio = System.nanoTime();
        branchAndBoundV2(grafo.getAdyacencia(), enCobertura, 0, estado);
        long fin = System.nanoTime();

        return new Resultado(
                true,
                construirListaEtiquetas(grafo, estado.mejorCobertura),
                fin - inicio,
                estado.nodosExplorados
        );
    }

    private static void branchAndBoundV2(int[][] adj, boolean[] enCobertura, int tamActual, EstadoV2 estado) {
        estado.nodosExplorados++;

        // Poda por cota superior
        if (tamActual >= estado.mejorTam) {
            return;
        }

        int[] seleccion = buscarVerticeMayorGradoNoCubierto(adj, enCobertura);
        int u = seleccion[0];
        int maxGrado = seleccion[1];

        // Caso base: todas las aristas están cubiertas y tamActual < estado.mejorTam
        if (maxGrado == 0) {
            estado.mejorTam = tamActual;
            estado.mejorCobertura = Arrays.copyOf(enCobertura, enCobertura.length);
            return;
        }

        // Poda: necesitamos al menos 1 vértice más para cubrir la arista incidente a u
        if (tamActual + 1 >= estado.mejorTam) {
            return;
        }

        // Caso grado máximo = 1: aristas restantes disjuntas (sin ramificación)
        if (maxGrado == 1) {
            enCobertura[u] = true;
            branchAndBoundV2(adj, enCobertura, tamActual + 1, estado);
            enCobertura[u] = false;
            return;
        }

        // Rama 1: incluir u (la primera rama completa actúa además como cota voraz inicial)
        enCobertura[u] = true;
        branchAndBoundV2(adj, enCobertura, tamActual + 1, estado);
        enCobertura[u] = false;

        // Rama 2: excluir u => incluir todos sus vecinos no cubiertos si puede mejorar el óptimo
        if (tamActual + maxGrado < estado.mejorTam) {
            int[] vecinos = obtenerVecinosNoCubiertos(adj[u], enCobertura, maxGrado);
            for (int v : vecinos) enCobertura[v] = true;

            branchAndBoundV2(adj, enCobertura, tamActual + maxGrado, estado);

            for (int v : vecinos) enCobertura[v] = false;
        }
    }

    // =========================================================================
    // Métodos auxiliares
    // =========================================================================

    private static int[] buscarVerticeMayorGradoNoCubierto(int[][] adj, boolean[] enCobertura) {
        int mejorU = -1;
        int maxGrado = 0;

        for (int u = 0; u < adj.length; u++) {
            if (enCobertura[u]) continue;
            int grado = 0;
            for (int v : adj[u]) {
                if (!enCobertura[v]) {
                    grado++;
                }
            }
            if (grado > maxGrado) {
                maxGrado = grado;
                mejorU = u;
            }
        }
        return new int[]{mejorU, maxGrado};
    }

    private static int[] obtenerVecinosNoCubiertos(int[] vecinosU, boolean[] enCobertura, int cantidad) {
        int[] resultado = new int[cantidad];
        int idx = 0;
        for (int v : vecinosU) {
            if (!enCobertura[v]) {
                resultado[idx++] = v;
            }
        }
        return resultado;
    }

    private static int contarMarcados(boolean[] arreglo) {
        int c = 0;
        for (boolean b : arreglo) {
            if (b) c++;
        }
        return c;
    }

    private static List<String> construirListaEtiquetas(Grafo grafo, boolean[] enCobertura) {
        List<String> lista = new ArrayList<>();
        for (int i = 0; i < enCobertura.length; i++) {
            if (enCobertura[i]) {
                lista.add(grafo.getEtiqueta(i));
            }
        }
        return lista;
    }
}