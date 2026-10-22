# Diagnóstico e instalação — Bedrock Offline 0.1.1

## Problema confirmado na versão anterior

O protótipo 0.1.0 atuava **somente** no identificador de entidade personalizado `scg:smart_copper_golem`. Golems vanilla presentes no mapa (`minecraft:copper_golem`) não eram convertidos depois da instalação; por isso pareciam completamente normais. Além disso, os filtros não eram os de molduras da edição Java, e esse detalhe podia fazer parecer que o Add-on não estava funcionando.

Na versão 0.1.1, os pacotes **substituem as definições de comportamento e renderização de `minecraft:copper_golem` diretamente**. O mesmo ID atende golems novos e antigos. As propriedades vanilla de oxidação, flor e interação foram preservadas no JSON.

## Confirme que os dois packs estão ATIVOS no mundo

Importar os arquivos para o Minecraft Bedrock no PC **não basta**. Você deve editar as configurações de um mundo de teste e ativar **Smart Copper Golems — Offline 0.1.1 (Behavior)** em *Pacotes de Comportamento* e **Smart Copper Golems — Offline 0.1.1 (Visual)** em *Pacotes de Recursos*. Se o jogo mostrar versões 0.1.0 anteriores, desative/remova-as antes de testar; a versão 0.1.1 usa o mesmo UUID com versão maior.

Abra/entre no mundo. No primeiro login após habilitar, a versão 0.1.1 deve enviar uma mensagem verde no chat:

`[Smart Copper Golems Offline 0.1.1] Scripts ativos...`

Se não aparecer: o Behavior Pack não foi ativado no mundo, o módulo de script não carregou, ou houve erro do pacote. Verifique o **Content Log**.

### Capturar o erro (Windows)

No Bedrock, Configurações > Criador (Creator) > Content Log: habilite *Content Log File* e *Content Log GUI*. Volte ao mundo e cheque mensagens de erro. Abra no Explorador do Windows:

`%APPDATA%\Minecraft Bedrock\logs\`

Envie o arquivo de log mais recente. A documentação Microsoft confirma este local para Bedrock Windows GDK.

## Teste objetivo sem cheats em mundo cópia

1. Ative os dois packs da versão 0.1.1 e confira a mensagem de scripts ativos.
2. Crie ou encontre um Copper Golem vanilla (mesmo antigo), em terreno plano, sem paredes.
3. Baú A, próximo ao golem: coloque **1 graveto no primeiro quadradinho**, **16 diamantes no terceiro**.
4. Baú B, a 3–5 blocos do Baú A: coloque **1 diamante no primeiro quadradinho** como filtro. Deixe os espaços seguintes vazios.
5. Aguarde o golem completar o ciclo. O algoritmo transporta para os slots **a partir do terceiro quadradinho** do destino. O graveto e a amostra não são consumidos.
6. Se não iniciar, envie o log e uma captura dos inventários dos baús.

O **resource pack do Geyser** `SmartCopperGolems_Bedrock_0.9.8.mcpack` é para **servidor Java**, não para offline. Para Bedrock offline instale apenas o novo `SmartCopperGolems_Offline_0.1.1_alpha.mcaddon`; ele contém tanto Behavior Pack como Resource Pack.

## Avisos

- O código permanece **Alpha**. Nenhum teste visual no Bedrock real foi executado pelo GitHub Actions; CI testa JSON, JS e módulos estáveis, não o motor de jogo.
- A lógica de filtros deste protótipo usa **slots internos**, não molduras com rotação 0°/45° como no Java. A movimentação é simples e pode falhar com escadas/paredes; o item não fica visível na mão.
- Sem testes no seu Bedrock, não garantimos que esta correção resolva todos os problemas. Favor enviar **Content Log** se aparecerem erros.
- Behavior Packs podem afetar o suporte a conquistas conforme versão/configuração. Não use como primeiro teste o mundo principal.
