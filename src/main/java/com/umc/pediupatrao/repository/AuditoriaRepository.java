package com.umc.pediupatrao.repository;

import com.umc.pediupatrao.entity.RegistroAuditoria;
import com.umc.pediupatrao.entity.enums.TipoOperacao;
import java.util.List;
import org.springframework.data.repository.Repository;

/**
 * Repositório da auditoria.
 *
 * Estende a interface base Repository (e NÃO MongoRepository) de propósito:
 * só existem os métodos declarados aqui. Não há save(), delete() ou update(),
 * então não existe caminho no código para alterar ou apagar um registro.
 * insert() só cria documentos novos.
 */
public interface AuditoriaRepository extends Repository<RegistroAuditoria, String> {

    <S extends RegistroAuditoria> S insert(S registro);

    List<RegistroAuditoria> findTop300ByOrderByDataHoraDesc();

    List<RegistroAuditoria> findTop300ByEntidadeOrderByDataHoraDesc(String entidade);

    List<RegistroAuditoria> findByEntidadeAndEntidadeIdOrderByDataHoraDesc(String entidade, String entidadeId);

    List<RegistroAuditoria> findTop300ByUsuarioOrderByDataHoraDesc(String usuario);

    List<RegistroAuditoria> findTop300ByOperacaoOrderByDataHoraDesc(TipoOperacao operacao);
}
