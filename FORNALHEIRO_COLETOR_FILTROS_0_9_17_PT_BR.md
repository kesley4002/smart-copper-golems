# Smart Copper Golems Extended 0.9.17 — Fornalheiro + filtros de Coletor (teste)

Branch de teste criada A PARTIR da 0.9.16 (Agricultor unificado aprovado em jogo).
Nada foi mesclado no projeto principal. Mantidos 42 grupos, 1413 itens, Geyser,
animações, dia/noite, raios individuais e as profissões Organizador, Coletor, Agricultor.

## Categoria da farinha de osso (confirmada no catálogo)
- minecraft:bone_meal: `agriculture`
- minecraft:wheat_seeds: `agriculture`
- minecraft:beetroot_seeds: `agriculture`
- minecraft:egg: `mob_drops`

## Fornalheiro (nova profissão)

Name Tag: `Fornalheiro` ou `Fornalheiro: Carlos`.

- Procura fornalhas normais, altos-fornos e defumadores já construídos em uma área
  de até 12 blocos horizontalmente e 3 verticalmente (sem carregar chunks).
- O combustível deve estar num baú ou barril com moldura **normal da categoria**
  (`coal`/ `charcoal` em `mineral_gems`) ou moldura exata 45° do combustível.
  `coal_block` pertence à categoria `mineral_blocks`.
- Somente usa carvão, carvão vegetal ou bloco de carvão. NÃO fabrica carvão vegetal,
  NÃO coleta madeira, NÃO remove/insere itens nos slots de entrada ou resultado.
- Visita fisicamente o baú, tira combustível validado, anda até o forno e repõe
  **somente o slot inferior de combustível**.
- Abastece com até 8 carvões/carvões vegetais no slot; quando o estoque cair a
  4 ou menos, repõe até 8. Para bloco de carvão, máximo 1 bloco.
- Preserva ao menos **1 combustível em cada pilha** do recipiente de origem, e
  verifica filtro e bloqueio novamente antes de retirar.
- Se o forno desaparecer/mudar, ou a configuração perder validade durante o
  transporte, o golem mantém qualquer carga em sua mão para devolução pela
  rotina original de baú filtrado. Não há destruição de itens intencional.
- Respeita limite de área individual e descanso noturno já configurados.
- É diferente de um Carvoeiro: nenhuma produção de carvão a partir de madeira.

## Coletor com filtro individual (sem nova profissão)

Name Tag: `Coletor: Pedro` (ou `Coletor: OutroNome`).

Comandos de operador (OP):
- `/coletor Pedro item ovo` — Pedro recolhe SOMENTE ovos.
- `/coletor Pedro item minecraft:egg` — forma exata equivalente.
- `/coletor Pedro` — consulta filtro atual.
- `/coletor Pedro reset` — volta a coletar TODOS os itens do chão.
- `/scg collector "Coletor: Pedro" item minecraft:egg` — sintaxe alternativa.

A pesquisa usa apenas golems **carregados**. Nomes duplicados geram erro; não
altera o filtro do golem errado. O filtro é persistido por **UUID** no mundo
em `smart-copper-golem-collectors.properties`. Ao renomear, o filtro acompanha
o mesmo indivíduo. Um Coletor que não recebe comando CONTINUA coletando tudo.
O filtro afeta apenas a seleção de drops no chão, e não altera os baús e filtros
de item-frame já implementados.

Importante: para o Coletor guardar ovos precisa de um baú/barril com moldura
compatível. Ovos estão na categoria `mob_drops`; recomenda-se moldura de OVO a
45° no destino se deseja guardar ovos separados dos outros drops.

## Uma possível próxima profissão (NÃO implementada nesta versão)
`Tosquiador: Nome`: caminha até ovelhas com lã e usa tesoura, coleta a lã
caída e guarda nos baús filtrados; respeitaria cooldown da ovelha e durabilidade
da tesoura. Deve ser um PR separado, após validar esta versão.

## Testes no Minecraft, antes de usar em mundo principal
1. Testar o Agricultor completo (colhe, replanta, composteira, farinha de osso).
2. Nomear `Fornalheiro: Carlos`; montar fornalha normal, alto-forno e defumador
   com pouco combustível, baú de carvão com moldura e combustível sobrando.
3. Observar Carlos andar até baú, recolher carvão, andar até forno e repor o
   slot INFERIOR sem alterar entrada/resultado.
4. Quando o forno já tiver 8 carvões, o golem não deve repor. Se tiver 4 ou menos,
   deverá repor até 8.
5. Colocar apenas 1 carvão no baú: não deve esvaziá-lo automaticamente.
6. Checar funcionamento em baú duplo e barril com filtro, sem carregar chunks.
7. Nomear `Coletor: Pedro`; executar `/coletor Pedro item ovo`, soltar ovo,
   pena, carne podre: deve pegar SOMENTE ovo.
8. Reiniciar mundo e verificar persistência do filtro de Pedro; testar
   `/coletor Pedro reset` e confirmar que volta a recolher os demais itens.
9. Confirmar que Coletor genérico, Organizador e Agricultor não mudaram, nem
   animações Geyser/Bedrock, turnos, raio ou categorias.

**Não instalar ou mesclar como estável até compilação e teste in-game.**
