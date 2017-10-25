package Modules;

// Klasse for å putte avstands informasjon fra googles JSON i et objekt
public class Avstand {
    public String text;
    public int value;

    public Avstand(String text, int value) {
        this.text = text;
        this.value = value;
    }
}