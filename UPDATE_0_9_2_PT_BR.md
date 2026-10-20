# Smart Copper Golems Extended 0.9.2 — nova versão experimental

Base do desenvolvimento: **Extended 0.9.1**, commit da branch `feature/books-alchemy-utilities-26.2`; não usa o JAR externo 1.2.0-beta.1. Inclui as **40 categorias originais e os 1.413 IDs** validados.

## 1. Destinos estritamente filtrados

- Baús e barris **sem moldura**, com moldura **vazia** ou com filtro **incompatível** nunca são destinos, mesmo quando o golem não encontra outro lugar.
- O golem mantém os itens consigo e avisa jogadores próximos no chat se não localizar depósito; mensagem exibe nome e ID (ex.: `minecraft:cooked_porkchop`) e a categoria, quando conhecida. Avisos da mesma pilha são limitados para não inundar o chat.
- Molduras retas **0° = categoria**; molduras a **45° = item exato**, como nas versões 0.9.x.
- **Graveto em 0° continua sendo origem**; o índice exclui esses recipientes da seleção de destinos. Baús de cobre permanecem fontes.
- O filtro é **revalidado antes de inserir**, inclusive se a moldura for removida durante o percurso.

## 2. Índice inteligente compartilhado por dimensão

Os golems consultam um índice das molduras com itens em entidades de chunks já carregados. São indexados separadamente:

- `categoria -> posições de baús/barris`
- `ID exato -> posições de baús/barris`
- `graveto em 0° -> posições de recipientes de origem`

O índice é reconstruído periodicamente (no máximo uma vez a cada 100 ticks por dimensão) e quando os comandos de categoria invalidam sua geração. O comando de girar todas as molduras também invalida o índice. Mudanças manuais em molduras podem levar até cinco segundos para aparecer. O servidor não força chunks distantes a carregar, e os golems ainda verificam a disponibilidade real de espaço e o filtro do destino selecionado — indexação não dispensa essas verificações de segurança.

## 3. Capacidade por viagem, OP

```mcfunction
/scg carry
/scg carry 16
/scg carry 32
/scg carry 64
```

Configuração do mundo em `smart-copper-golem-carry.properties`; o padrão 64 preserva o limite máximo atual da edição Extended. A capacidade só altera **quantos itens do mesmo tipo em uma pilha** o golem pode carregar: itens não empilháveis continuam limitados a 1 e pilhas com tamanho menor que 64 respeitam suas regras vanilla. Valores diferentes de 16, 32 e 64 são rejeitados pelo comando.

## 4. Categorias personalizadas, OP

```mcfunction
/scg categories list
/scg categories create blocos_laranja
/scg categories add blocos_laranja minecraft:orange_wool
/scg categories add blocos_laranja minecraft:orange_concrete
/scg categories remove blocos_laranja minecraft:orange_concrete
/scg categories delete blocos_laranja
```

As categorias criadas têm nomes minúsculos sem espaços; use sublinhado. Cada ID pode pertencer a **uma única categoria personalizada**, e esta prevalece sobre a categoria padrão enquanto existir. Coloque qualquer item associado à categoria em uma moldura **0°** para ativar o filtro desse grupo. A consulta de categorias é atualizada sem precisar reiniciar.

```mcfunction
/scg categories clear
/scg categories restore
```

**Atenção**: `clear` DESATIVA as 40 categorias padrão e apaga as personalizadas; `restore` RESTABELECE as 40 padrão e apaga quaisquer categorias personalizadas. Faça backup do mundo antes de usar essas duas operações. As categorias padrão não são excluídas do código — apenas desativadas na configuração do mundo. As mudanças são salvas em `smart-copper-golem-custom-categories.properties` e persistem após reiniciar. Adicionar IDs inexistentes no jogo cria uma associação inócua, não cria um novo item.

## 5. Baús subterrâneos e deslocamento

- O depósito remoto/instantâneo é **bloqueado** quando o destino está pelo menos três blocos abaixo do golem. Não atravessa chão sólido magicamente.
- O golem procura **escadas, escadas de mão e andaimes acessíveis** em uma região próxima (até 16 blocos em X/Z e cinco níveis abaixo), testa alguns caminhos navegáveis e tenta utilizá-los como pontos intermediários. A procura é limitada e realizada a cada 100 ticks.
- O caminho natural do Minecraft precisa existir: descidas muito distantes, trancadas ou sem acesso não podem ser garantidas pela busca local. Nesses casos, o golem conserva o item e informa a ausência de destino útil quando aplicável; não deve mover itens através do teto.
- Depósitos em níveis superiores continuam usando a abertura remota e sons já testados.

## Testes no seu servidor

1. Faça **backup** da pasta do mundo, dos arquivos de configuração e do JAR 0.9.1 antes de substituir.
2. Configure um baú com moldura de categoria 0° e outro sem moldura: todo o material deve ir exclusivamente para o primeiro.
3. Esvazie a moldura ou remova-a quando o golem estiver a caminho: o item não deve ser inserido ali.
4. Deixe somente destinos sem filtro: deve aparecer alerta com nome, ID e categoria, sem despejo em qualquer baú.
5. Confira transporte em `/scg carry 16`, depois 32 e 64; ao reiniciar, confirme a configuração.
6. Crie `blocos_laranja`, atribua dois IDs e teste uma moldura 0° de uma das entradas.
7. Teste `clear` e `restore` **somente em mundo de teste**, para não atrapalhar seus 64 baús.
8. Execute `/set item frames 0` próximo da base; verifique que gravetos em origem continuam preservados.
9. No subterrâneo, experimente um destino com escada caminhável e outro sem saída: não deve ocorrer depósito atravessando o teto.
10. Teste vários golems, baús duplos, barris e Geyser/Floodgate, observando logs/TPS.

Compilar não significa que todos os comportamentos em mundo real foram validados. Nenhum PR foi mesclado nem release publicada.
