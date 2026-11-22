import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

public class Main {

    public static void main(String[] args) {
        start();
    }

    private static void start() {
        Scanner sc = new Scanner(System.in);
        File[] archivos = new File[0];

        while (true) {
            System.out.println("=== Vertex Cover (V1 y V2) ===");
            System.out.println("Archivos seleccionados: " + formatearSeleccion(archivos));
            System.out.println("1. Seleccionar archivos desde carpeta 'Files'");
            System.out.println("2. Ejecutar V1 (cobertura de tamaño k) en archivos seleccionados");
            System.out.println("3. Ejecutar V2 (cobertura mínima) en archivos seleccionados");
            System.out.println("4. Ejecutar V1 y V2 en archivos seleccionados");
            System.out.println("0. Salir");
            System.out.print("Opción: ");

            String opcion = sc.nextLine().trim();
            System.out.println();

            switch (opcion) {
                case "1":
                    archivos = seleccionarArchivos(sc);
                    break;
                case "2":
                    if (archivos.length == 0) {
                        System.out.println("Primero seleccione al menos un archivo (opción 1).\n");
                    } else {
                        ejecutarLote(archivos, sc, true, false);
                    }
                    break;
                case "3":
                    if (archivos.length == 0) {
                        System.out.println("Primero seleccione al menos un archivo (opción 1).\n");
                    } else {
                        ejecutarLote(archivos, sc, false, true);
                    }
                    break;
                case "4":
                    if (archivos.length == 0) {
                        System.out.println("Primero seleccione al menos un archivo (opción 1).\n");
                    } else {
                        ejecutarLote(archivos, sc, true, true);
                    }
                    break;
                case "0":
                    return;
                default:
                    System.out.println("Opción inválida.\n");
            }
        }
    }

    private static File[] seleccionarArchivos(Scanner sc) {
        File folder = new File(Paths.get("Files").toAbsolutePath().toString());
        File[] dir = folder.listFiles(File::isFile);

        if (!folder.isDirectory() || dir == null || dir.length == 0) {
            System.out.println("Error al buscar la carpeta 'Files' o está vacía");
            System.out.println("Ruta esperada: " + folder.getAbsolutePath() + "\n");
            return new File[0];
        }

        Arrays.sort(dir);
        System.out.println("-- Archivos en el sistema. --");
        for (int i = 0; i < dir.length; i++) {
            System.out.println((i + 1) + ". " + dir[i].getName());
        }

        System.out.print("Formato: 1-2-3-...-n (o 'todos')\nIngrese los archivos para simular: ");
        String entrada = sc.nextLine().trim();

        List<File> sel = new ArrayList<>();
        if (entrada.equalsIgnoreCase("todos")) {
            sel.addAll(Arrays.asList(dir));
        } else {
            String[] temp = entrada.split("-");
            for (String t : temp) {
                if (t.trim().isEmpty()) continue;
                try {
                    int ind = Integer.parseInt(t.trim()) - 1;
                    if (ind < 0 || ind >= dir.length) {
                        System.out.println("Opción inválida ignorada: " + t.trim());
                    } else if (!sel.contains(dir[ind])) {
                        sel.add(dir[ind]);
                    }
                } catch (NumberFormatException e) {
                    System.out.println("Valor inválido ignorado: " + t.trim());
                }
            }
        }

        File[] archivos = sel.toArray(new File[0]);
        System.out.println("Archivos seleccionados: " + archivos.length);
        System.out.println();
        return archivos;
    }

    private static void ejecutarLote(File[] archivos, Scanner sc, boolean correrV1, boolean correrV2) {
        for (File archivo : archivos) {
            System.out.println("========================================");
            try {
                Grafo grafo = Grafo.cargarDesdeArchivo(archivo.getAbsolutePath());
                imprimirResumenGrafo(archivo.getName(), grafo);

                if (correrV1) {
                    System.out.print("Ingrese k para " + archivo.getName()
                            + " (1 a " + grafo.getNumVertices() + ", o Enter para omitir V1): ");
                    String entradaK = sc.nextLine().trim();
                    if (!entradaK.isEmpty()) {
                        int k = Integer.parseInt(entradaK);
                        ejecutarYMostrarV1(grafo, k);
                    }
                }

                if (correrV2) {
                    ejecutarYMostrarV2(grafo);
                }
            } catch (IOException e) {
                System.err.println("Error al leer " + archivo.getName() + ": " + e.getMessage());
            } catch (IllegalArgumentException e) {
                System.err.println("Error en " + archivo.getName() + ": " + e.getMessage());
            }
            System.out.println();
        }
    }

    private static String formatearSeleccion(File[] archivos) {
        if (archivos.length == 0) {
            return "(ninguno)";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < archivos.length; i++) {
            if (i > 0) sb.append(", ");
            sb.append(archivos[i].getName());
        }
        return sb.toString();
    }

    private static void imprimirResumenGrafo(String nombre, Grafo grafo) {
        System.out.println("Archivo: " + nombre);
        System.out.println("|V| = " + grafo.getNumVertices() + ", |A| = " + grafo.getNumAristas());
    }

    private static void ejecutarYMostrarV1(Grafo grafo, int k) {
        VertexCoverSolver.Resultado res = VertexCoverSolver.resolverV1(grafo, k);
        System.out.println("\n--- Resultado V1 (k = " + k + ") ---");
        if (res.existeSolucion()) {
            boolean valida = grafo.esCoberturaValida(res.getCobertura()) && res.getCobertura().size() == k;
            System.out.println("Existe cobertura: SÍ");
            System.out.println("Cobertura C:      " + res.getCobertura());
            System.out.println("Verificación:     " + (valida ? "VÁLIDA" : "ERROR"));
        } else {
            System.out.println("Existe cobertura: NO (no existe cobertura de tamaño " + k + ")");
        }
        System.out.printf("Tiempo:           %.4f ms (%d ns)%n", res.getTiempoMs(), res.getTiempoNanos());
        System.out.println("Nodos explorados: " + res.getNodosExplorados());
    }

    private static void ejecutarYMostrarV2(Grafo grafo) {
        VertexCoverSolver.Resultado res = VertexCoverSolver.resolverV2(grafo);
        boolean valida = grafo.esCoberturaValida(res.getCobertura());
        System.out.println("\n--- Resultado V2 (Cobertura Mínima) ---");
        System.out.println("Tamaño mínimo:    " + res.getCobertura().size());
        System.out.println("Cobertura C*:     " + res.getCobertura());
        System.out.println("Verificación:     " + (valida ? "VÁLIDA" : "ERROR"));
        System.out.printf("Tiempo:           %.4f ms (%d ns)%n", res.getTiempoMs(), res.getTiempoNanos());
        System.out.println("Nodos explorados: " + res.getNodosExplorados());
    }


}