# Smart Copper Golems — Bedrock nativo (reescrita do zero)

## Objetivo da versão 0.0.1

**Diagnóstico funcional**, não uma versão completa do mod. Nenhum código, entidade ou definição visual foi copiado da versão Fabric ou do port Bedrock anterior. Esta primeira entrega usa **apenas Script API estável** e o Copper Golem **vanilla** do próprio Bedrock.

O Resource Pack foi criado do zero, mas é intencionalmente uma **base sem alterações de textura/animação**. Somente depois que a lógica de inventário for comprovada no jogo vamos acrescentar um modelo, animações e controlador Bedrock. Isso elimina conflitos entre as definições visuais antigas e o Copper Golem original.

## Instalação (Windows / iPhone / Android)

1. Faça um **mundo novo de testes**. Não use seu mundo principal.
2. Baixe o artifact `SmartCopperGolems_Native_0.0.1_Test` do workflow **Bedrock Native POC - validate and package** no GitHub Actions e extraia o ZIP.
3. Importe o `SmartCopperGolems_Native_0.0.1_Test.mcaddon` no Minecraft Bedrock.
4. Em **Configurações do mundo**, ative **Smart Copper Golems Native - Logic Test** (Behavior Pack) e **Smart Copper Golems Native - Visual Foundation** (Resource Pack).
5. Desative os packs anteriores **Smart Copper Golems Offline 0.1.0/0.1.1** e **Smart Copper Golems Extended - Bedrock visuals** no mundo e em Recursos Globais. Eles podem sobrescrever o Copper Golem vanilla.
6. Entre no mundo: deve aparecer **[SCG Native 0.0.1] Script Bedrock carregado!** no chat. Se não aparecer, envie o **Content Log**. A importação dos packs não prova que estão ativos no mundo.

## Teste mínimo de transporte

- Use um Copper Golem vanilla em terreno plano.
- A até 5 blocos dele, coloque um **baú de origem** com **1 graveto no primeiro espaço** e **16 diamantes no segundo espaço**.
- Coloque outro **baú de destino** também a até 5 blocos dele com **1 diamante no primeiro espaço**, os demais vazios.
- Espere alguns segundos. O protótipo deve transferir **1 diamante a cada ciclo**, mostrando no chat **[SCG Native] 1 item transferido com sucesso entre baús!**.
- O golem **não caminha até os baús nem anima** nesta etapa. O transporte é feito pelo script à distância, propositalmente, para isolar a prova de funcionamento da API de inventários. Não é a lógica final do mod.

## Se o teste falhar

Se a mensagem verde de inicialização não aparecer: confira se o Behavior Pack está **ativo no mundo** e veja erros no Content Log. Se aparecer mas não houver transferência: mande print do inventário dos dois baús e o Content Log. Se houver transferência, a próxima versão terá movimentação e animação nativa.

**Sem cheats** para usar a lógica; a compatibilidade de conquistas não é garantida em todas as versões do Bedrock. **CI testa sintaxe e empacotamento, não executa o Minecraft**.
