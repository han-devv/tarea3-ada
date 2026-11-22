import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class Grafo {
    private final List<String> etiquetas;
    private final Map<String, Integer> indicePorEtiqueta;
    private final int[][] adyacencia;
    private final int numAristas;

    private Grafo(List<String> etiquetas, List<Set<Integer>> listaAdj, int numAristas) {
        this.etiquetas = Collections.unmodifiableList(etiquetas);
        this.indicePorEtiqueta = new HashMap<>();
        for (int i = 0; i < etiquetas.size(); i++) {
            this.indicePorEtiqueta.put(etiquetas.get(i), i);
        }
        this.adyacencia = new int[etiquetas.size()][];
        for (int i = 0; i < etiquetas.size(); i++) {
            Set<Integer> vecinos = listaAdj.get(i);
            this.adyacencia[i] = new int[vecinos.size()];
            int idx = 0;
            for (int v : vecinos) {
                this.adyacencia[i][idx++] = v;
            }
        }
        this.numAristas = numAristas;
    }

    /**
     * Carga un grafo no dirigido desde un archivo de texto.
     * - Líneas vacías y comentarios (# o //) se ignoran.
     * - Una línea con 1 token agrega un vértice aislado.
     * - Una línea con 2 tokens "u v" agrega la arista no dirigida {u, v}.
     */
    public static Grafo cargarDesdeArchivo(String rutaArchivo) throws IOException {
        List<String> lineas = Files.readAllLines(Paths.get(rutaArchivo), StandardCharsets.UTF_8);
        Set<String> conjuntoVertices = new LinkedHashSet<>();
        List<String[]> aristasCrud = new ArrayList<>();

        int numLinea = 0;
        for (String lineaOriginal : lineas) {
            numLinea++;
            String linea = lineaOriginal.trim();
            int idxHash = linea.indexOf('#');
            if (idxHash >= 0) linea = linea.substring(0, idxHash).trim();
            int idxSlash = linea.indexOf("//");
            if (idxSlash >= 0) linea = linea.substring(0, idxSlash).trim();

            if (linea.isEmpty()) continue;

            String[] tokens = linea.split("\\s+");
            if (tokens.length == 1) {
                conjuntoVertices.add(tokens[0]);
            } else if (tokens.length == 2) {
                String u = tokens[0];
                String v = tokens[1];
                conjuntoVertices.add(u);
                conjuntoVertices.add(v);
                if (u.equals(v)) {
                    System.err.println("Aviso (línea " + numLinea + "): se ignoró el lazo (" + u + ", " + v + ").");
                } else {
                    aristasCrud.add(new String[]{u, v});
                }
            } else {
                throw new IllegalArgumentException(
                        "Formato inválido en línea " + numLinea + ": \"" + lineaOriginal + "\". Use 'u v' o 'u'."
                );
            }
        }

        // Ordenar etiquetas (numéricamente si es posible) para salidas limpias
        List<String> etiquetas = new ArrayList<>(conjuntoVertices);
        etiquetas.sort((a, b) -> {
            try {
                return Long.compare(Long.parseLong(a), Long.parseLong(b));
            } catch (NumberFormatException e) {
                return a.compareTo(b);
            }
        });

        Map<String, Integer> mapaIndices = new HashMap<>();
        List<Set<Integer>> listaAdj = new ArrayList<>(etiquetas.size());
        for (int i = 0; i < etiquetas.size(); i++) {
            mapaIndices.put(etiquetas.get(i), i);
            listaAdj.add(new HashSet<>());
        }

        int aristasUnicas = 0;
        for (String[] par : aristasCrud) {
            int u = mapaIndices.get(par[0]);
            int v = mapaIndices.get(par[1]);
            if (listaAdj.get(u).add(v)) {
                listaAdj.get(v).add(u);
                aristasUnicas++;
            }
        }

        return new Grafo(etiquetas, listaAdj, aristasUnicas);
    }
    public boolean esCoberturaValida(Collection<String> cobertura) {
        boolean[] marcada = new boolean[getNumVertices()];
        for (String etiqueta : cobertura) {
            Integer idx = indicePorEtiqueta.get(etiqueta);
            if (idx == null) return false;
            marcada[idx] = true;
        }
        for (int u = 0; u < adyacencia.length; u++) {
            if (marcada[u]) continue;
            for (int v : adyacencia[u]) {
                if (!marcada[v]) return false;
            }
        }
        return true;
    }
    public int getNumVertices() { return etiquetas.size(); }
    public int getNumAristas() { return numAristas; }
    public int[][] getAdyacencia() { return adyacencia; }
    public String getEtiqueta(int indice) { return etiquetas.get(indice); }
}