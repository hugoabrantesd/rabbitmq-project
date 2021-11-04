package br.edu.fafic.main;

import br.edu.fafic.classes.Barber;

import java.io.IOException;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.TimeoutException;

public class Barber2 {
    public static void main(String[] args) throws IOException, TimeoutException, InterruptedException, URISyntaxException, NoSuchAlgorithmException, KeyManagementException {
        Barber barber2 = new Barber("Taylson", "taylson@gmail.com", "87654321");
        barber2.execute();
    }
}
