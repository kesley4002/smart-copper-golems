# Smart Copper Golems Extended 0.9.12 — correção da profissão Agricultor

## Por que a 0.9.11 colhia sem recolher
1. O loop de coleta física do chão abortava quando o mob não era COLETOR. O AGRICULTOR iniciava a busca por itens, mas o estado WALK_TO_ITEM cancelava imediatamente.
2. A 0.9.11 priorizava um novo cultivo maduro ANTES dos itens dropados; o Agricultor podia deixar a produção para trás enquanto colhia outra planta.
3. O alcance para colher admitia até 3 blocos de distância; a movimentação usava ponto de aproximação otimizado para **baús**, não para plantas.

## Correções implementadas
- ORGANIZADOR: mantém comportamento anterior, sem coletar drops nem plantar.
- COLETOR: mantém seu transporte validado em 31/31 testes.
- AGRICULTOR: compartilha a coleta e o depósito filtrado dos drops do Coletor (sem mudar as regras de baús).
- PRIORIDADE: recolhe primeiro os drops com destino válido e espaço, só depois procura outra planta madura.
- MOVIMENTO: anda fisicamente até o cultivo (aproximadamente até 1,1 bloco horizontal do centro da planta) antes de colher, sem usar pontos de acesso a baús.
- PLANTAÇÕES: só seleciona planta madura em farmland arado, em chunk carregado, e exige pathfinding viável. Alvos inacessíveis são ignorados. Ao mudar a rota ou passar 240 ticks sem chegar, cancela com segurança.
- REPLANTIO: reserva 1 semente/cultivo REALMENTE obtido da colheita. Não cria material gratuito. Replantio em terra arada existente.
- DESTINO: continua exigindo moldura categorizada a 0 graus ou filtro exato a 45 graus, baú/barril com espaço dentro da área do golem.
- Sem teleporte, sem carregar chunks, sem tocar nos outros mods/arquivos de animação/ponte Bedrock.

## Atenção às categorias de armazenamento
Na tabela real de 42 categorias do mod:
- `minecraft:wheat`, `minecraft:wheat_seeds`, `minecraft:beetroot_seeds` usam a categoria `agriculture`.
- `minecraft:carrot`, `minecraft:potato`, `minecraft:beetroot`, `minecraft:poisonous_potato` usam `food_raw_vegetables`.
Logo, um único baú com moldura por categoria não necessariamente guarda os dois tipos de produto e semente. Prepare os destinos apropriados antes de testar. Sem destino, o drop ficará no mundo — essa é a política de filtragem existente, não deve haver depósito aleatório.

## Checklist de validação em cópia do mundo
1. Nomeie `Agricultor: João` e configure a área individual; verifique caminhada até a planta antes de colher.
2. Repita com trigo, cenoura, batata e beterraba maduros. Plântulas imaturas não devem ser quebradas.
3. Solte no chão trigo + sementes, cenouras, batatas, beterrabas + sementes. Confira se recolhe e distribui entre os depósitos com molduras corretas.
4. Observe uma colheita com várias plantas maduras: antes de colher outra, deve recolher os drops que têm destino.
5. Remova moldura/complete o baú: os itens sem destino devem permanecer no chão, e não serem duplicados ou destruídos.
6. Teste cultivo atrás de parede, em área inacessível, terra não arada e fora do raio individual.
7. Teste /scg night work on/off e raio noturno, múltiplos agricultores, alteração de etiqueta.
8. Teste um COLETOR e um ORGANIZADOR normais em paralelo para garantir ausência de regressões.
9. Conte os itens e sementes antes/depois de uma colheita; não deve existir dupe nem perda fora dos drops normais do jogo.
10. Reinicie servidor e cliente Bedrock via Geyser; reavalie movimento e animações.

## Escopo
A compostagem, coleta de farinha de osso e semear terras aradas vazias continuam fora deste hotfix. Não mesclar este PR antes de compilar e validar em jogo. Preserve o JAR 0.9.10 (base validada) e 0.9.11 (versão anterior) para rollback.
