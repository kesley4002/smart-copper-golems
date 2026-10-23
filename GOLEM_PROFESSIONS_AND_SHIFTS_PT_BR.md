# Smart Copper Golems — profissões, etiquetas e turnos (protótipo)

## Entregue nesta branch (Fabric 26.2)
- Área individual por golem com Name Tag. O vínculo é armazenado por UUID em `smart-copper-golem-work-zones.properties` do mundo, não pelo texto da etiqueta.
- Comandos com permissão OP:
  - `/scg radius 500`: largura global 500×500 (comportamento existente).
  - `/scg golem "João" radius 30`: largura individual 30×30 ao redor da âncora original do golem.
  - `/copper_golem "João" radius 30`: atalho equivalente.
  - `/scg golem "João"`: consultar limite individual.
  - `/scg golem "João" reset`: remover limite individual.
  - `/scg night`: consultar política noturna.
  - `/scg night work on` e `/scg night work off`: ligar/desligar trabalho noturno.
  - `/scg night radius 100`: largura 100×100 durante o trabalho noturno.
  - `/scg night radius reset`: retirar limite noturno especial.
- A largura é sempre PAR de 16 a 1000. "30" significa 30×30, não raio circular de 30. A área é quadrada e horizontal, centrada na posição registrada pela primeira vez para o golem.
- Os limites individuais prevalecem quando forem menores que o limite noturno; nunca ampliamos automaticamente a área particular de um golem.
- Nenhum chunk é carregado para procurar golems por nome nem para procurar baús.
- Dois golems carregados com o mesmo nome causam erro de ambiguidade (não altera nenhum). Golem descarregado precisa estar carregado para receber um comando por nome.
- Descanso noturno é o padrão, preservando comportamento prévio. Caso o golem ainda esteja levando um item, a tarefa em andamento pode concluir antes do descanso.
- Quando os limites mudam durante uma rota, o golem abandona o alvo fora da área e volta à âncora SEM destruir ou descartar o item carregado. As animações de alcançar baús não são interrompidas no meio do ciclo.
- Compatibilidade mantida na lógica existente: 42 categorias, 1413 IDs, molduras 0°/45°, graveto de origem, barris, ordenação interna, bloqueio de baús, sons e animação telescópica.
- **Atenção:** os comandos limitam destinos e corrigem desvios de navegação. Um caminho calculado pelo Minecraft pode tocar a borda fora da área por um tick; não é barreira física absoluta.
- **Não lançar como estável:** validar compilação do Fabric CI e testes em jogo antes de mesclar. Geyser/Bedrock não recebeu código novo de integração visual nesta branch.

## Proposta das profissões — NÃO implementada nesta branch
Identificar profissão pelo prefixo da etiqueta, mantendo nome individual:

- `Coletor: João` — recolher apenas itens de entidades dropadas dentro da área de trabalho e roteá-los via filtros existentes.
- `Agricultor: Ana` — recolher produtos e sementes *já dropados* por fazendas, sem arar, quebrar ou replantar. Pode enviar excedentes autorizados ao compostador.
- `Composteiro: Pedro` — alimentar composteiras com materiais habilitados; nunca usar sementes abaixo de uma reserva configurada; retirar farinha de osso, preferencialmente via baú/hopper.
- `Fornalheiro: Lia` — repor combustível no slot inferior de fornos, altos-fornos e defumadores, respeitando tipos de combustível aceitos e o nível alvo.
- `Carvoeiro: Carlos` — usar madeira em um forno designado, com combustível inicial, coleta do carvão vegetal e estoque mínimo de madeira separado.
- Ausência de prefixo especial = Organizador atual, sem alterar a IA de roteamento.

### Cuidados antes de implementar
1. Não consumir item de moldura (filtros), não pegar materiais de reservas protegidas.
2. Transações de inventário reversíveis/atômicas: checar espaço no destino ANTES da retirada, revalidar no commit, preservar item carregado após abortar.
3. Não converter sementes essenciais em farinha de osso; iniciar com compostagem desligada por padrão.
4. Um único golem reserva um destino/forno/composteira por vez, compartilhando locks dimensionais.
5. Profissões não executam tarefas em chunks descarregados. Não tentar carregar áreas de fazenda automaticamente.
6. Ao completar uma profissão, reutilizar filtros e categorias existentes para guardar resultados, sem criar depósitos aleatórios.
7. Testar com servidor Fabric 26.2 primeiro; portar a lógica à implementação Bedrock nativa em projeto separado, sem prometer paridade automática.

## Ideias futuras ligadas ao armazenamento
- **Repositor:** manter um baú de acesso rápido com quantidades mínimas de determinados materiais, retirando excedentes do depósito central.
- **Compactador:** converter 9 itens em blocos quando configurado, com proteção para estoques de reserva e sem transformar itens raros sem autorização.
- **Supervisor de estoque:** enviar avisos sobre baús lotados, categorias sem destino e escassez de materiais (com intervalo para evitar spam).
- **Separador de excedentes:** mover sobras de fazendas para baús de compostagem ou descarte apenas com autorização explícita e prioridade de armazenagem.
