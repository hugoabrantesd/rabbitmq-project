package br.edu.fafic.classes;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;

public class Barber extends Consumer {

    public Barber(String name, String email, String password) throws IOException, TimeoutException {
        super(name, email, password);
    }

    public void execute() throws IOException, TimeoutException {

        int op;
        while (true) {
            consume();
            System.out.print("\n======================================\n" +
                    "=== APP BARBEARIA ===\n" +
                    "1- Ver agendamentos\n" +
                    "0- Sair\n" +
                    "==> ");


            op = this.getScanner().nextInt();
            if (op == 1) {
                if (!this.getAgendamentos().isEmpty()) {
                    acceptScheduling();
                } else {
                    System.out.println("Sem agendamentos por enquanto.");
                }
            } else {
                System.exit(0);
                break;
            }
        }
    }

    private void acceptScheduling() throws IOException, TimeoutException {
        System.out.println("");
        for (int i = 0; i < this.getAgendamentos().size(); i++) {
            System.out.println("===========================================");
            System.out.println("ID: " + this.getAgendamentos().get(i).get("id") + "\n" +
                    "Cliente: " + this.getAgendamentos().get(i).get("nameClient") + "\n" +
                    "Data agendamento: " + this.getAgendamentos().get(i).get("dateScheduling") + "\n");
        }

        System.out.print("Digite o ID do cliente para aceitar o agendamento ou 0(zero) para sair:\n ==> ");
        int idDigitado = this.getScanner().nextInt();
        Map<String, Object> ag;
        if (idDigitado > 0) {
            ag = this.getAgendamentos().get(idDigitado - 1);
            if (ag != null && !ag.isEmpty()) {
                enviarResposta(ag.get("nameClient").toString());
                this.getAgendamentos().remove(ag);
            }
            if (!this.getAgendamentos().isEmpty()) {
                assert ag != null;
                reporQueueAgendamentos(this.getAgendamentos());
                this.getAgendamentos().clear();
            }
        } else {
            System.out.println("\nNenhum agendamento foi selecionado!\n");
            assert false;
            if (!this.getAgendamentos().isEmpty()) {
                for (int i = 0; i < this.getAgendamentos().size(); i++) {
                    String queue = this.getAgendamentos().get(i).get("nameClient").toString();
                    reporQueueAgendamentos(this.getAgendamentos());
                }
                this.getAgendamentos().clear();
            }

        }
    }

    @Override
    public void consume() throws IOException, TimeoutException {
        System.out.println("Recebendo mensagens...");

        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            String str = new String(delivery.getBody());
            Type type = new TypeToken<Map<String, String>>() {
            }.getType();
            Map<String, Object> result = new Gson().fromJson(str, type);
            this.getAgendamentos().add(result);
            //System.out.println("json: " + result.toString());
            //this.nomeCliente = result.get("nameClient").toString();
        };
        if (this.getChannel().isOpen()) {
            this.getChannel().basicConsume(this.getName(), true, deliverCallback, consumerTag -> {
            });
        }


    }

    public void enviarResposta(String queue) throws IOException, TimeoutException {

        final ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        try (Connection connection = factory.newConnection()) {
            Channel channel = connection.createChannel();

            String message = "Agendamento confirmado para o barbeiro: " + this.getName();

            channel.basicPublish("", queue, null, message.getBytes());

            System.out.println("Confirmação enviada!!!");

        }
    }

    public void reporQueueAgendamentos(List<Map<String, Object>> agendamentos)
            throws IOException, TimeoutException {

        final ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        try (Connection connection = factory.newConnection()) {
            Channel channel = connection.createChannel();
            //1º queue name
            channel.exchangeDeclare(Constants.EXCHANGE_SCHEDULES, "direct");
            channel.queueDeclare(this.getName(), false, false, false, null);
            channel.queueBind(this.getName(), Constants.EXCHANGE_SCHEDULES, Constants.BARBER_ROUTING_KEY);

            String message = new Gson().toJson(agendamentos);

            //1º exchange, 2º queue name
            channel.basicPublish(Constants.EXCHANGE_SCHEDULES, Constants.BARBER_ROUTING_KEY,
                    null, message.getBytes());

            System.out.println("Agendamentos repostos!!!");
        }
    }
}
