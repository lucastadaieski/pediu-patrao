package com.umc.pediupatrao.config;

import com.umc.pediupatrao.entity.enums.Perfil;
import com.umc.pediupatrao.entity.Usuario;
import com.umc.pediupatrao.repository.UsuarioRepository;
import java.util.Arrays;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.security.crypto.password.PasswordEncoder;


@Configuration
public class DataInitializer {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    @Bean
    CommandLineRunner initDatabase(UsuarioRepository repo, PasswordEncoder encoder, MongoTemplate mongoTemplate,
                                   @Value("${app.admin.username:admin}") String adminUsername,
                                   @Value("${app.admin.senha:}") String adminSenha) {
        return args -> {
            migrarPerfisAntigos(mongoTemplate);

            Optional<Usuario> existente = repo.findByUsername(adminUsername);
            if (existente.isPresent()) {
                Usuario admin = existente.get();
                if (admin.getRole() == null) {
                    admin.setRole(Perfil.ADMIN);
                    repo.save(admin);
                    log.info("Perfil ADMIN atribuído ao usuário '{}'.", adminUsername);
                }
                return;
            }

            if (adminSenha == null || adminSenha.isBlank()) {
                log.warn("ADMIN_PASSWORD não definido no .env: administrador inicial NÃO foi criado.");
                return;
            }
            repo.save(new Usuario(adminUsername, encoder.encode(adminSenha), Perfil.ADMIN));
            log.info("Usuário administrador '{}' criado.", adminUsername);
        };
    }

    private void migrarPerfisAntigos(MongoTemplate mongoTemplate) {
        String[] validos = Arrays.stream(Perfil.values()).map(Enum::name).toArray(String[]::new);
        Query perfisInvalidos = Query.query(Criteria.where("role").exists(true).ne(null).nin((Object[]) validos));
        long alterados = mongoTemplate.updateMulti(perfisInvalidos, Update.update("role", Perfil.ATENDENTE.name()),
                Usuario.class).getModifiedCount();
        if (alterados > 0) {
            log.info("{} usuário(s) com perfil antigo migrado(s) para ATENDENTE.", alterados);
        }
    }
}
