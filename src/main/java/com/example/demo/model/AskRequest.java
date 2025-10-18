package com.example.demo.model;

import jakarta.validation.constraints.NotBlank;

public class AskRequest {
    @NotBlank(message = "question must not be blank")
    private String question;
    public String getQuestion() { return question; }
    public void setQuestion(String question) { this.question = question; }
}
