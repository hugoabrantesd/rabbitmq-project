package br.edu.fafic.dao;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Dao {

    private static final List<Map<String, Object>> agendamentosAceitos = new ArrayList<>();

    public static void addAgendamento(Map<String, Object> agendamento) {
        agendamentosAceitos.add(agendamento);
    }

    public static List<Map<String, Object>> getAgendamentosAceitos() {
        return agendamentosAceitos;
    }

    public static void removerAgendamento(int idCliente) {
        Map<String, Object> agendamento = agendamentosAceitos.get(idCliente - 1);
        agendamentosAceitos.remove(agendamento);
    }

}
