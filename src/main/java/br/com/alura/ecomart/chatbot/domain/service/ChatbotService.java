package br.com.alura.ecomart.chatbot.domain.service;

import org.springframework.stereotype.Service;

import com.theokanning.openai.completion.chat.ChatCompletionChunk;

import br.com.alura.ecomart.chatbot.infra.openai.DadosRequisicaoChatCompletion;
import br.com.alura.ecomart.chatbot.infra.openai.OpenAIClient;
import io.reactivex.Flowable;
import lombok.AllArgsConstructor;

@AllArgsConstructor
@Service
public class ChatbotService {

    private final OpenAIClient openAIClient;
    
    public Flowable<ChatCompletionChunk> responderPergunta(String pergunta) {
        String promptSistema = "Você é um chatbot de atendimento a clientes de um ecommerce e deve responder apenas perguntas relacionadas ao ecommerce. ";
        DadosRequisicaoChatCompletion dados = new DadosRequisicaoChatCompletion(promptSistema, pergunta);
        return openAIClient.enviarRequisicaoChatCompletion(dados);
    }
}
