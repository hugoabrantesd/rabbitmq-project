package br.edu.fafic.classes;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

public class Barber extends Consumer {

    public Barber(String name, String email, String password) throws IOException, TimeoutException, URISyntaxException, NoSuchAlgorithmException, KeyManagementException {
        super(name, email, password);
    }

    public void execute() throws IOException, TimeoutException, InterruptedException {
        int op;
        while (true) {
            consume();
            System.out.print("\n======================================\n" +
                    "=========== APP BARBEARIA --> BARBEIRO ===========\n" +
                    "1- Ver agendamentos\n" +
                    "0- Sair\n" +
                    "==> ");
            op = this.getScanner().nextInt();
            if (op == 1) {
                if (!this.getAgendamentos().isEmpty()) {
                    acceptScheduling();
                } else {
                    System.out.print("\n---> Sem agendamentos por enquanto <---".toUpperCase());
                }
            } else {
                System.exit(0);
                break;
            }
        }
    }

    private void acceptScheduling() throws IOException, TimeoutException, InterruptedException {
        int num = this.getChannel().queueDeclare("response", false, false, false, null)
                .getMessageCount();
        System.out.println("\nAGENDAMENTOS:");
        for (int i = 0; i < this.getAgendamentos().size(); i++) {
            //lista todos os agendamentos.
            System.out.println("===========================================");
            System.out.print("ID: " + this.getAgendamentos().get(i).get("id") + "\n" +
                    "Cliente: " + this.getAgendamentos().get(i).get("nameClient") + "\n" +
                    "Data agendamento: " + this.getAgendamentos().get(i).get("dateScheduling"));
            System.out.println("\n");
        }

        System.out.print("1- Aceitar agendamento\n" +
                "2- Recusar agendamento\n" +
                "3- Enviar para fila de espera\n" +
                "0- Voltar ao menu principal\n" +
                "==> ");
        int opcao = this.getScanner().nextInt();
        Map<String, Object> ag;
        if (opcao == 1) {
            ag = getAgendamento();
            if (ag != null && !ag.isEmpty()) {
                validarConfirmacao("Agendamento confirmado para o barbeiro: " + this.getName(), ag);
            }
        } else if (opcao == 2) {
            ag = getAgendamento();
            if (ag != null) {
                this.getAgendamentos().remove(ag);
            }
            this.getChannel().queueDelete("response");
            System.out.print("\nAgendamento recusado!");
        } else if (opcao == 3) {
            ag = getAgendamento();
            if (ag != null) {
                enviarParaFilaEspera(ag);
            }
        }
    }

    private Map<String, Object> getAgendamento() {
        System.out.print("Digite o ID do cliente: \n" +
                "--> ");
        this.getScanner().nextLine();
        final String idCliente = this.getScanner().nextLine();
        for (int i = 0; i < this.getAgendamentos().size(); i++) {
            Map<String, Object> agend = this.getAgendamentos().get(i);
            if (idCliente.equals(agend.get("id").toString())) {
                return agend;
            }
        }
        return null;
    }

    private void enviarParaFilaEspera(Map<String, Object> agendamento) throws IOException {
        String json = new Gson().toJson(agendamento);
        this.getChannel().basicPublish("", this.getName(), null, json.getBytes());
        this.getAgendamentos().remove(agendamento);
        System.out.print("Agendamento enviado para a fila de espera!");
    }

    @Override
    public void consume() throws IOException {
        System.out.println("\n*** Aguardando novos agendamentos...");

        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            String str = new String(delivery.getBody());
            Type type = new TypeToken<Map<String, String>>() {
            }.getType();
            Map<String, Object> result = new Gson().fromJson(str, type);
            this.getAgendamentos().add(result);
        };
        this.getChannel().basicConsume(this.getName(), true, deliverCallback, consumerTag -> {
        });
    }

    private void validarConfirmacao(String msg, Map<String, Object> agendamento) throws IOException,
            InterruptedException, TimeoutException {
        //ConnectionFactory factory = new ConnectionFactory();
        //factory.setHost("localhost");
        //Connection connection = factory.newConnection();
//        factory.setHost("192.168.2.4");
//        factory.setPort(5672);
//        factory.setUsername("hugo");
//        factory.setPassword("13111992");
//        factory.setVirtualHost("/");
        //Channel channel = connection.createChannel();

        String queueResponse = "response";
        this.getChannel().queueDeclare(queueResponse, false, false, false, null);
        this.getChannel().basicPublish("", queueResponse,
                null, msg.getBytes());
        verificarSolicitacoes(agendamento);
    }

    private void verificarSolicitacoes(Map<String, Object> agendamento) throws IOException, InterruptedException, TimeoutException {
        String nomeCliente = agendamento.get("nameClient").toString();
        AtomicReference<String> response = new AtomicReference<>("");

//        ConnectionFactory factory = new ConnectionFactory();
//        factory.setHost("192.168.2.4");
//        factory.setPort(5672);
//        factory.setUsername("hugo");
//        factory.setPassword("13111992");
//        factory.setVirtualHost("/");
//        Connection connection = factory.newConnection();
//        Channel channel = connection.createChannel();

        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            String res = new String(delivery.getBody());
            response.set(res);
            if (response.get().contains(this.getName())) {
                try {
                    enviarResposta(nomeCliente);
                    this.getAgendamentos().remove(agendamento);
                } catch (TimeoutException | InterruptedException e) {
                    e.printStackTrace();
                }
            } else {
                this.getAgendamentos().remove(agendamento);
            }
        };

        this.getChannel().basicConsume("response", true, deliverCallback, consumerTag -> {
        });
        TimeUnit.SECONDS.sleep(2);
        if (response.get().isEmpty() || !response.get().contains(this.getName())) {
            System.out.println("\n*** Este agendamento já foi aceito por outro barbeiro!".toUpperCase());
            this.getAgendamentos().remove(agendamento);
            this.getChannel().queueDelete("response");
        }
    }

    public void enviarResposta(String nomeCliente) throws IOException, TimeoutException, InterruptedException {
        String message = "Agendamento confirmado para o barbeiro: " + this.getName();
        //1º exchange, 2º queue name
        this.getChannel().basicPublish("", nomeCliente,
                null, message.getBytes());

        System.out.println("Confirmação enviada!!!".toUpperCase());
        System.out.println("Por favor aguarde...");
        TimeUnit.SECONDS.sleep(1);
    }

    public void reporQueueAgendamentos(Map<String, Object> agendamento)
            throws IOException {

        //1º queue name
        this.getChannel().exchangeDeclare(Constants.EXCHANGE_SCHEDULES, "direct");
        this.getChannel().queueDeclare(this.getName(), false, false, false, null);
        this.getChannel().queueBind(this.getName(), Constants.EXCHANGE_SCHEDULES, Constants.BARBER_ROUTING_KEY);

        String message = new Gson().toJson(agendamento);

        //1º exchange, 2º queue name
        this.getChannel().basicPublish(Constants.EXCHANGE_SCHEDULES, Constants.BARBER_ROUTING_KEY,
                null, message.getBytes());
        this.getAgendamentos().remove(agendamento);

        System.out.println("Agendamentos repostos!!!");

    }
}
