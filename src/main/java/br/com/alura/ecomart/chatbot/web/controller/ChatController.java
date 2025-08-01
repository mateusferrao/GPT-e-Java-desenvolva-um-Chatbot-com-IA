package br.com.alura.ecomart.chatbot.web.controller;

import br.com.alura.ecomart.chatbot.domain.service.ChatbotService;
import br.com.alura.ecomart.chatbot.web.dto.PerguntaDto;
import io.reactivex.Flowable;
import lombok.AllArgsConstructor;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;

import com.theokanning.openai.completion.chat.ChatCompletionChunk;

@AllArgsConstructor
@Controller
@RequestMapping({"/", "chat"})
public class ChatController {

    private static final String PAGINA_CHAT = "chat";
    private final ChatbotService chatbotService;

    @GetMapping
    public String carregarPaginaChatbot() {
        return PAGINA_CHAT;
    }

    @PostMapping
    @ResponseBody
    public ResponseBodyEmitter responderPergunta(@RequestBody PerguntaDto dto) {
        Flowable<ChatCompletionChunk> fluxoResposta = chatbotService.responderPergunta(dto.pergunta());
        ResponseBodyEmitter emitter = new ResponseBodyEmitter();
        fluxoResposta.subscribe(chunk -> {
            String token = chunk.getChoices().get(0).getMessage().getContent();
            if (token != null) {
                emitter.send(token);
            }
        }, emitter::completeWithError, emitter::complete);
        return emitter;
    }

    @GetMapping("limpar")
    public String limparConversa() {
        return PAGINA_CHAT;
    }

}
