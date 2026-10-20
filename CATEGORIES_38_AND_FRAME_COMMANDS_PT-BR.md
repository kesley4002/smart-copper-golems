# Smart Copper Golems Extended 0.9.0 — organização de 38 categorias

Catálogo oficial preservado: **1413 IDs únicos**. A divisão anterior de 34 categorias passou a **38 categorias**, sem duplicar nem excluir nenhum item. Os detalhes e totais corretos vêm dos arquivos `category-index.tsv` e `category-manifest.tsv`.

## Regras das molduras — ATENÇÃO À INVERSÃO

- **0° (reto)**: categoria inteira do item exibido.
- **45° (um clique)**: apenas o item exibido (filtro exato).
- **90°, 135° etc.**: não correspondem a filtros válidos; use o comando de alinhamento.
- **Exceção indispensável**: **graveto em 0° continua marcando baú/barril de ORIGEM**, nunca destino. Graveto em 45° seleciona apenas o item graveto.
- As molduras de um baú duplo continuam compartilhadas pelas duas metades. Barris permitem molduras em qualquer face.

## Comandos OP para alinhar as molduras

```mcfunction
/scg set itemframes 0
/scg set itemframes 45
```

Atalhos adicionais: `/set item frames 0`, `/set item frames 45`, `/set itemframes 0` ou `/set itemframes 45`. São necessários poderes de OP/moderador, iguais a `/scg radius 500`.

O comando altera apenas as molduras **com itens** presentes nas entidades **já carregadas da dimensão atual**, sem gerar chunks nem editar o mapa inteiro. Molduras contendo **graveto são sempre preservadas**, para proteger os baús/barris de origem. O operador recebe a contagem de molduras alteradas, já ajustadas e preservadas.

**Cuidado**: esse comando não migra a finalidade de filtros exatos antigos; altera os ângulos de todos os filtros disponíveis. Antes de rodar, faça backup do mundo se houver filtros exatos que não deseja converter em categorias.

## Catálogo e itens representativos para molduras em 0°

| Categoria | IDs | Exemplo de item para moldura |
|---|---:|---|
| Pedras e blocos de cobre para construção | 100 | `minecraft:cobblestone` |
| Escadas, lajes e muros de pedra/cobre | 137 | `minecraft:stone_stairs` |
| Terras, solos e cascalhos | 14 | `minecraft:clay` |
| Blocos especiais e dimensionais | 37 | `minecraft:blue_ice` |
| Blocos compactados de minerais | 13 | `minecraft:diamond_block` |
| Gemas e recursos sem fundição | 15 | `minecraft:diamond` |
| Minérios brutos (inclusive em bloco) | 22 | `minecraft:raw_iron` |
| Lingotes, pepitas e metais refinados | 8 | `minecraft:iron_ingot` |
| Madeiras brutas, troncos e tábuas | 62 | `minecraft:oak_log` |
| Escadas e lajes de madeira | 26 | `minecraft:oak_stairs` |
| Acessórios de madeira: placas, botões, prateleiras e gravetos | 61 | `minecraft:oak_sign` |
| Marcenaria: portas, cercas e portões | 48 | `minecraft:oak_door` |
| Vegetação e blocos naturais | 83 | `minecraft:acacia_leaves` |
| Iluminação (exceto tocha de redstone) | 33 | `minecraft:copper_bulb` |
| Vidros e painéis | 35 | `minecraft:black_stained_glass` |
| Lãs e tapetes | 32 | `minecraft:black_carpet` |
| Concreto e pó de concreto | 32 | `minecraft:black_concrete` |
| Terracotas comuns e vitrificadas | 33 | `minecraft:black_glazed_terracotta` |
| Corantes | 16 | `minecraft:black_dye` |
| Decoração colorida e estátuas | 67 | `minecraft:black_banner` |
| Armazenamento portátil: shulkers e bolsas | 34 | `minecraft:black_bundle` |
| Corais e itens aquáticos | 37 | `minecraft:brain_coral` |
| Agricultura: sementes e colheitas | 15 | `minecraft:bamboo` |
| Frutas | 7 | `minecraft:apple` |
| Vegetais, batatas e algas | 7 | `minecraft:baked_potato` |
| Carnes e peixes crus | 9 | `minecraft:beef` |
| Refeições, sopas e doces | 9 | `minecraft:beetroot_soup` |
| Carnes e peixes cozidos | 7 | `minecraft:cooked_beef` |
| Drops de mobs e tesouros | 49 | `minecraft:rotten_flesh` |
| Componentes e comandos de redstone | 17 | `minecraft:repeater` |
| Máquinas de redstone e cobre | 16 | `minecraft:dispenser` |
| Recipientes, bancadas e blocos funcionais | 81 | `minecraft:copper_bars` |
| Transportes e montarias | 62 | `minecraft:acacia_boat` |
| Ferramentas e armas | 51 | `minecraft:bow` |
| Armaduras e moldes de ferraria | 47 | `minecraft:bolt_armor_trim_smithing_template` |
| Livros, encantamentos, poções e navegação | 29 | `minecraft:arrow` |
| Baldes e utilitários | 17 | `minecraft:axolotl_bucket` |
| Música e arqueologia | 45 | `minecraft:angler_pottery_sherd` |

## Mudanças relevantes

- Troncos, tábuas e madeira bruta **não** incluem escadas/lajes. Escadas e lajes de madeira estão no filtro separado de **escadas/lajes de madeira**; portas/cercas/portões seguem em **marcenaria**; placas, botões, prateleiras, placas de pressão e gravetos ficam com **acessórios de madeira**.
- Pedras comuns mantêm sua categoria; escadas, lajes e muros de pedra formam uma categoria única. Formas de cobre cortado passam a ser construção de pedra/cobre.
- Minérios foram divididos em **minérios brutos** (raw iron/copper/gold, minério no bloco), **refinados** (lingotes/pepitas), **gemas/recursos sem fundição** (diamante, lápis-lazúli, esmeralda, carvão, quartzo etc.) e **blocos compactados** (blocos de diamante, ferro, cobre, ouro etc.).
- As antigas categorias exclusivas `copper_building` e `copper_machines` foram removidas; itens redistribuídos para construção, máquinas, iluminação, decoração e blocos funcionais.
- Redstone de controle (repetidores, comparadores, botões, alavancas, sensores e **tochas de redstone**) permanece no filtro de componentes redstone. Dispensers, droppers, pistões, funis, crafter e para-raios de cobre ficam em **máquinas**.
- Lanternas e lâmpadas de cobre, tochas de cobre, lâmpadas redstone e shroomlight vão para **iluminação**. Glow lichen permanece em **vegetação natural**.
- Os nomes de exibição em PT-BR e a organização alfabética dos recipientes permanecem independentes dos filtros.

## Teste no servidor

1. Salve backup do mundo e do `.jar` da versão anterior.
2. Teste moldura de tronco a 0°: troncos e tábuas aceitos, escadas de madeira rejeitadas.
3. Teste escada de carvalho a 0°: escadas e lajes aceitas, madeira bruta rejeitada.
4. Teste porta de carvalho a 0°: portas, cercas e portões aceitos.
5. Teste minérios brutos, refinados, gemas e blocos compactados em quatro baús diferentes.
6. Teste redstone e máquinas separadamente; glow lichen deve permanecer na vegetação.
7. Com OP, teste `/scg set itemframes 0` em chunks carregados; verifique se baús marcados por gravetos continuam como origem.
8. Teste `/scg set itemframes 45` e confirme que os demais filtros ficaram exatos.
9. Confira a abertura/fechamento de baús e barris e que nenhum item foi perdido.
10. Confirme o comando e a lista de itens no servidor Fabric com clientes Bedrock.

Esta versão é experimental; não foi mesclada nem publicada sem aprovação.
