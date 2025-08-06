package br.com.alura.ecomart.chatbot.web.controller;

import br.com.alura.ecomart.chatbot.domain.service.ChatbotService;
import br.com.alura.ecomart.chatbot.web.dto.PerguntaDto;
import lombok.AllArgsConstructor;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@AllArgsConstructor
@Controller
@RequestMapping({"/", "chat"})
public class ChatController {

    private static final String PAGINA_CHAT = "chat";
    private final ChatbotService chatbotService;

    @GetMapping
    public String carregarPaginaChatbot(Model model) {
        var mensagens = chatbotService.carregarHistorico();
        model.addAttribute("historico", mensagens);
        return PAGINA_CHAT;
    }

    @PostMapping
    @ResponseBody
    public String responderPergunta(@RequestBody PerguntaDto dto) {
        return chatbotService.responderPergunta(dto.pergunta());
    }

    @GetMapping("limpar")
    public String limparConversa() {
        chatbotService.limparHistorico();
        return "redirect:/chat";
    }

}
