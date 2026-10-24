# Smart Copper Golems Extended 0.9.16 — Agricultor unificado

Este protótipo foi construído em cima da `0.9.15` (`fix/farmer-harvest-composter-routing-0.9.15`), sem reescrever Organizador ou Coletor. Não houve merge na branch anterior.

## Apenas UMA profissão faz todas as tarefas
Name Tag: `Agricultor` ou `Agricultor: João`.

O mesmo golem:
1. Vai até trigo/cenoura/batata/beterraba madura.
2. Colhe, reserva uma semente/unidade obtida para replantar na mesma terra arada.
3. Recolhe apenas os drops agrícolas e os leva até baús/barris com molduras de filtro. Ignora carne podre, flechas e outros drops de mobs.
4. Busca periodicamente uma composteira perto de baú agrícola com excedentes.
5. **Agora caminha até o BAÚ**, retira **UMA** unidade de excedente preservando a reserva mínima e o filtro. Carrega a unidade na mão enquanto caminha de volta até a **COMPOSTEIRA** e a insere nela com a chance vanilla.
6. Quando a composteira estiver pronta, retira a farinha de osso e usa a coleta/armazenamento normal para guardá-la no baú de bone meal.

Não é necessário nem suportado criar outro golem com etiqueta `Composteiro` para a função do Agricultor.

## Montagem correta
- Uma composteira acessível.
- Um **baú/barril agrícola** com moldura de filtro 0° (categoria) ou 45° (item), a até **3 blocos horizontalmente e 1 verticalmente** da composteira. A composteira NÃO precisa de moldura. O item COMPOSTEIRA em moldura 90° no baú continua sendo um marcador alternativo opcional.
- Um **baú/barril com moldura de farinha de osso** (categoria de agricultura ou 45° bone meal) e espaço.
- Devem existir excedentes de verdade **acima da reserva mínima**: com reserva 32, apenas 8 unidades das 40 sementes depositadas poderão ser compostadas.
- A composteira será habilitada por padrão em mundos sem configuração prévia. Configurações já salvas são respeitadas: se `compost.enabled=false` foi persistido, execute `/scg farmer compost on`. `/scg farmer` mostra estado e reserva. `/scg farmer compost off` desativa a compostagem sem mudar a profissão.
- `/scg farmer reserve 32` protege 32 de CADA tipo de item em cada baú/barril que fornece insumos. Não existe consumo de itens do depósito geral além das fontes com moldura compatível próximas às composteiras. A reserva só cobre o recipiente da estação.

## Proteções
- O agricultor não pega itens de monstros e não consome itens sem filtro de baú/autorização; o Coletor continua independente.
- Apenas o Agricultor executa a nova rota visível `baú -> composteira`.
- Se um baú perder a moldura, ficar sem excedentes, for aberto pelo jogador ou a composteira ficar indisponível, o golem não retira item indevidamente.
- Se a composteira encher **depois** de o golem pegar uma unidade, ele **não a destrói**: retorna com o item à lógica de armazenamento normal.
- Baús e barris são protegidos pelas travas existentes e verificações de reserva antes da retirada.
- Não gera nem duplica sementes na compostagem; um item real é consumido por interação com a composteira.
- Não utiliza chunks descarregados nem altera as animações existentes do Geyser.

## Checklist curto para o mundo de testes
1. Nomeie o mesmo golem `Agricultor: João`. Ele deve colher e replantar, sem outro nome/tag de Composteiro.
2. Execute `/scg farmer`: a compostagem deve aparecer LIGADA. Se estiver DESLIGADA por configuração salva, use `/scg farmer compost on`.
3. Ponha **40 cenouras** no baú com moldura 0° de cenoura ou 45° de cenoura, junto à composteira, e `/scg farmer reserve 32`.
4. Verifique se **o mesmo golem sai da horta, caminha ao baú, tira UMA cenoura, volta à composteira e a insere**.
5. Conte as cenouras: nunca devem restar menos de 32 na fonte. A composteira pode demorar a encher por chance de sucesso vanilla.
6. Deixe a composteira completar a maturação e confira a retirada de bone meal e transporte ao baú com moldura correta.
7. Desative compostagem durante transporte: nenhum item pode desaparecer; o golem deve guardá-lo normalmente.
8. Teste sem moldura, sem excedentes, com dois Agricultores, com Coletor e Organizador, turnos de dia/noite e raio.
9. Teste baús baixos e elevados e o cliente Bedrock via Geyser. São comportamentos que ainda precisam de validação em jogo.

**Experimental:** não mesclar ou substituir a versão aprovada até que o build e os testes dentro do Minecraft tenham sucesso. Preserve o JAR 0.9.15 para voltar.
