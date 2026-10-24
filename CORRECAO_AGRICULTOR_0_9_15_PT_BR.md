# Smart Copper Golems 0.9.15 — correção específica do Agricultor

**Origem exata:** `fix/farmer-compost-nearby-deposits-0.9.14`; o mod antigo, Coletor e Organizador permanecem preservados. **Não mesclar sem validação no jogo.**

## Bugs relatados nas fotos / no teste 0.9.14
- Agricultores abandonavam a horta ao perceber drops de monstros (carne podre, ossos etc.).
- Às vezes ficavam parados segurando colheita sem alcançar o destino.
- As composteiras não recebiam itens, mesmo depois do ajuste anterior.

## Mudanças no código
1. `FarmerHarvestDrops` filtra **só** trigo, sementes de trigo, cenoura, batata, batata venenosa, beterraba, sementes de beterraba e farinha de osso. Não coleta carne podre, ossos, flechas nem outros drops de mobs. O Coletor continua recolhendo qualquer item.
2. Uma composteira com insumos válidos recebe prioridade periódica mesmo que existam itens da horta espalhados; interações ocorrem em movimento real até uma posição acessível ao lado do bloco.
3. O Agricultor só retira itens do chão se existir um depósito com moldura e espaço; para baús próximos verifica se há caminho físico viável. Se nenhum destino servir, deixa o item no chão, sem segurá-lo indefinidamente. Outros golems mantêm o roteamento antigo.
4. O Agricultor conclui a aproximação de um baú por **distância física ao baú**, não só por estar próximo ao waypoint de navegação. Periodicamente recalcula o waypoint ao detectar que ainda está longe.
5. Farinha de osso só é extraída da composteira quando há um baú de destino com filtro e espaço; `hasSpaceFor` tem semântica INVERTIDA no código herdado (retorna true quando não há espaço). O método permanece corretamente precedido de `!`.

## Montagem de compostagem
- A composteira NÃO precisa de item frame.
- Coloque um **baú ou barril** com filtro normal 0° (categoria) ou 45° (item) **até 3 blocos horizontalmente e 1 verticalmente** da composteira. Esse recipiente precisa conter **excedente real** de um item que a composteira aceite.
- Fonte de compostagem usa somente trigo, sementes de trigo, cenoura, batata, beterraba e sementes de beterraba. Itens fora dessa lista, como carne podre, ferramentas e diamantes, são ignorados.
- Um baú/barril de farinha de osso precisa ter moldura 0° da categoria ou 45° bone meal e espaço.
- Use `/scg farmer compost on`, `/scg farmer` (ver status) e `/scg farmer reserve 32`.
- **Reserva de 32:** com exatamente 32 itens elegíveis do mesmo tipo no baú NÃO haverá compostagem; com 40, há até 8 tentativas. Para TESTAR num mundo de cópia com poucas unidades, usar temporariamente `/scg farmer reserve 1` (ao concluir, restaurar 32). Reserva protegida é por item e por recipiente, não global.
- Quando a composteira chegar ao nível 7 precisa maturar para nível 8 antes de extrair bone meal. O preenchimento pode falhar aleatoriamente como no jogo vanilla.

## Testes mínimos antes de substituir seu JAR
1. Solte carne podre perto da horta: Agricultor **ignora** e continua colher/trazer sementes, Coletor ainda recolhe normalmente.
2. Solte trigo, sementes, cenoura, batata e beterraba: Agricultor leva aos baús com filtros, aproximando-se.
3. Confirme que não carrega um item quando nenhum baú com moldura é alcançável.
4. Fonte com 40 sementes e reserva 32, composteira a até 3 blocos do baú, compost on: composteira precisa consumir no máximo 8 (uma por serviço); não usar a 32ª.
5. Baú de farinha de osso: conferir que produto é extraído e armazenado; sem baú ou baú cheio, produto permanece na composteira.
6. Testar ao lado de vários composters e duas farms em funcionamento, inclusive dia/noite.
7. Golem de nome normal Organizador e golem Coletor mantêm funcionalidades da 0.9.10.
8. Baús a 2–5 blocos de altura continuam com stretch e sem depósito instantâneo distante.
9. Testar com duas pessoas, uma via Bedrock/Geyser, e reiniciar servidor para validar persistência.

Para reversão, reinstalar o JAR 0.9.12 ou 0.9.14, sem substituir nem mesclar branches. O cenário de produção real exige validação humana no Minecraft.
