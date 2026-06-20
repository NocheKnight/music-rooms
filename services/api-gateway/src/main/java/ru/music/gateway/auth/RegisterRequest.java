package ru.music.gateway.auth;

public record RegisterRequest(String username, String password, String email) {}
