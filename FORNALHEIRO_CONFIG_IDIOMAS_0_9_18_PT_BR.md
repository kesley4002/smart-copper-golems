# Smart Copper Golems 0.9.18 — combustível configurável e nomes/comandos bilíngues

Base exata: `feature/furnace-tender-and-collector-filters-0.9.17` (compilada, ainda requer teste em jogo). Esta branch é aditiva: **sem merge**, sem mudanças no Agricultor, Coletor original, animações, 42 categorias / 1413 itens, Geyser, raios ou turnos.

## Fornalheiro: quantidades 8, 16, 24, 32, 40, 48, 56 ou 64
- **Padrão anterior preservado:** 8 unidades de carvão ou carvão vegetal.
- Múltiplos exatos de 8, até 64 (máximo de uma pilha vanilla). Cada carvão rende 8 itens processados em forno normal, alto-forno ou defumador, conforme sua receita.
- O golem lê a meta escolhida na busca da tarefa, antes de tirar carvão do baú, e novamente ao inserir combustível no forno. Uma mudança durante o transporte não força um depósito maior que a nova meta.
- Como antes, abastece quando o slot está vazio ou com **até metade** da meta configurada. Por exemplo, meta 16 → se houver 8 ou menos, tenta completar até 16; meta 32 → quando houver 16 ou menos, completa até 32.
- O transporte de combustível do Fornalheiro agora pode levar até a meta completa em **uma viagem**, quando houver itens suficientes no baú. O limite genérico `/scg carry 16` não restringe a meta especial de carvão. O estoque mínimo de 1 combustível por pilha no baú de origem continua respeitado.
- **Bloco de carvão:** continua meta fixa de **1 bloco** no slot (ele já queima 80 itens). Não inventa produção de carvão nem gasta madeira.
- Nunca retira do slot de ingredientes e nunca altera o slot de resultados.
- Se não houver combustível suficiente na fonte, o golem usa o que estiver disponível acima da reserva e não excede a meta. A rotina mantém a regra de reabastecimento do protótipo 0.9.17.

## Configuração geral (todos os Fornalheiros sem ajuste individual)

| Português | English |
|---|---|
| `/fornalheiro combustivel 16` | `/furnace_tender fuel 16` |
| `/fornalheiro` | `/furnace_tender` |
| `/fornalheiro reset` | `/furnace_tender reset` |
| `/scg fornalheiro combustivel 24` | `/scg furnace fuel 24` |

É uma preferência armazenada por mundo, em `smart-copper-golem-furnaces.properties`, e sobrevive à reinicialização do servidor.

## Configuração individual (nome único por Name Tag)

Nome português: `Fornalheiro: Carlos`. Nome inglês: `Furnace Tender: Charles`.

| Português | English |
|---|---|
| `/fornalheiro Carlos combustivel 32` | `/furnace_tender Charles fuel 32` |
| `/fornalheiro Carlos` | `/furnace_tender Charles` |
| `/fornalheiro Carlos reset` | `/furnace_tender Charles reset` |

O valor individual **tem prioridade** sobre o global; reset individual volta ao valor global atual. Essa preferência acompanha o UUID do golem mesmo que ele seja renomeado. Se houver dois golems carregados com nomes repetidos, a operação falha, sem mudar ninguém. O comando só alcança entidades carregadas.

## Idiomas das profissões

| Etiqueta em português | Etiqueta em inglês | Função |
|---|---|---|
| `Coletor: Pedro` | `Collector: Peter` | Drops do chão, com filtro individual opcional |
| `Agricultor: João` | `Farmer: John` | Colheita, replantio, baús e compostagem / farinha de osso |
| `Fornalheiro: Carlos` | `Furnace Tender: Charles` | Suprimento de carvão / charcoal / coal block |

Aliases adicionais ingleses de Fornalheiro: `Furnace Keeper`, `Furnace Operator` e `Furnace`.

## Comandos bilíngues existentes, sem quebrar os originais

| Português | English |
|---|---|
| `/coletor Pedro item ovo` | `/collector Peter item egg` |
| `/coletor Pedro reset` | `/collector Peter reset` |
| `/agricultor compostagem ligar` | `/farmer compost on` |
| `/agricultor compostagem desligar` | `/farmer compost off` |
| `/agricultor reserva 32` | `/farmer reserve 32` |
| `/scg agricultor reserva 32` | `/scg farmer reserve 32` |

Os comandos antigos `/scg farmer ...`, `/scg collector ...`, `/coletor ...` e `/copper_golem "Nome" radius 30` permanecem disponíveis.

## Testes antes de mesclar (mundo de cópia)
1. Configure `/fornalheiro combustivel 16`; coloque pelo menos 17 carvões em baú com moldura compatível, deixe forno sem combustível: ele deve chegar a 16.
2. Com 9 carvões no forno e meta 16, ele não deve repor; com 8 ou menos, deve buscar combustível até a meta, quando houver estoque suficiente.
3. Configure `/fornalheiro Carlos combustivel 32`; verifique Carlos com 32 e outro Fornalheiro com a meta global 16. Depois resete Carlos.
4. Verifique que 10, 15, 65 são rejeitados e 8, 24, 40, 48, 56, 64 são aceitos.
5. Verifique bloco de carvão: meta sempre 1 mesmo com `fuel 64`.
6. Reinicie mundo e valide persistência do padrão/valores individuais.
7. Renomeie um golem para `Furnace Tender: Charles`; use `/furnace_tender Charles fuel 24` e confirme abastecimento.
8. Verifique `/collector Peter item egg` e `/farmer compost on` funcionando com Name Tags inglesas.
9. Certifique-se de que Agricultor, Coletor e Organizador continuem com seus comportamentos anteriores e que Bedrock/Geyser siga vendo as animações.

**Ainda experimental**: compilar primeiro e validar no Minecraft antes de mesclar. A branch 0.9.17 permanece intacta para rollback.
