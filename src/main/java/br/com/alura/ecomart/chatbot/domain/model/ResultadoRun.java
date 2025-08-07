package br.com.alura.ecomart.chatbot.domain.model;

import com.theokanning.openai.runs.Run;

public class ResultadoRun {
    private final Run run;
    private final boolean concluido;
    private final boolean precisaChamarFuncao;
    
    public ResultadoRun(Run run, boolean concluido, boolean precisaChamarFuncao) {
        this.run = run;
        this.concluido = concluido;
        this.precisaChamarFuncao = precisaChamarFuncao;
    }
    
    public Run run() {
        return this.run; 
    }

    public boolean concluido() {
        return this.concluido;
    }

    public boolean precisaChamarFuncao() {
        return this.precisaChamarFuncao; 
    }
}
