# Smart Copper Golems Extended — Agricultor 0.9.14

Base: 0.9.13 `feature/farmer-compost-0.9.13`. Esta branch de correção foi criada sem substituir a versão anterior. Não modifica os 42 grupos e 1413 itens, o Coletor/Organizador, os filtros 0°/45° nem a ponte Geyser.

## 1. Como montar a composteira — ficou mais simples
A **composteira em si não precisa de moldura**.

1. Ponha uma composteira acessível ao Agricultor, próxima (até 3 blocos horizontalmente e 1 na vertical) de um baú/barril onde ele guarda trigo, sementes, cenouras, batatas ou beterrabas.
2. Esse recipiente precisa continuar com uma moldura **normal de destino**, seja 0° para a categoria ou 45° para o item exato. O Agricultor agora reconhece essa moldura como autorização para compostar os **excedentes correspondentes ao filtro**.
3. A segunda moldura com item COMPOSTEIRA em 90° no baú continua aceita, mas **não é mais obrigatória**. Se quiser marcar um baú específico só para adubo, ainda pode utilizar a marcação opcional.
4. Use `/scg farmer compost on` (a compostagem é desligada por padrão) e `/scg farmer reserve 32`. O comando `/scg farmer` mostra se está ativo.
5. Para recolher farinha de osso automaticamente, deixe outro baú ou barril com moldura compatível para farinha de osso e espaço disponível.

**Exemplo:** 40 sementes no baú com filtro e reserva 32 permitem compostar no máximo 8, *não garantem* encher a composteira por causa da chance vanilla. Mantenha mais excedentes se precisar de produção constante.

A reserva é por **item individual e por recipiente**, não uma reserva global entre os seus depósitos. Outros itens e baús sem filtro ficam protegidos. Um baú marcado como origem por graveto também NÃO será usado como insumo da composteira.

## 2. Compostagem acontece mesmo quando a horta está produtiva
- Os itens já caídos continuam tendo prioridade.
- Periodicamente (até aproximadamente 3 segundos entre buscas quando livre), o Agricultor procura uma composteira que precisa de serviço **antes** de escolher a próxima planta madura.
- O pathfinding agora procura um local **ao lado** da composteira, não no bloco sólido dela.
- O Agricultor só interage quando se aproxima da estação.
- Usa apenas 1 item por interação e respeita a reserva. Revalida o filtro antes do consumo.
- Quando pronta, só retira farinha de osso se houver destino com moldura e espaço. O item extraído cai no chão e é recolhido pela mesma rotina usada na colheita.

## 3. Baús mais naturais (somente Agricultor)
- Para baús/barris normais, o Agricultor caminha até ficar a até aproximadamente 1,8 bloco horizontalmente e 1,5 bloco verticalmente, antes de abrir e depositar.
- O fallback que fazia depósitos à distância foi **desativado SOMENTE para Agricultor**; Coletor e Organizador mantêm o comportamento anterior.
- Para baús altos de 2 a 5 blocos, a animação telescópica é mantida, mas ele precisa ficar perto horizontalmente (até aproximadamente 1,8 bloco) para ativá-la.
- Quando não consegue alcançar fisicamente um baú, o Agricultor não força a inserção. Permanece segurando a carga; isto é intencional para impedir teleporte de itens e perda de carga.

## Testes no jogo (cópia do mundo)
1. Use `/scg farmer compost on` e `/scg farmer reserve 32`; veja o status com `/scg farmer`.
2. Coloque composteira ao lado de um baú com moldura normal de trigo/sementes. Coloque 40 ou mais do mesmo item e confira o consumo dos excedentes; **sem moldura extra**.
3. Teste com exatamente 32 sementes — nenhuma pode ser consumida. Com 33, no máximo uma deve ser consumida.
4. Deixe várias plantações maduras: o Agricultor deve colher e ainda encontrar tempo para abastecer a composteira.
5. Encha a composteira e prepare baú de farinha de osso com moldura e espaço. Verifique retirada, coleta e armazenamento.
6. Remova a moldura do baú fonte e veja se para de consumir, mesmo com compostagem ativa.
7. Deixe um baú de colheita a 5–10 blocos: Agricultor deve caminhar até perto antes de depositar; não pode aparecer item no baú quando estiver longe.
8. Teste baú alto de 2–5 blocos: animação telescópica apenas quando golem está ao lado.
9. Verifique que Coletor e Organizador continuam organizando e os comandos de dia/noite e raio funcionam.
10. Teste dois Agricultores e reinicialização do mundo; não devem perder/duplicar itens ou consumir abaixo do estoque mínimo.

Este PR não implementa plantio a partir de baús em terra arada vazia. A compilação CI e os testes in-game são necessários antes do merge.
