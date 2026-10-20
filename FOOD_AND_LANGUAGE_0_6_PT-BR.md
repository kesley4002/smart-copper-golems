# Smart Copper Golems Extended 0.6.0 — alimentos e ordenação PT-BR/EN

## O que muda

**35 categorias** para os mesmos **1413 IDs originais** do catálogo Survival vanilla Java 26.2. A antiga categoria geral de alimentos foi substituída por **seis filtros independentes**, e a carne podre foi deslocada para drops de mobs.

| Categoria | Exemplo na moldura girada 45° | Exemplos aceitos | IDs |
|---|---|---|---:|
| Carnes e peixes crus | `minecraft:beef` (bife cru) | frango cru, carneiro cru, peixes crus e baiacu | 9 |
| Carnes e peixes cozidos | `minecraft:cooked_beef` (filé) | frango assado, salmão assado, coelho assado | 7 |
| Vegetais crus | `minecraft:carrot` (cenoura) | batata, beterraba, cenoura dourada, batata venenosa | 5 |
| Vegetais preparados | `minecraft:baked_potato` (batata assada) | sopa de beterraba, ensopado de cogumelos, alga seca, ensopado suspeito | 5 |
| Frutas | `minecraft:apple` (maçã) | melancia, frutas doces e brilhantes, fruta-do-coro, maçãs douradas | 7 |
| Refeições e doces | `minecraft:bread` (pão) | bolo, biscoito, torta de abóbora, ensopado de coelho, mel | 6 |

**O aviso de migração é importante:** uma moldura de pão a 45° não aceita mais todos os alimentos. Agora ela representa apenas refeições/doces. Moldura sem giro (0°) continua sendo filtro exato de item.

### Idioma da organização alfabética

Somente jogadores com permissão OP/moderador (ou o console) podem usar os comandos:

- `/scg language pt` — nomes oficiais em português brasileiro.
- `/scg language en` — IDs ingleses originais (`minecraft:coal`, `minecraft:iron_ingot` etc.).
- `/scg language` — consulta a configuração atual.
- `/language pt`, `/language en`, `/language` — atalhos equivalentes; caso um outro mod utilize `/language`, prefira `/scg`.

A configuração **é única para todos os jogadores do mundo e persiste após reiniciar**. O arquivo é `smart-copper-golem-sort.properties` na pasta do mundo; valor padrão `en`. Os recipientes já existentes mudam de ordem **quando recebem um novo item por um golem**, e não imediatamente após o comando. Isso limita o custo da operação e mantém o fluxo testado.

### Nomes brasileiros

O arquivo `Fabric/src/main/resources/smart-copper-golem/item-names-pt_br.tsv` contém exatamente os 1413 nomes de itens/blocos. A origem dos nomes é a tradução oficial Minecraft PT-BR espelhada em `InventivetalentDev/minecraft-assets`, arquivos Minecraft 1.21.11. Foi conferida a existência de uma tradução de item ou bloco para **cada ID** do catálogo Java 26.2, sem lacunas. O servidor não depende do idioma do cliente.

No idioma PT-BR, a ordenação utiliza regras de comparação do português do Brasil sem diferenciar acentos. Itens com nomes iguais recebem desempate pelo ID. A compactação de pilhas continua separando componentes diferentes, encantamentos, nomes renomeados e durabilidade.

### Testes recomendados

1. Com cheats/OP, executar `/scg language pt` e conferir a resposta.
2. Depositar com golem ferro e ouro; comparar a ordem PT-BR (`Lingote de Ferro` antes de `Lingote de Ouro`) com EN (`gold_ingot` antes de `iron_ingot`).
3. Fechar e reabrir o mundo/servidor; consultar `/language` para confirmar que a configuração persistiu.
4. Entrar com usuário **sem OP** e confirmar que `/scg language` / `/language` não podem alterar o idioma.
5. Usar a Miriam pelo Bedrock: ambos devem enxergar os mesmos slots ordenados.
6. Testar as **seis novas categorias de alimentos**, comparando crus, cozidos, frutas e refeições.
7. Testar duas molduras iguais para a mesma categoria, baús comuns, baús duplos e barris.
8. Conferir que o barril abre, fecha e toca ambos os sons, e que vários golems não duplicam nem perdem itens.
9. Confirmar que a nova separação de alimentos não reclassificou nenhum ID não previsto e que todas as 35 categorias seguem disponíveis.

Versão experimental em PR separado: não mesclar ou publicar release antes da validação manual.
