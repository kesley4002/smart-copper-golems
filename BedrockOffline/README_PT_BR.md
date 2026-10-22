# Smart Copper Golems Extended — Bedrock Offline 0.1.1 ALPHA

**Add-on Bedrock independente**. Nenhum Fabric, nenhum Geyser, nenhum Java ou servidor são necessários. O arquivo instalável `.mcaddon` inclui **Behavior Pack (scripts)** e **Resource Pack (modelo e animação)**. O `.mcpack` da integração Geyser **não** é necessário para jogos offline; está reaproveitado dentro desta versão.

## Instalação em Windows/Android Bedrock

**Importar não ativa o comportamento automaticamente.** Os dois packs devem estar habilitados nas configurações do mundo. Para investigar se os scripts carregaram e como testar o inventário dos baús, leia [DIAGNOSTICO_PT_BR.md](DIAGNOSTICO_PT_BR.md).

1. Faça backup do mundo atual. Recomendo criar **um mundo de testes**.
2. No GitHub Actions, baixe o artifact **SmartCopperGolems_Offline_0.1.1_alpha** e extraia o arquivo `.mcaddon`.
3. Abra o arquivo `.mcaddon` com o Minecraft Bedrock e aguarde a importação dos dois packs.
4. Ao criar/editar o mundo, ative os pacotes **Smart Copper Golems — Offline (Behavior)** e **Smart Copper Golems — Offline (Visual)**. Confira dependências e mensagens no jogo.
5. Faça um Copper Golem normalmente no **Survival**, colocando uma abóbora esculpida no bloco de cobre. O próprio `minecraft:copper_golem` recebe o comportamento offline: tanto os golems já existentes quanto os novos podem participar, com os dois packs ativos no mundo. Confirme no chat a mensagem verde `[Smart Copper Golems Offline 0.1.1] Scripts ativos`.
6. Monte um teste com um baú de origem e outro de destino com filtros (veja abaixo).

**Importante:** esta é uma ALPHA funcional em nível de lógica e empacotamento, ainda **não validada dentro do jogo**. Não prometemos compatibilidade com todas as edições/plataformas ou paridade total com o Java até o teste.

## Marcadores de baú para esta primeira versão Bedrock

O Bedrock Script API ainda não foi integrado ao sistema de rotações 0°/45° de molduras Java. O filtro Bedrock offline usa **slots reservados dentro dos baús**:

| Baú | Configuração | Restante |
| --- | --- | --- |
| **Origem 1** | Uma **vareta/stick no slot 1** (índice 0, primeiro espaço) de baú normal, baú de armadilha ou barril | Itens da origem nos espaços seguintes |
| **Origem 2** | Um **baú de cobre vanilla**, sem marcador | Quaisquer itens |
| **Destino de item exato** | **Slot 1:** uma amostra do item que deve entrar ali; **slot 2:** livre, sem papel | Itens a partir do slot 3 |
| **Destino de categoria** | **Slot 1:** uma amostra da categoria; **slot 2:** uma folha de papel | Itens a partir do slot 3 |

**Slot 1 = o primeiro quadradinho da interface do baú (índice 0 no código).** Os itens marcadores ficam reservados. Quando o segundo slot contém papel, o filtro usa a categoria da amostra, de acordo com o catálogo de 42 categorias/1.413 IDs importado do projeto Java. Nem todos os IDs do Java existem com a mesma nomenclatura no Bedrock; por isso, recomendamos começar pelo modo **item exato**. Itens sem entrada no catálogo continuam suportados no modo exato.

Exemplo: **origem:** baú comum com stick no primeiro slot e diamantes no terceiro; **destino:** baú comum com um diamante no primeiro slot e os demais vazios. O golem busca os diamantes e os transporta em lotes de até **16 unidades**, respeitando a capacidade de empilhamento.

## O que a alpha faz

- Aplica o comportamento modificado ao **Copper Golem vanilla**, inclusive aos golems existentes em mundos de teste; mantém propriedades de oxidação no visual.
- Procura baús carregados a até **9 blocos na horizontal e 5 na vertical**.
- Separa origem e destino com filtros explícitos. Ignora baús não marcados e evita usar outros compartimentos como destino arbitrário.
- Transporta itens em até **16 por viagem**; transfere os itens apenas **no momento da entrega**, com revalidação de origem/destino para reduzir risco de perda/duplicação.
- Move o golem em passos curtos somente se o chão e o espaço livre forem verificados. **Não usa teleporte através de paredes**.
- Mostra a animação original do usuário com alcance ajustado para baús de 2–5 blocos de altura, aproximadamente **2,6 s crescendo** e **2,6 s encolhendo**, sem depender do servidor.
- Mantém todos os dados de textura do Copper Golem vanilla e as animações de interação superiores declaradas pela Mojang.

## Limitações importantes — ainda não equivalem ao mod Java completo

- A movimentação ainda **não possui A* / escadas / navegação complexa**: o protótipo usa deslocamento horizontal cauteloso e pode desistir de destinos atrás de obstáculos.
- O golem **não segura visivelmente o item na mão** durante o trajeto na primeira versão; o item só é removido da origem no depósito para evitar perdas se a entidade desaparecer.
- O recurso de molduras 0°/45°, o catálogo Bedrock normalizado para todos os IDs, comandos `/scg`, múltiplas dimensões avançadas, baús distantes e personalização de categorias ainda precisam de portabilidade e testes adicionais.
- A animação de crescimento depende de o cliente reconhecer a entidade e os controladores Bedrock. Sintaxe JSON e JS foram validadas automaticamente, mas **testar em jogo é indispensável**.
- **Conquistas:** nenhum cheat é necessário para o uso normal, mas o Bedrock pode restringir conquistas com Behavior Packs de terceiros dependendo da versão. Não podemos assegurar que permaneçam ativas. **Teste num mundo cópia antes de ativar no mundo principal.**
- Outros Add-ons que substituem o Copper Golem podem causar conflitos.

## Desenvolvimento e build sem programas locais

A compilação e verificação sintática/estática são feitas em `.github/workflows/bedrock-offline-addon.yml`. O workflow verifica que os packs possuem UUIDs distintos, 1.413 categorias/IDs válidos no índice, 42 grupos, e valida `main.js` com TypeScript + `@minecraft/server@2.2.0`. Depois empacota `behavior_pack/` e `resource_pack/` num único `SmartCopperGolems_Offline_0.1.1_alpha.mcaddon`.

Fonte original da entidade e modelo visual: **Mojang bedrock-samples**. A animação das pernas deriva dos keyframes entregues pelo usuário no Blockbench. Projeto original Java Smart Copper Golems por GameMech007 (Apache-2.0), edição Extended e porta para Bedrock por Kesley. Código da nova lógica sob Apache-2.0 conforme licença do repositório.
