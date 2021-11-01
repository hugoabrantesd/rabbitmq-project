package br.edu.fafic.main;

import br.edu.fafic.classes.Barber;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

public class Barber1 {
    public static void main(String[] args) throws IOException, TimeoutException, InterruptedException {
        Barber barber1 = new Barber("Hugo", "hugo@gmail.com", "12345678");
        barber1.execute();
    }
}
