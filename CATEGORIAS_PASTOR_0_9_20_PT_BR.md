# Smart Copper Golems Extended 0.9.20 — Revisão de categorias e Pastor de ovelhas

**Base preservada:** 0.9.19 (`fix/furnace-detection-from-0.9.18-0.9.19`), que inclui Agricultor, Fornalheiro e Coletor. Branch isolada. Não mesclar até testar.

## 1. Classificação de materiais

Alteramos o catálogo BASE, não apenas uma preferência de um mundo.

- `minecraft:stick`: `wood_crafting` → **`wood_shapes`**, com lajes e escadas de madeira.
- `minecraft:netherrack` e `minecraft:nether_brick`: `stone_blocks` → **`nether_blocks`**, junto dos tijolos, blocos vermelhos e outros materiais do Nether.
- **Basalto / blackstone** e suas variantes de pedra, escada, laje e muro: `nether_blocks`. Cercas, muros e escadas de tijolos do Nether também passam a ficar nesse grupo.
- `soul_sand` e `soul_soil`: `soil` → `nether_blocks`.
- **Madeira crimson/warped já estava correta**: troncos/hifas/tábuas em `wood`; lajes/escadas em `wood_shapes`; portas, cercas e portões em `wood_crafting`. Não mudamos essas madeiras.

**Atenção à moldura de origem:** graveto (`stick`) numa moldura normal com rotação **0°** permanece o marcador especial do baú de origem. Para identificar um baú da categoria `wood_shapes` use uma escada/laje como representante, não um graveto.

As categorias seguem sendo **42** e os IDs continuam sendo os mesmos **1.413**, sem duplicatas, com `category-manifest.tsv` e testes sincronizados. Baús com filtro em rotação de 0° passarão a refletir as novas famílias na próxima busca do golem; para isolar um item exato, use 45°. Configurações *personalizadas* de categorias de um mundo continuam tendo prioridade sobre o catálogo.

## 2. Profissão Pastor / Shepherd — tosquia automática

Etiquetas compatíveis: `Pastor: Pedro`, `Pastor de Ovelhas: Ana`, `Tosquiador: João`, `Shepherd: Alex` ou `Shearer: Kate`.

- O Pastor busca **ovelhas vivas prontas para tosquia**, dentro de um raio local de 12 blocos na horizontal e 4 na vertical, e sempre respeita a área individual configurada e o descanso noturno.
- Antes de cortar, verifica se existe **baú ou barril com moldura de lã correspondente e espaço disponível** dentro da área. Sem destino, não tosquia e não deixa lã espalhada propositadamente.
- Caminha fisicamente até a ovelha, se aproxima e usa a ação vanilla `shear`, que produz a lã real da cor da ovelha, com som de tosquia. A ovelha continua viva e precisa comer grama para regenerar lã.
- É uma **tesoura virtual integrada à profissão** (não exige tesoura no baú e não consome durabilidade). Se desejado, pode-se criar posteriormente uma versão que requer e gasta tesouras reais.
- **Recolhe exclusivamente os 16 blocos de lã de ovelha** e leva aos baús identificados, usando o mesmo sistema de molduras, limites de pilha, caminho e travas do Coletor.
- Não recolhe carpetes, carne de carneiro, couro, penas, ovos, carne podre ou qualquer outro drop. O Coletor e o Agricultor continuam inalterados.
- Lã de qualquer cor está na categoria `wool` do catálogo, que também contém os carpetes. **Para guardar todas as cores juntas**, use uma moldura de lã em 0°. **Para guardar cada cor separadamente**, use a moldura com a lã daquela cor em 45°.
- Nenhum novo item ou entidade precisa estar instalado no cliente Bedrock. A IA é processada no servidor Java, com a mecânica vanilla de tosquia e transporte.

## Checklist de validação em um BACKUP do mundo

1. Coloque 3–5 ovelhas em um cercado com grama acessível e um Pastor dentro dele. Certifique-se de que a horta e o galinheiro próximos também estão funcionando.
2. Identifique um baú/barril com uma moldura de lã a 0° para a categoria; teste um baú com lã branca a 45° para somente lã branca.
3. Observe o Pastor se aproximar de ovelhas com lã, tosquiar e recolher os drops (a quantidade segue a regra vanilla).
4. Pinte algumas ovelhas: a lã colorida deve ser guardada somente se houver filtro e espaço compatíveis.
5. Solte no chão lã, carpete, carneiro cru e carne podre: o Pastor deve buscar apenas lã.
6. Remova a moldura ou encha o baú: ele NÃO deve começar novas tosquias sem destino válido.
7. Abra o baú em que o Pastor deposita lã e verifique ausência de duplicação, perda de itens ou depósito sem aproximação.
8. Verifique o regrowth após as ovelhas comerem grama e o comportamento após reiniciar o servidor.
9. Confira stick com escadas/lajes, netherrack/nether bricks/blackstone com itens do Nether, madeiras e cercas crimson/warped com madeira.
10. Teste Coletor, Agricultor (inclusive composteira e bonemeal), Fornalheiro (inclusive comandos PT/EN) e Geyser/Bedrock.

**Status: experimental.** CI comprova compilação e testes automatizados; não substitui a validação do comportamento no Minecraft.
