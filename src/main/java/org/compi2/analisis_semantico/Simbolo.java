    package org.compi2.analisis_semantico;

    import lombok.AllArgsConstructor;
    import lombok.Getter;
    import org.compi2.tipos.Tipo;

    @Getter
    @AllArgsConstructor
    public class Simbolo {
        private final int id;
        private final String nombre;
        private final Tipo tipo;
        private final String categoria;
        private final String ambito;
        private final int linea;
        private final int columna;
        private final int desplazamiento;

        @Override
        public String toString() {
            return String.format("[%d] %s (%s, %s) en %s - L:%d C:%d | Offset:%d",
                id, nombre, tipo.getNombreTipo(), categoria, ambito, linea, columna, desplazamiento);
        }
    }