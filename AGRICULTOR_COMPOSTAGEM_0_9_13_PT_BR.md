# Agricultor — compostagem protegida e coleta mais rápida (0.9.13 experimental)

Derivado EXATAMENTE da branch testada `fix/farmer-approach-and-storage-26.2` (0.9.12). Não modifica as profissões Coletor/Organizador nem a ponte Geyser.

## Novidade: estação de compostagem
O Agricultor pode usar UMA composteira próxima de um baú/barril de insumos, sem consumir itens indevidos.

### Montagem da estação
1. Coloque uma **composteira** em local acessível ao Agricultor.
2. A até 2 blocos na horizontal e 1 na vertical, coloque um baú (simples/duplo) ou barril.
3. Coloque no recipiente a moldura **normal do armazenamento**: por exemplo semente de trigo em 0° (categoria agriculture) ou cenoura em 0° (categoria food_raw_vegetables). Ela permite que o Agricultor coloque a produção ali normalmente.
4. Adicione uma **SEGUNDA moldura** nesse recipiente mostrando o ITEM COMPOSTEIRA e gire essa segunda moldura a **90°** (dois cliques). Ela marca o recipiente como **fonte de excedentes autorizada**. Uma moldura isolada com composteira a 90° NÃO é um filtro de destino.
5. Configure um baú de destino para farinha de osso: 0° com farinha de osso (categoria agriculture) ou 45° se quiser filtro por item exato.
6. Use comandos de operador: `/scg farmer compost on` para ativar. A compostagem fica OFF por padrão para evitar consumir suprimentos sem autorização.
7. Use `/scg farmer reserve 32` para manter no mínimo 32 unidades de **cada item permitido** dentro do recipiente que fornece adubo. Pode usar 0 a 256; não recomendamos 0 para sementes.

### Funcionamento
- O Agricultor continua priorizando recolher drops da horta, depositá-los no baú com moldura, e colher/replantar plantações maduras.
- Quando está livre, aproxima-se da composteira marcada e transfere UM excedente por interação. Usa a chance vanilla de compostagem.
- Só permite compostagem de: sementes de trigo, sementes de beterraba, trigo, cenoura, batata e beterraba. Não consome madeira, ferramentas, diamantes nem itens de outras categorias.
- Mantém a reserva por **tipo de item por recipiente**: se houver exatamente 32 cenouras no baú e a reserva estiver em 32, não consome nenhuma; se houver 40, pode consumir 8.
- Quando a composteira fica pronta (nível 8), o Agricultor retira farinha de osso pelo comportamento vanilla (item cai no chão) e usa a mesma coleta/armazenagem automática do Coletor. A retirada só acontece se um baú de farinha de osso com filtro compatível e espaço estiver carregado.
- Não consome itens de baús SEM a moldura de composteira a 90°, nem de recipientes fora da zona ou chunks descarregados.
- Mantém as travas de recipientes já existentes; não manipula mais de um item por serviço de estação.

### Rapidez
- A busca do Agricultor durante o estado IDLE passa de 100 ticks (5 segundos) para 20 ticks (1 segundo), sem alterar o cooldown do Coletor nem do Organizador.
- Após colher, a procura por drops continua com prioridade e reinício rápido.

### Testes recomendados
1. Confirme colheita, replantio e transporte normais ANTES de ligar a compostagem.
2. Monte uma estação marcada corretamente, ponha 40 sementes de trigo, reserve 32 e ative. A composteira deve consumir no máximo 8 sementes daquele recipiente.
3. Compare com 32, 31 e 33 sementes; não consumir abaixo/igual ao limite.
4. Teste itens não permitidos junto de sementes: não devem ser consumidos.
5. Teste várias pilhas da mesma semente no mesmo baú: a reserva considera o total.
6. Faça a composteira atingir nível 8 e confira se a farinha de osso cai e vai para um baú compatível.
7. Retire/lotar o baú da farinha de osso ANTES de retirar o adubo: o Agricultor deve deixar a composteira cheia (o jogador ainda pode retirar manualmente).
8. Remova a moldura de composteira, desative /scg farmer compost off, reduza o raio, ou descarregue a área: não pode continuar consumindo insumos.
9. Confira funcionamento do Organizador e Coletor, filtros 0°/45°, raios individuais, turnos e Bedrock/Geyser.
10. Reinicie o mundo: o comando on/off e a reserva precisam continuar persistentes.

**Limites deste protótipo:** a reserva protege itens já armazenados no recipiente da estação; não cria nem retira sementes de outros baús para plantar em terra arada vazia. O jogador deve construir a estação com duas molduras para permitir entrada automática e compostagem. Não mesclar sem testar em cópia do mundo.
