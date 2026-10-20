# Smart Copper Golems Extended 0.9.5 — crescimento visual nativo (Java)

## Origem do movimento

O arquivo `Fabric/src/main/resources/assets/smart-copper-golem/animations/copper_golem.animation.json` contém os keyframes enviados pelo usuário no Blockbench, com primeiro deslocamento do corpo ajustado de Y=0.275 para zero conforme a correção relatada. A duração é **5,05 segundos**. Ossos: `body`, `right_leg`, `left_leg` (não substitui braços e cabeça). Escalas máximas das pernas: 13,8×; deslocamento final: 64 unidades do modelo.

## Java Fabric 26.2

O modelo original e as quatro texturas de oxidação continuam vanilla. Não é necessário EMF, ETF, OptiFine ou instalar um resource pack adicional no Java. **Instalar a mesma edição Extended no servidor e nos clientes Java** é necessário para exibir esta animação. O servidor continua controlando o inventário e o movimento, o cliente aplica a pose.

O servidor envia aos clientes que suportam o protocolo de animação o estado por uma mensagem Fabric `smart-copper-golem:stretch`: ID da entidade, fase (1=crescer, 2=manter, 3=encolher, 0=parar) e ticks de animação transcorridos. O renderizador lê esse estado durante os quadros; um mixin injeta a transformação dos ossos **depois** das animações vanilla.

O golem começa a crescer somente quando encontra um **baú elevado** acima de sua posição atual e chega ao momento em que faria o depósito remoto. Após crescer por 101 ticks, mantém a pose durante a abertura/depósito/fechamento. Depois encolhe por 101 ticks e só então libera a navegação. O tempo prévio de 100 ticks para reconhecer o estado travado permanece na versão inicial; isso deixa a sequência total mais lenta. Em mundos muito movimentados, o servidor pode variar o tempo em segundos devido ao TPS.

A escala é a altura **máxima da animação enviada**, ainda não proporcional à posição exata de cada baú. Essa é uma melhoria futura. O mod aumenta o limite de duração do comportamento para não interromper o movimento completo.

## Bedrock e Geyser

O arquivo de animação em si é compatível com a sintaxe de animação do Bedrock, mas não funciona sozinho na pasta `resource_packs`. É necessário construir um pacote com `manifest.json`, definições de entidade que preservem as variantes de oxidação, e um controlador de animação que receba o estado e dispare a sequência. **O Geyser não traduz automaticamente as mensagens personalizadas Fabric para eventos de animação Bedrock**, portanto a sincronização dessa animação ainda requer um módulo próprio ou outra ponte de integração. Esta 0.9.5 implementa a sincronização para CLIENTES JAVA FABRIC, não para Bedrock. Um arquivo `.mcpack` sem essa ponte teria o movimento apenas declarado, sem ser ativado no momento do depósito.

## Checklist antes de instalar num servidor de verdade

1. Faça backup do mundo e mantenha o 0.9.4 como rollback.
2. Instale o jar 0.9.5 nos clientes Java e no servidor; remova a edição anterior para não duplicar mods.
3. Em mundo de teste, use baús elevados de 2–5 blocos e filtros com moldura 0°.
4. Com itens na mão, confirme que o golem espera, cresce, interage, encolhe completamente e somente depois caminha.
5. Confirme que os braços continuam na animação vanilla e que o item não duplica/desaparece.
6. Compare variantes oxidada, parcialmente oxidada e normal.
7. Teste com dois golems e baús duplos; confirme a abertura e fechamento.
8. Confirme que baús acessíveis no chão continuam normais.
9. Teste Geyser/Bedrock: o sistema de baús deve continuar a funcionar, **mas a animação extra não aparecerá ainda**.
10. Confira o latest.log para possíveis erros de Mixin caso a API/mapeamento Java tenha mudado.

Não mesclar ou publicar uma release sem validação manual.
