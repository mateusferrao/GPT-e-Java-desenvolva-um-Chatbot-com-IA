package br.com.alura.ecomart.chatbot.infra.openai;

import br.com.alura.ecomart.chatbot.domain.DadosCalculoFrete;
import br.com.alura.ecomart.chatbot.domain.model.ResultadoRun;
import br.com.alura.ecomart.chatbot.domain.service.CalculadorDeFrete;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.theokanning.openai.completion.chat.ChatFunction;
import com.theokanning.openai.completion.chat.ChatFunctionCall;
import com.theokanning.openai.completion.chat.ChatMessageRole;
import com.theokanning.openai.messages.Message;
import com.theokanning.openai.messages.MessageRequest;
import com.theokanning.openai.runs.Run;
import com.theokanning.openai.runs.RunCreateRequest;
import com.theokanning.openai.runs.SubmitToolOutputRequestItem;
import com.theokanning.openai.runs.SubmitToolOutputsRequest;
import com.theokanning.openai.service.FunctionExecutor;
import com.theokanning.openai.service.OpenAiService;
import com.theokanning.openai.threads.ThreadRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class OpenAIClient {

    private final String apiKey;
    private final String assistantId;
    private String threadId;
    private final OpenAiService service;
    private final CalculadorDeFrete calculadorDeFrete;

    public OpenAIClient(@Value("${app.openai.api.key}") String apiKey,
            @Value("${app.openai.assistant.id}") String assistantId, CalculadorDeFrete calculadorDeFrete) {
        this.apiKey = apiKey;
        this.service = new OpenAiService(apiKey, Duration.ofSeconds(60));
        this.assistantId = assistantId;
        this.calculadorDeFrete = calculadorDeFrete;
    }

    public String enviarRequisicaoChatCompletion(DadosRequisicaoChatCompletion dados) {
        var messageRequest = criarMessageRequest(dados);
        gerenciarThread(messageRequest);
        
        var run = criarEIniciarRun();
        var resultadoRun = aguardarConclusaoRun(run);
        
        if (resultadoRun.precisaChamarFuncao()) {
            processarChamadaDeFuncao(resultadoRun.run());
            aguardarConclusaoFinal(resultadoRun.run());
        }
        
        return obterUltimaMensagem();
    }

    private MessageRequest criarMessageRequest(DadosRequisicaoChatCompletion dados) {
        return MessageRequest
                .builder()
                .role(ChatMessageRole.USER.value())
                .content(dados.promptUsuario())
                .build();
    }

    private void gerenciarThread(MessageRequest messageRequest) {
        if (this.threadId == null) {
            criarNovaThread(messageRequest);
        } else {
            adicionarMensagemAThread(messageRequest);
        }
    }

    private void criarNovaThread(MessageRequest messageRequest) {
        var threadRequest = ThreadRequest
                .builder()
                .messages(Arrays.asList(messageRequest))
                .build();
        
        var thread = service.createThread(threadRequest);
        this.threadId = thread.getId();
    }

    private void adicionarMensagemAThread(MessageRequest messageRequest) {
        service.createMessage(this.threadId, messageRequest);
    }

    private Run criarEIniciarRun() {
        var runRequest = RunCreateRequest
                .builder()
                .assistantId(assistantId)
                .build();
        return service.createRun(threadId, runRequest);
    }

    private ResultadoRun aguardarConclusaoRun(Run run) {
        var concluido = false;
        var precisaChamarFuncao = false;
        
        try {
            while (!concluido && !precisaChamarFuncao) {
                Thread.sleep(1000 * 10);
                run = service.retrieveRun(threadId, run.getId());
                concluido = run.getStatus().equalsIgnoreCase("completed");
                precisaChamarFuncao = run.getRequiredAction() != null;
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        
        return new ResultadoRun(run, concluido, precisaChamarFuncao);
    }

    private void processarChamadaDeFuncao(Run run) {
        var precoDoFrete = chamarFuncao(run);
        var submitRequest = criarSubmitRequest(run, precoDoFrete);
        service.submitToolOutputs(threadId, run.getId(), submitRequest);
    }

    private SubmitToolOutputsRequest criarSubmitRequest(Run run, String precoDoFrete) {
        return SubmitToolOutputsRequest
                .builder()
                .toolOutputs(Arrays.asList(
                        new SubmitToolOutputRequestItem(
                                run
                                        .getRequiredAction()
                                        .getSubmitToolOutputs()
                                        .getToolCalls()
                                        .get(0)
                                        .getId(),
                                precoDoFrete)))
                .build();
    }

    private void aguardarConclusaoFinal(Run run) {
        var concluido = false;
        
        try {
            while (!concluido) {
                Thread.sleep(1000 * 10);
                run = service.retrieveRun(threadId, run.getId());
                concluido = run.getStatus().equalsIgnoreCase("completed");
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private String obterUltimaMensagem() {
        var mensagens = service.listMessages(threadId);
        return mensagens
                .getData()
                .stream()
                .sorted(Comparator.comparingInt(Message::getCreatedAt).reversed())
                .findFirst().get().getContent().get(0).getText().getValue()
                .replaceAll("\\\u3010.*?\\\u3011", "");
    }    

    private String chamarFuncao(Run run) {
        try {
            var funcao = run.getRequiredAction().getSubmitToolOutputs().getToolCalls().get(0).getFunction();
            var funcaoCalcularFrete = ChatFunction.builder()
                    .name("calcularFrete")
                    .executor(DadosCalculoFrete.class, d -> calculadorDeFrete.calcular(d))
                    .build();

            var executorDeFuncoes = new FunctionExecutor(Arrays.asList(funcaoCalcularFrete));
            var functionCall = new ChatFunctionCall(funcao.getName(),
                    new ObjectMapper().readTree(funcao.getArguments()));
            return executorDeFuncoes.execute(functionCall).toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public List<String> carregarHistoricoDeMensagens() {
        var mensagens = new ArrayList<String>();

        if (this.threadId != null) {
            mensagens.addAll(
                    service
                            .listMessages(this.threadId)
                            .getData()
                            .stream()
                            .sorted(Comparator.comparingInt(Message::getCreatedAt))
                            .map(m -> m.getContent().get(0).getText().getValue())
                            .collect(Collectors.toList()));
        }

        return mensagens;
    }

    public void apagarThread() {
        if (this.threadId != null) {
            service.deleteThread(this.threadId);
            this.threadId = null;
        }
    }
}
