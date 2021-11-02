package br.edu.fafic.classes;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.TimeoutException;

public abstract class Consumer {

    private String name;
    private String email;
    private String password;
    private final Scanner scanner;
    private final ArrayList<Map<String, Object>> agendamentos;
    private final Channel channel;

    public Consumer(String name, String email, String password) throws IOException, TimeoutException {
        this.name = name;
        this.email = email;
        this.password = password;
        this.scanner = new Scanner(System.in);
        this.agendamentos = new ArrayList<>();

        Channel channel = this.initConnection();
        channel.exchangeDeclare(Constants.EXCHANGE_SCHEDULES, "direct");
        channel.queueDeclare(this.getName(), false, false, false, null);
        channel.queueBind(this.getName(), Constants.EXCHANGE_SCHEDULES, Constants.BARBER_ROUTING_KEY);
        this.channel = channel;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Scanner getScanner() {
        return scanner;
    }

    public ArrayList<Map<String, Object>> getAgendamentos() {
        return agendamentos;
    }

    public Channel getChannel() {
        return this.channel;
    }

    public Channel initConnection() throws IOException, TimeoutException {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        Connection connection = factory.newConnection();
        //Channel channel = connection.createChannel();
        return connection.createChannel();
    }

    public abstract void consume() throws IOException, TimeoutException;

}
