package com.finploit.android.ui.theme

import androidx.compose.runtime.compositionLocalOf

/**
 * Quem é quem no workspace do casal.
 *
 * Cada registo (transação, conta, recorrente) já traz o `userId` de quem o
 * criou — é a coluna por onde o `coupleUserIds` do servidor faz o escopo. Só
 * faltava traduzi-lo para um nome, e o perfil (`GET /contacts/profile`) traz
 * os dois nomes que existem no workspace: o próprio e o do cônjuge.
 *
 * Carregado uma vez no `MainViewModel` e distribuído por `LocalOwnerNaming`:
 * cada ecrã a pedir o perfil por sua conta seriam quatro pedidos para a mesma
 * resposta.
 */
data class OwnerNaming(
    val myId: Int? = null,
    val spouseId: Int? = null,
    val spouseName: String? = null,
) {
    /** Só há autoria a mostrar quando o workspace é de duas pessoas. */
    val isShared: Boolean get() = spouseId != null

    fun isMine(userId: Int?): Boolean = userId != null && myId != null && userId == myId

    /**
     * Nome curto de quem criou o registo, ou `null` quando não há nada útil a
     * dizer (workspace individual, ou registo sem `userId`) — quem chama usa o
     * `null` para não desenhar nada.
     */
    fun nameOf(userId: Int?): String? {
        if (!isShared || userId == null) return null
        return when (userId) {
            myId -> "Você"
            // O apelido não cabe num chip e não desambigua duas pessoas.
            spouseId -> spouseName?.trim()?.substringBefore(' ')?.takeIf { it.isNotEmpty() }
                ?: "Parceiro(a)"
            // Um id que não é nem o meu nem o do cônjuge não devia chegar aqui
            // (o servidor filtra), mas inventar um nome seria pior.
            else -> "Outro"
        }
    }
}

val LocalOwnerNaming = compositionLocalOf { OwnerNaming() }
