package com.umc.pediupatrao.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;

@Configuration
public class MongoInitConfig implements CommandLineRunner {

    private final MongoTemplate mongoTemplate;

    public MongoInitConfig(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public void run(String... args) throws Exception {
        if (!mongoTemplate.collectionExists("pedidos")) {
            mongoTemplate.createCollection("pedidos");
        }
        if (!mongoTemplate.collectionExists("clientes")) {
            mongoTemplate.createCollection("clientes");
        }
        if (!mongoTemplate.collectionExists("usuarios")) {
            mongoTemplate.createCollection("usuarios");
        }
        if (!mongoTemplate.collectionExists("produtos")) {
            mongoTemplate.createCollection("produtos");
        }
        if (!mongoTemplate.collectionExists("auditoria")) {
            mongoTemplate.createCollection("auditoria");
        }
        if (!mongoTemplate.collectionExists("horariosAtendimento")) {
            mongoTemplate.createCollection("horariosAtendimento");
        }

        // Índice usado pelo dashboard: toda consulta filtra os pedidos por período de entrada.
        // Se o índice já existir, o MongoDB não faz nada.
        mongoTemplate.indexOps("pedidos")
                .ensureIndex(new Index().on("dataHoraEntrada", Sort.Direction.DESC).named("idx_pedidos_entrada"));
    }
}
