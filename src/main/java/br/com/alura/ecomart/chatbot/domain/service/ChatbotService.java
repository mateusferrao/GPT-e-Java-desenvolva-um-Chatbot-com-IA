package br.com.alura.ecomart.chatbot.domain.service;

import java.util.List;

import org.springframework.stereotype.Service;

import br.com.alura.ecomart.chatbot.infra.openai.DadosRequisicaoChatCompletion;
import br.com.alura.ecomart.chatbot.infra.openai.OpenAIClient;
import lombok.AllArgsConstructor;

@AllArgsConstructor
@Service
public class ChatbotService {

    private final OpenAIClient openAIClient;
    
    public String responderPergunta(String pergunta) {
        String promptSistema = "Você é um chatbot de atendimento a clientes de um ecommerce e deve responder apenas perguntas relacionadas ao ecommerce. ";
        DadosRequisicaoChatCompletion dados = new DadosRequisicaoChatCompletion(promptSistema, pergunta);
        return openAIClient.enviarRequisicaoChatCompletion(dados);
    }

    public List<String> carregarHistorico() {
        return openAIClient.carregarHistoricoDeMensagens();
    }

    public void limparHistorico() {
        openAIClient.apagarThread();
    }
}
