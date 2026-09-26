package org.compi2.analisis_semantico;

import lombok.Getter;

@Getter
public class ContadorDesplazamiento {
    private int actual;

    public ContadorDesplazamiento() {
        this(0);
    }

    public ContadorDesplazamiento(int inicio) {
        this.actual = inicio;
    }

    public int asignar(int tamanoBytes) {
        int pos = this.actual;
        this.actual += tamanoBytes;
        return pos;
    }
}
