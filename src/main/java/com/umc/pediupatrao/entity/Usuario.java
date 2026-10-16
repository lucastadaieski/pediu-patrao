package com.umc.pediupatrao.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.umc.pediupatrao.entity.enums.Perfil;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "usuarios")
public class Usuario {

    @Id
    private String id;

    private String username; // Nome de usuário para login

    // WRITE_ONLY: a senha (mesmo criptografada) nunca é devolvida em JSON
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password; // Senha criptografada (BCrypt)

    // Mantido o nome "role" para preservar os documentos já gravados no MongoDB
    private Perfil role;

    public Usuario() {
    }

    public Usuario(String username, String password, Perfil role) {
        this.username = username;
        this.password = password;
        this.role = role;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Perfil getRole() {
        return role;
    }

    public void setRole(Perfil role) {
        this.role = role;
    }

    @Override
    public String toString() {
        return "Usuario{id='" + id + "', username='" + username + "', role=" + role + "}";
    }
}
