package com.santec.polenta.service;

import opennlp.tools.tokenize.TokenizerME;
import opennlp.tools.tokenize.TokenizerModel;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;

@Service
public class TokenizerService {
    private TokenizerME tokenizer;

    @PostConstruct
    public void init() {
        try (InputStream modelIn = getClass().getResourceAsStream("/en-token.bin")) {
            TokenizerModel model = new TokenizerModel(modelIn);
            tokenizer = new TokenizerME(model);
        } catch (IOException e) {
            throw new RuntimeException("No se pudo cargar el modelo de tokenización", e);
        }
    }

    public String[] tokenize(String text) {
        if (tokenizer == null) {
            throw new IllegalStateException("El modelo de tokenización no está inicializado");
        }
        return tokenizer.tokenize(text);
    }
}

