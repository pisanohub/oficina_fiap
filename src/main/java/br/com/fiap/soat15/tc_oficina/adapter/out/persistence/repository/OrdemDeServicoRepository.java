package br.com.fiap.soat15.tc_oficina.adapter.out.persistence.repository;

import br.com.fiap.soat15.tc_oficina.domain.entity.OrdemDeServico;
import br.com.fiap.soat15.tc_oficina.domain.entity.StatusOS;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OrdemDeServicoRepository extends JpaRepository<OrdemDeServico, Long> {

    List<OrdemDeServico> findByVeiculoClienteId(Long clienteId);

    boolean existsByNumero(String numero);

    boolean existsByIdAndVeiculoClienteId(Long id, Long clienteId);

    @Query("SELECT os FROM OrdemDeServico os WHERE os.dataInicioExecucao BETWEEN :dataInicial AND :dataFinal AND os.status = :status")
    List<OrdemDeServico> findByDataExecucaoBetweenAndStatusEquals(
            LocalDateTime dataInicial, LocalDateTime dataFinal, StatusOS status);

    @Query("""
            SELECT os FROM OrdemDeServico os
            WHERE os.status NOT IN (:statusExcluidos)
            ORDER BY
                CASE os.status
                    WHEN br.com.fiap.soat15.tc_oficina.domain.entity.StatusOS.EM_EXECUCAO THEN 1
                    WHEN br.com.fiap.soat15.tc_oficina.domain.entity.StatusOS.AGUARDANDO_APROVACAO THEN 2
                    WHEN br.com.fiap.soat15.tc_oficina.domain.entity.StatusOS.EM_DIAGNOSTICO THEN 3
                    WHEN br.com.fiap.soat15.tc_oficina.domain.entity.StatusOS.ABERTA THEN 4
                    ELSE 5
                END,
                os.dataAbertura ASC
            """)
    List<OrdemDeServico> findAtivasOrdenadasPorPrioridade(@Param("statusExcluidos") List<StatusOS> statusExcluidos);
}
