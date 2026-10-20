# Extended 0.9.3 — Nether, End e Sculk separados

Base: Smart Copper Golems Extended 0.9.2. Preserva o mesmo catálogo de **1.413 IDs únicos** e todas as funcionalidades anteriores (origens por graveto, área 500×500, índice inteligente de molduras, armazenamento com filtro estrito, capacidade 16/32/64, categorias personalizadas, idioma PT/EN e animações).

A antiga categoria `special_blocks` tinha 37 IDs. Agora existem quatro filtros separados:

| Categoria | Identificador | Itens | Representante na moldura 0° |
|---|---|---:|---|
| Blocos Especiais | `special_blocks` | 17 | `minecraft:obsidian` |
| Blocos do Nether | `nether_blocks` | 11 | `minecraft:nether_bricks` |
| Blocos do End | `end_blocks` | 4 | `minecraft:purpur_block` |
| Blocos de Sculk | `sculk_blocks` | 5 | `minecraft:sculk` |

As 40 categorias anteriores passam a **43 categorias totais**, pois uma categoria foi dividida em quatro.

## Novo grupo especial — permanece com Obsidiana

`blue_ice`, `bone_block`, `chiseled_resin_bricks`, `creaking_heart`, `crying_obsidian`, `ice`, `obsidian`, `packed_ice`, `pointed_dripstone`, `resin_block`, `resin_brick`, `resin_bricks`, `resin_clump`, `snow`, `snow_block`, `snowball` e `sulfur_spike`.

## Nether

`chiseled_nether_bricks`, `cracked_nether_bricks`, `crimson_nylium`, `gilded_blackstone`, `magma_block`, `nether_brick_fence`, `nether_bricks`, `nether_wart_block`, `red_nether_bricks`, `warped_nylium`, `warped_wart_block`.

## End

`chorus_flower`, `popped_chorus_fruit`, `purpur_block` e `purpur_pillar`.

## Sculk

`sculk`, `sculk_catalyst`, `sculk_sensor`, `sculk_shrieker` e `sculk_vein`.

## Importante para a sala de baús

- A moldura a **0°** indica categoria, e **45°** indica o item exato; graveto a 0° continua sendo marcador de origem.
- O baú que exibe **Obsidiana a 0° não receberá mais os 20 itens redistribuídos**. Separe outros três baús com Tijolos do Nether, Bloco de Púrpura e Sculk nas molduras retas.
- Baús sem moldura ou com moldura vazia continuam proibidos como destinos.
- A separação se aplica aos itens que **já estavam na categoria especial**; não reclassifica materiais de outras categorias como terra das almas, netherrack ou end stone sem decisão explícita.
- A ordenação dos itens que já estão em baús não é feita automaticamente na mudança de versão. Os golems organizam as pilhas quando fazem um depósito válido, e é necessário mover itens já guardados em categoria antiga para os novos baús, caso desejado.
- Configurações personalizadas prevalecem sobre categorias padrão. Comandos para restaurar tudo continuam disponíveis, mas `/scg categories restore` apaga categorias personalizadas.

## Testes manuais recomendados

1. Faça backup do mundo e do mod 0.9.2 antes da instalação.
2. Monte quatro baús com as quatro molduras representativas em 0°.
3. Coloque no baú de origem (moldura com graveto 0°) Obsidiana, Resina, Tijolos do Nether, Púrpura e Sculk.
4. Confira que o golem usa os quatro destinos distintos, sem colocar nenhum desses itens em baús sem moldura.
5. Confirme que `/scg categories list` exibe 43 categorias padrão ativas.
6. Teste reiniciar o servidor e verificar se comandos de capacidade/idioma/categorias continuam persistentes.
7. Teste várias pilhas e dois golems ao mesmo tempo para conferir ausência de duplicação ou perda.

A build automatizada valida compilação, pacote Fabric e integridade do catálogo; movimentação no mundo deve ser verificada separadamente.
