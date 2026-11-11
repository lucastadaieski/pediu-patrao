package com.umc.pediupatrao.repository;

import com.umc.pediupatrao.entity.RegistroAuditoria;
import com.umc.pediupatrao.entity.enums.TipoOperacao;
import java.util.List;
import org.springframework.data.repository.Repository;

public interface AuditoriaRepository extends Repository<RegistroAuditoria, String> {

    <S extends RegistroAuditoria> S insert(S registro);

    List<RegistroAuditoria> findTop300ByOrderByDataHoraDesc();

    List<RegistroAuditoria> findTop300ByEntidadeOrderByDataHoraDesc(String entidade);

    List<RegistroAuditoria> findByEntidadeAndEntidadeIdOrderByDataHoraDesc(String entidade, String entidadeId);

    List<RegistroAuditoria> findTop300ByUsuarioOrderByDataHoraDesc(String usuario);

    List<RegistroAuditoria> findTop300ByOperacaoOrderByDataHoraDesc(TipoOperacao operacao);
}
