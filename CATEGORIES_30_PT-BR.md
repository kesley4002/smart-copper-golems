# Smart Copper Golems Extended 0.4.0 — as 30 categorias

Fonte: catálogo de Survival vanilla Java 26.2 fornecido pelo usuário (61 seções de origem e **1413 IDs únicos**). As categorias foram agrupadas sem perder IDs. O índice autoritativo do mod é `Fabric/src/main/resources/smart-copper-golem/category-index.tsv`. O resumo de origem e os totais estão em `category-manifest.tsv`.

## Como usar

- **Moldura a 0°**: continua filtrando exclusivamente o item exibido (com seus componentes).
- **Moldura com um giro, a 45°**: o item exibido identifica uma das **30 categorias**. O golem deposita nesse baú ou barril todos os itens do catálogo associados à categoria.
- A rotação 2–7 continua filtrando item exato, como antes.
- Baús comuns, duplos, baús-armadilha e barris continuam disponíveis como destinos. Baú de cobre é a origem.
- **Shulkers e bolsas são itens da categoria armazenamento portátil, NÃO recipientes de destino.**
- IDs de outros mods, comandos ou itens não catalogados não entram automaticamente em categorias; use um filtro exato (0°) ou um destino sem moldura preenchida.

## Categorias e item de referência

| Nº | Categoria | IDs catalogados | Exemplo de item na moldura a 45° |
|---:|---|---:|---|
| 1 | Pedras brutas e trabalhadas | 69 | `minecraft:stone` |
| 2 | Construção de pedra: escadas, lajes e muros | 121 | `minecraft:stone_stairs` |
| 3 | Terras, solos e cascalhos | 14 | `minecraft:dirt` |
| 4 | Blocos especiais e dimensionais | 37 | `minecraft:obsidian` |
| 5 | Minérios, metais e cristais | 58 | `minecraft:diamond` |
| 6 | Construções de cobre | 47 | `minecraft:cut_copper` |
| 7 | Madeiras: troncos, tábuas, escadas e lajes | 88 | `minecraft:oak_log` |
| 8 | Marcenaria: portas, cercas, placas e prateleiras | 109 | `minecraft:oak_door` |
| 9 | Vegetação: folhas, mudas, flores, fungos e musgos | 84 | `minecraft:oak_leaves` |
| 10 | Vidros e painéis | 35 | `minecraft:glass` |
| 11 | Lãs e tapetes | 32 | `minecraft:white_wool` |
| 12 | Concreto e pó de concreto | 32 | `minecraft:white_concrete` |
| 13 | Terracotas comuns e vitrificadas | 33 | `minecraft:terracotta` |
| 14 | Corantes | 16 | `minecraft:white_dye` |
| 15 | Decoração colorida: camas, velas e estandartes | 59 | `minecraft:white_bed` |
| 16 | Armazenamento portátil: shulkers e bolsas | 34 | `minecraft:shulker_box` |
| 17 | Corais e itens aquáticos | 37 | `minecraft:brain_coral` |
| 18 | Agricultura: sementes e colheitas | 15 | `minecraft:wheat_seeds` |
| 19 | Alimentos | 40 | `minecraft:bread` |
| 20 | Drops de mobs e tesouros | 48 | `minecraft:blaze_rod` |
| 21 | Redstone e circuitos | 26 | `minecraft:repeater` |
| 22 | Maquinaria e acessórios de cobre | 73 | `minecraft:copper_bulb` |
| 23 | Transportes e montarias | 62 | `minecraft:rail` |
| 24 | Ferramentas e armas | 51 | `minecraft:diamond_pickaxe` |
| 25 | Armaduras e moldes de ferraria | 47 | `minecraft:diamond_chestplate` |
| 26 | Livros, encantamentos, poções e navegação | 29 | `minecraft:enchanted_book` |
| 27 | Baús, bancadas e blocos funcionais | 41 | `minecraft:crafting_table` |
| 28 | Iluminação | 14 | `minecraft:torch` |
| 29 | Baldes e utilitários | 17 | `minecraft:bucket` |
| 30 | Música e arqueologia | 45 | `minecraft:music_disc_cat` |

**Soma verificada:** 1413 itens únicos, distribuídos em 30 categorias.

## Ajustes intencionais de classificação

Os 61 grupos do arquivo original foram reduzidos aos 30 grupos acima. Alguns itens foram corrigidos para uso mais intuitivo (sem perder IDs):

- `minecraft:stick` (graveto): **marcenaria**, não minerais.
- `minecraft:glistering_melon_slice`: **livros/encantamentos/alquimia**, como ingrediente de poção, não alimento.
- `minecraft:popped_chorus_fruit`: **blocos especiais/dimensionais**, não alimento consumível.
- `minecraft:sugar`: **agricultura**, como ingrediente de produção.
- `minecraft:carrot`, `minecraft:potato`, `minecraft:beetroot`, `minecraft:sweet_berries` e `minecraft:glow_berries`: **alimentos**, preservando a intuição dos filtros antigos.

## Migração da organização anterior

Os seis filtros originais foram repartidos/expandidos para atingir 30 grupos:

- A moldura de **diamante a 45°** continua aceitando ferro e carvão, mas agora inclui todos os outros minérios, metais e cristais da categoria.
- A moldura de **tronco de carvalho a 45°** continua aceitando madeiras brutas, tábuas, escadas e lajes; portas, cercas, placas e prateleiras vão para uma categoria nova, representada por uma **porta de carvalho**.
- A moldura de **pedregulho a 45°** seleciona pedras brutas/trabalhadas; escadas, lajes e muros de pedra agora ficam em uma categoria própria, representada por uma **escada de pedra**.
- **Pão, lã e vidro** continuam selecionando alimentos, lãs/tapetes e vidros/painéis, respectivamente.

Uma categoria não deve receber automaticamente itens de outra. Barris e baús sem moldura podem continuar sendo destinos de sobra. Para verificar a nova lógica, use baús ou barris de teste vazios antes de substituir os filtros da base.

## Testes automatizados

- O carregador rejeita duplicações de IDs e linhas malformadas.
- O teste JUnit confere **1413** entradas, **30** categorias, um exemplo de cada categoria, exceções importantes e igualdade entre índice e manifesto.
- O workflow do GitHub confere se o JAR final realmente contém ambos os recursos completos, cada item uma única vez e os totais exatos.
- O servidor registra a contagem carregada no log. Esses testes não substituem o teste de transporte real no Minecraft.

