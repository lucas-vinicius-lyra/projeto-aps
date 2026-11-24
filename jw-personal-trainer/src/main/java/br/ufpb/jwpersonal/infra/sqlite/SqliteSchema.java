
package br.ufpb.jwpersonal.infra.sqlite;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Cria a estrutura de tabelas e índices do sistema.
 */
public final class SqliteSchema {

    private SqliteSchema() {
    }

    /**
     * Cria todas as tabelas necessárias ao JW Personal Trainer.
     */
    public static void criarTabelas(ConexaoSQLite conexao) {

        SqliteSupport.executar(
                conexao,
                "criação do esquema",
                connection -> {

                    try (Statement statement =
                                 connection.createStatement()) {

                        for (String sql : comandosDDL()) {
                            statement.execute(sql);
                        }
                    }

                    return null;
                }
        );
    }

    private static String[] comandosDDL() {

        return new String[]{

                // USUÁRIOS
                """
                CREATE TABLE IF NOT EXISTS usuario (
                    id TEXT PRIMARY KEY,
                    nome TEXT NOT NULL,
                    email TEXT NOT NULL COLLATE NOCASE UNIQUE,
                    senha_hash TEXT NOT NULL,
                    perfil TEXT NOT NULL
                        CHECK (perfil IN ('PERSONAL', 'ALUNO')),
                    ativo INTEGER NOT NULL
                        CHECK (ativo IN (0, 1))
                )
                """,

                // EXERCÍCIOS
                """
                CREATE TABLE IF NOT EXISTS exercicio (
                    id TEXT PRIMARY KEY,
                    nome TEXT NOT NULL,
                    instrucoes TEXT,
                    video_url TEXT,
                    fonte_referencia TEXT,
                    ativo INTEGER NOT NULL
                        CHECK (ativo IN (0, 1))
                )
                """,

                // PLANOS
                """
                CREATE TABLE IF NOT EXISTS plano (
                    id TEXT PRIMARY KEY,
                    titulo TEXT NOT NULL,
                    objetivo TEXT NOT NULL,
                    personal_id TEXT NOT NULL,
                    FOREIGN KEY (personal_id)
                        REFERENCES usuario(id)
                        ON DELETE RESTRICT
                )
                """,

                // VERSÕES DOS PLANOS
                """
                CREATE TABLE IF NOT EXISTS versao_plano (
                    plano_id TEXT NOT NULL,
                    numero INTEGER NOT NULL CHECK (numero >= 1),
                    estado TEXT NOT NULL
                        CHECK (estado IN (
                            'RASCUNHO',
                            'PUBLICADA',
                            'ARQUIVADA'
                        )),
                    criada_em TEXT NOT NULL,
                    publicada_em TEXT,

                    PRIMARY KEY (plano_id, numero),

                    FOREIGN KEY (plano_id)
                        REFERENCES plano(id)
                        ON DELETE RESTRICT,

                    CHECK (
                        (estado = 'RASCUNHO'
                            AND publicada_em IS NULL)
                        OR
                        (estado IN ('PUBLICADA', 'ARQUIVADA')
                            AND publicada_em IS NOT NULL)
                    )
                )
                """,

                // UM ÚNICO RASCUNHO POR PLANO
                """
                CREATE UNIQUE INDEX IF NOT EXISTS
                    idx_versao_unico_rascunho
                ON versao_plano(plano_id)
                WHERE estado = 'RASCUNHO'
                """,

                // UMA ÚNICA VERSÃO PUBLICADA POR PLANO
                """
                CREATE UNIQUE INDEX IF NOT EXISTS
                    idx_versao_unica_publicada
                ON versao_plano(plano_id)
                WHERE estado = 'PUBLICADA'
                """,

                // TREINOS
                """
                CREATE TABLE IF NOT EXISTS treino (
                    plano_id TEXT NOT NULL,
                    numero_versao INTEGER NOT NULL,
                    nome TEXT NOT NULL COLLATE NOCASE,
                    ordem INTEGER NOT NULL,

                    PRIMARY KEY (
                        plano_id,
                        numero_versao,
                        nome
                    ),

                    UNIQUE (
                        plano_id,
                        numero_versao,
                        ordem
                    ),

                    FOREIGN KEY (plano_id, numero_versao)
                        REFERENCES versao_plano(plano_id, numero)
                        ON DELETE CASCADE
                )
                """,

                // ITENS DOS TREINOS
                """
                CREATE TABLE IF NOT EXISTS item_treino (
                    plano_id TEXT NOT NULL,
                    numero_versao INTEGER NOT NULL,
                    nome_treino TEXT NOT NULL COLLATE NOCASE,
                    ordem INTEGER NOT NULL,
                    exercicio_id TEXT NOT NULL,
                    series INTEGER NOT NULL
                        CHECK (series BETWEEN 1 AND 10),
                    repeticoes INTEGER NOT NULL
                        CHECK (repeticoes BETWEEN 1 AND 50),
                    carga TEXT NOT NULL,

                    PRIMARY KEY (
                        plano_id,
                        numero_versao,
                        nome_treino,
                        ordem
                    ),

                    FOREIGN KEY (
                        plano_id,
                        numero_versao,
                        nome_treino
                    )
                    REFERENCES treino(
                        plano_id,
                        numero_versao,
                        nome
                    )
                    ON DELETE CASCADE,

                    FOREIGN KEY (exercicio_id)
                        REFERENCES exercicio(id)
                        ON DELETE RESTRICT
                )
                """,

                // ATRIBUIÇÕES
                """
                CREATE TABLE IF NOT EXISTS atribuicao (
                    id TEXT PRIMARY KEY,
                    aluno_id TEXT NOT NULL,
                    plano_id TEXT NOT NULL,
                    numero_versao INTEGER NOT NULL,
                    inicio TEXT NOT NULL,
                    fim TEXT,
                    status TEXT NOT NULL
                        CHECK (status IN ('ATIVA', 'ENCERRADA')),

                    FOREIGN KEY (aluno_id)
                        REFERENCES usuario(id)
                        ON DELETE RESTRICT,

                    FOREIGN KEY (plano_id, numero_versao)
                        REFERENCES versao_plano(plano_id, numero)
                        ON DELETE RESTRICT,

                    CHECK (
                        (status = 'ATIVA' AND fim IS NULL)
                        OR
                        (status = 'ENCERRADA' AND fim IS NOT NULL)
                    )
                )
                """,

                // UMA ÚNICA ATRIBUIÇÃO ATIVA POR ALUNO
                """
                CREATE UNIQUE INDEX IF NOT EXISTS
                    idx_atribuicao_ativa_aluno
                ON atribuicao(aluno_id)
                WHERE status = 'ATIVA'
                """,

                // EXECUÇÕES
                """
                CREATE TABLE IF NOT EXISTS execucao (
                    id TEXT PRIMARY KEY,
                    aluno_id TEXT NOT NULL,
                    atribuicao_id TEXT NOT NULL,
                    plano_id TEXT NOT NULL,
                    numero_versao INTEGER NOT NULL,
                    nome_treino TEXT NOT NULL COLLATE NOCASE,
                    data TEXT NOT NULL,
                    status TEXT NOT NULL
                        CHECK (status = 'FINALIZADO'),
                    registrada_em TEXT NOT NULL,

                    FOREIGN KEY (aluno_id)
                        REFERENCES usuario(id)
                        ON DELETE RESTRICT,

                    FOREIGN KEY (atribuicao_id)
                        REFERENCES atribuicao(id)
                        ON DELETE RESTRICT,

                    FOREIGN KEY (plano_id, numero_versao)
                        REFERENCES versao_plano(plano_id, numero)
                        ON DELETE RESTRICT
                )
                """,

                // PROTEÇÃO CONTRA EXECUÇÕES DUPLICADAS
                """
                CREATE UNIQUE INDEX IF NOT EXISTS
                    idx_execucao_chave_natural
                ON execucao(
                    aluno_id,
                    plano_id,
                    numero_versao,
                    nome_treino COLLATE NOCASE,
                    data
                )
                """,

                // ÍNDICES DE CONSULTA
                """
                CREATE INDEX IF NOT EXISTS
                    idx_plano_personal
                ON plano(personal_id)
                """,

                """
                CREATE INDEX IF NOT EXISTS
                    idx_atribuicao_historico
                ON atribuicao(aluno_id, inicio DESC)
                """,

                """
                CREATE INDEX IF NOT EXISTS
                    idx_execucao_historico
                ON execucao(aluno_id, data DESC)
                """
        };
    }
}
