# Smart Copper Golems Extended 0.5.0 — ordem alfabética por ID

## Novo comportamento

Após **cada depósito bem-sucedido** num destino (baú, baú-armadilha, baú duplo ou barril), o golem reorganiza o inventário inteiro, de A a Z pelo identificador de item em inglês, por exemplo: `minecraft:coal`, `minecraft:diamond`, `minecraft:emerald`, `minecraft:gold_ingot`, `minecraft:iron_ingot`.

- Todos os itens existentes no destino são incluídos, mesmo que já estivessem desorganizados antes da chegada do golem.
- Pilhas com mesmo ID **e componentes exatamente iguais** são consolidadas até o tamanho máximo permitido. Encantamentos, dano, nomes personalizados e propriedades distintas não se misturam.
- Os itens são compactados nos primeiros slots e os vazios ficam ao final. Dentro de um mesmo ID, a ordenação é estável (não força uma ordem arbitrária de componentes).
- A rotina só é executada se **algum item for realmente depositado**; não reorganiza na pesquisa ou enquanto o golem está parado.
- O depósito continua respeitando as molduras a 0°/45° e as 30 categorias; a ordenação não altera o destino escolhido.
- Nos baús duplos, os 54 slots são considerados em conjunto. Nos barris, os 27 slots.
- Depósitos físicos mantêm os sons e animações vanilla. Os depósitos remotos existentes em recipientes inacessíveis ordenam também, mesmo que não possam tocar a animação.
- Quando vários golems estão em operação, ordenação e inserção devem ocorrer na mesma seção protegida pelo bloqueio do destino.
- Não adiciona shulker boxes como destino.

## Testes manuais

1. Colocar ferro, ouro, carvão e diamante fora de ordem num barril. Fazer um golem inserir mais um item; verificar a ordem de A a Z pelos **IDs em inglês**.
2. Colocar 20+30 ferros em slots diferentes; depois de um depósito, deve restar uma única pilha de 50, respeitando o máximo por slot.
3. Colocar 55+36 ferros e mais um item; verificar duas pilhas 64+27.
4. Testar baú duplo com itens misturados nas duas metades, e baú-armadilha.
5. Testar itens não empilháveis, espadas encantadas ou renomeadas; diferentes componentes não podem ser fundidos.
6. Testar 3–5 golems trabalhando, contando **todos os itens** antes e depois para detectar duplicação ou perda.
7. Testar um destino cheio, itens rejeitados pela moldura e itens que não cabem; esses casos não devem forçar alteração do conteúdo.
8. Verificar sons de abertura e fechamento e a animação do barril.
9. Salvar, sair e voltar; os itens precisam permanecer no lugar.
10. Confirmar que os 30 filtros por categoria continuam funcionando.

O número do mod é `1.1.2-extended.0.5.0`. Trata-se de uma versão experimental: não deve substituir a cópia de segurança da versão anterior antes da validação manual.
